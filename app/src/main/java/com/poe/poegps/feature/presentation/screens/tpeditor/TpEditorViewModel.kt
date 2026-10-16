package com.poe.poegps.feature.presentation.screens.tpeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureLog
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.data.repository.GeoRepository
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.capture.CapturePolicy
import com.poe.poegps.feature.presentation.capture.CaptureResult
import com.poe.poegps.feature.presentation.capture.CaptureSessionFactory
import com.poe.poegps.feature.presentation.mapper.toDomainModel
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.hasCoordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** B7 · ТП/РП: capture of a substation's single point, with the same gate and checks as poles. */
@HiltViewModel
class TpEditorViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    geoRepository: GeoRepository,
    captureSessionFactory: CaptureSessionFactory,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = TpEditorFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val session = captureSessionFactory.create(viewModelScope)
    private val logKey = CaptureLog.tpKey(args.tplnr)

    val gps: StateFlow<GpsFix> = session.gps
    val trust: StateFlow<TrustState> = session.trust

    private val _tp = MutableStateFlow<OprDisplayable?>(null)
    val tp: StateFlow<OprDisplayable?> = _tp

    /** The TP's row in the upload table, if its point was taken on this phone. */
    val progress: StateFlow<LineProgress?> = repository.getLineProgress()
        .map { rows -> rows.filter { it.tplnr == args.tplnr }.reduceOrNull { a, b -> a.copy(captured = a.captured + b.captured, unsent = a.unsent + b.unsent) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val captureMeta: StateFlow<CaptureMeta?> = session.log
        .map { it[logKey] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _gisLines = MutableStateFlow<String?>(null)
    val gisLines: StateFlow<String?> = _gisLines

    private val _captureResult = Channel<CaptureResult>()
    val captureResult = _captureResult.receiveAsFlow()

    init {
        viewModelScope.launch { _tp.value = repository.getSingleTpObject(args.tplnr, args.isAbon).toOprDisplayable() }
        viewModelScope.launch { geoRepository.branchLines().collect { _gisLines.value = it } }
    }

    fun restartGps() = session.restartGps()

    fun capture(belowGate: Boolean = false, confirmed: Boolean = false) {
        val tp = _tp.value ?: return
        val (reading, blocked) = session.read(belowGate)
        viewModelScope.launch(Dispatchers.IO) {
            if (reading == null) {
                _captureResult.send(blocked ?: CaptureResult.NoFix)
                return@launch
            }
            val lat = reading.location.latitude
            val lng = reading.location.longitude
            val warnings = plausibilityWarnings(tp, lat, lng)
            if (!confirmed && warnings.isNotEmpty()) {
                _captureResult.send(CaptureResult.NeedsConfirm(tp, belowGate, warnings))
                return@launch
            }
            store(tp, lat, lng)
            session.recordGps(logKey, reading, warnings)
            _captureResult.send(CaptureResult.Captured(tp.name, reading.accuracy, belowGate))
        }
    }

    /** B6 for a TP: placed by hand on the map. */
    fun placeManually(lat: Double, lng: Double, confirmed: Boolean = false) {
        val tp = _tp.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val warnings = plausibilityWarnings(tp, lat, lng)
            if (!confirmed && warnings.isNotEmpty()) {
                _captureResult.send(CaptureResult.NeedsConfirm(tp, belowGate = false, warnings, manual = lat to lng))
                return@launch
            }
            store(tp, lat, lng)
            session.recordManual(logKey, lat, lng, warnings)
            _captureResult.send(CaptureResult.Placed(tp.name))
        }
    }

    // saveTpCoord writes the upload row and the TP table
    private suspend fun store(tp: OprDisplayable, lat: Double, lng: Double) {
        val saved = tp.copy(lat = Geo.coordText(lat), lng = Geo.coordText(lng))
        repository.saveTpCoord(saved.toDomainModel())
        _tp.value = saved
    }

    /** A TP doesn't move: a new point far from its current one is more likely a wrong fix. */
    private fun plausibilityWarnings(tp: OprDisplayable, lat: Double, lng: Double): List<String> {
        if (!tp.hasCoordinates) return emptyList()
        val offset = Geo.distanceM(tp.lat.toDouble(), tp.lng.toDouble(), lat, lng)
        return if (offset > CapturePolicy.MAX_GIS_OFFSET_M) listOf("${offset.toInt()} м від нинішньої позиції ТП") else emptyList()
    }
}
