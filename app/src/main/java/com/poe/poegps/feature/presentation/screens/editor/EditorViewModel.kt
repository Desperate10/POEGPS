package com.poe.poegps.feature.presentation.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.GpsTracker
import com.poe.poegps.core.location.TrustAssessor
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureLog
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.data.repository.GeoRepository
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.domain.model.Recloser
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.mapper.toDomainModel
import com.poe.poegps.feature.presentation.mapper.toLineDomainModel
import com.poe.poegps.feature.presentation.mapper.toObjectDisplayable
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class InsertResult {
    object Success : InsertResult()
    object Duplicate : InsertResult()
    object Failure : InsertResult() // For any other unexpected error
}

sealed class CaptureResult {
    data class Captured(val pole: String, val accuracy: Float?, val belowGate: Boolean) : CaptureResult()
    data class Placed(val pole: String) : CaptureResult()
    object NoFix : CaptureResult()
    data class Locked(val trust: TrustState) : CaptureResult()
    data class NeedsAccuracy(val accuracy: Float?) : CaptureResult()
    /** The position passed the gate but looks wrong for this pole; the crew confirms or cancels. */
    data class NeedsConfirm(
        val pole: OprDisplayable,
        val belowGate: Boolean,
        val warnings: List<String>,
        /** Set when confirming a manual placement rather than a GPS capture. */
        val manual: Pair<Double, Double>? = null,
    ) : CaptureResult()
}

object CapturePolicy {
    /** B13: capture waits for this accuracy unless the crew explicitly overrides. */
    const val ACCURACY_GATE_M = 5f
    // longer spans than these are almost always a wrong position, not a long span
    const val MAX_SPAN_04_M = 150f
    const val MAX_SPAN_10_M = 350f
    /** GIS coordinates are often off, so only a gross difference asks for confirmation. */
    const val MAX_GIS_OFFSET_M = 200f
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    private val geoRepository: GeoRepository,
    private val gpsTracker: GpsTracker,
    private val captureLog: CaptureLog,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args by lazy { EditorFragmentArgs.fromSavedStateHandle(savedStateHandle) }

    private val _lineName = MutableStateFlow<String>(args.pltxt)
    val lineName = _lineName

    private val _oprDisplayable = MutableStateFlow<List<OprDisplayable>>(emptyList())
    val oprDisplayable = _oprDisplayable

    private val _parentObjectName = MutableStateFlow<String>("")
    val parentObjectName = _parentObjectName

    // Channel to send one-time UI events
    private val _insertResultChannel = Channel<InsertResult>()
    val insertResultFlow = _insertResultChannel.receiveAsFlow() // Expose as a Flow for collection


    private val _captureResult = Channel<CaptureResult>()
    val captureResult = _captureResult.receiveAsFlow()

    // bumped after the location permission is granted, to start listening again
    private val gpsRestart = MutableStateFlow(0)
    val gps: StateFlow<GpsFix> = gpsRestart
        .flatMapLatest { gpsTracker.fixes() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), GpsFix())

    private val trustAssessor = TrustAssessor()

    /** Whether the current position can be trusted; jamming and spoofing lock capture. */
    val trust: StateFlow<TrustState> = gps
        .map { trustAssessor.update(it.toSample()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), TrustState())

    /** How each pole on this phone was captured (kept on the phone only). */
    val captureMeta: StateFlow<Map<Int, CaptureMeta>> = captureLog.entries

    /** The pole the big button captures; null = the first one without coordinates. */
    private val _selectedPoleId = MutableStateFlow<Int?>(null)
    val selectedPoleId: StateFlow<Int?> = _selectedPoleId

    /** The branch's lines for the map (GeoJSON), from the server or its cached copy. */
    private val _gisLines = MutableStateFlow<String?>(null)
    val gisLines: StateFlow<String?> = _gisLines

    /** Where GIS currently has this line's poles; shown hollow on the map. */
    val gisPoles: StateFlow<List<Pillar>> = when (args.objectType) {
        ObjectType.LINE04, ObjectType.LINEABON04 -> repository.getPillar04List(args.tplnr)
        ObjectType.LINE10, ObjectType.LINEABON10 -> repository.getPillar10List(args.tplnr)
        else -> flowOf(emptyList())
    }.map { list -> list.filter { it.lat.toDoubleOrNull()?.let { lat -> lat != 0.0 } == true } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            geoRepository.branchLines().collect { _gisLines.value = it }
        }
        viewModelScope.launch {
            geoRepository.branchArea().collect { trustAssessor.setBranch(it) }
        }
        viewModelScope.launch { captureLog.load() }
        viewModelScope.launch {
            _lineName.flatMapLatest {
                repository.getSavedPillars(it)
            }.collectLatest { list ->
                _oprDisplayable.value = list.map { opr -> opr.toOprDisplayable() }
            }
        }
    }

    fun setLineName(lineName: String) {
        _lineName.value = lineName
    }

    fun getPillar04List(tplnr: String) =
        repository.getPillar04List(tplnr).map { list ->
            list.map { opr -> opr.toOprDisplayable() }
        }

    fun getPillar10List(tplnr: String) =
        repository.getPillar10List(tplnr).map { list ->
            list.map { opr -> opr.toOprDisplayable() }
        }

    fun getLineList10() =
        repository.getLineList10().map { list ->
            list.map { line -> line.toObjectDisplayable() }
        }

    fun getLineList04() =
        repository.getLineList04().map { list ->
            list.map { line -> line.toObjectDisplayable() }
        }

    fun getTpList(isAbon: Boolean) =
        repository.getTpList(isAbon).map { list ->
            list.map { tp -> tp.toOprDisplayable() }
        }

    fun addPillarToDisplay(obj: OprDisplayable) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowId = repository.savePillar(obj.toDomainModel())
                if (rowId != -1L) {
                    _insertResultChannel.send(InsertResult.Success)
                } else {
                    // Room returns -1L when INSERT ... WHERE NOT EXISTS prevents insertion
                    _insertResultChannel.send(InsertResult.Duplicate)
                }
            } catch (e: Exception) {
                // Catch any other potential database errors during insertion
                _insertResultChannel.send(InsertResult.Failure)
                // You might also log the error: Log.e("PillarViewModel", "Error saving pillar", e)
            }
        }
    }

    fun addRecloserToPillar(id: Int, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecloserToPillar(id, name)
        }
    }

    fun createRecloser(recloser: RecloserDisplayable) {
        viewModelScope.launch {
            repository.saveRecloser(recloser.toDomainModel())
        }
    }

    fun createOtp(pillarStart: OprDisplayable, pillarNext: OprDisplayable) {
        viewModelScope.launch {
            repository.copyPillarForOtp(pillarStart.copy(id = 0).toDomainModel())
            repository.savePillar(pillarNext.toDomainModel())
            when(args.objectType) {
                ObjectType.LINE04 -> repository.saveLine04(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINE10 -> repository.saveLine10(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINEABON04 -> repository.saveAbonLine04(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINEABON10 -> repository.saveAbonLine10(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                /* ObjectType.LINE35 -> TODO()
                 ObjectType.LINE110 -> TODO()
                 ObjectType.LINE154 -> TODO()
                 ObjectType.LINEABON35 -> TODO()
                 ObjectType.LINEABON110 -> TODO()
                 ObjectType.LINEABON154 -> TODO()*/
                ObjectType.TP -> {
                }
                ObjectType.TPABON -> {}
                ObjectType.KLKV04 -> {}
                ObjectType.KLKV10 -> {}
                ObjectType.KLKVABON04 -> {}
                ObjectType.KLKVABON10 -> {}
            }
            /*repository.saveLine(
                ObjectDisplayable(
                    tplnr = pillarStart.tplnr,
                    name = pillarStart.parentName
                ).toLineDomainModel()
            )*/
        }
    }

    fun clearCoord(pillar: OprDisplayable?) {
        pillar ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePillarCoords(pillar.id, "0.0", "0.0")
            captureLog.forget(pillar.id)
        }
    }

    fun deleteOpr(pillar: OprDisplayable?) {
        viewModelScope.launch(Dispatchers.IO) {
            pillar?.toDomainModel()?.let { repository.deletePillar(it) }
            pillar?.let { captureLog.forget(it.id) }
        }
    }

    fun getParentName(tplnr: String) {
        viewModelScope.launch {
            _parentObjectName.value = repository.getParentName(tplnr)
        }
    }

    fun restartGps() {
        gpsRestart.value++
    }

    fun selectPole(id: Int?) {
        _selectedPoleId.value = id
    }

    /**
     * Stores the current GPS fix as [opr]'s position, in the same "%.5f" text the upload has always
     * sent. In order: a fix is needed, jamming/spoofing refuses, the accuracy gate waits unless
     * [belowGate], and implausible positions ask for confirmation unless [confirmed].
     */
    fun capture(opr: OprDisplayable, belowGate: Boolean = false, confirmed: Boolean = false) {
        val fix = gps.value
        val location = fix.current
        val trustNow = trust.value
        viewModelScope.launch(Dispatchers.IO) {
            val accuracy = fix.accuracy
            val warnings = location?.let { plausibilityWarnings(opr, it.latitude, it.longitude) }.orEmpty()
            val blocked = when {
                location == null -> CaptureResult.NoFix
                trustNow.level.locksCapture -> CaptureResult.Locked(trustNow)
                !belowGate && (accuracy == null || accuracy > CapturePolicy.ACCURACY_GATE_M) ->
                    CaptureResult.NeedsAccuracy(accuracy)
                !confirmed && warnings.isNotEmpty() -> CaptureResult.NeedsConfirm(opr, belowGate, warnings)
                else -> null
            }
            if (blocked != null || location == null) {
                _captureResult.send(blocked ?: CaptureResult.NoFix)
                return@launch
            }
            storePosition(opr, location.latitude, location.longitude)
            captureLog.record(
                opr.id,
                CaptureMeta(
                    timeMs = System.currentTimeMillis(),
                    accuracyM = accuracy,
                    satellites = fix.satellitesUsed,
                    trust = trustNow.level.name,
                    reasons = trustNow.reasons,
                    belowAccuracyGate = accuracy == null || accuracy > CapturePolicy.ACCURACY_GATE_M,
                    confirmedWarnings = warnings,
                ),
            )
            // the button moves on to the next pole without coordinates
            if (_selectedPoleId.value == opr.id) _selectedPoleId.value = null
            _captureResult.send(CaptureResult.Captured(opr.name, accuracy, belowGate))
        }
    }

    /**
     * B6: the crew puts the pole on the map by hand, e.g. when GPS is jammed or spoofed. The same
     * span and GIS checks apply; the capture log records it as manual, with its offset from GPS.
     */
    fun placeManually(opr: OprDisplayable, lat: Double, lng: Double, confirmed: Boolean = false) {
        val fix = gps.value
        val trustNow = trust.value
        viewModelScope.launch(Dispatchers.IO) {
            val warnings = plausibilityWarnings(opr, lat, lng)
            if (!confirmed && warnings.isNotEmpty()) {
                _captureResult.send(CaptureResult.NeedsConfirm(opr, belowGate = false, warnings, manual = lat to lng))
                return@launch
            }
            storePosition(opr, lat, lng)
            // GPS under jamming/spoofing says nothing about where the pole is
            val here = fix.current?.takeIf { !trustNow.level.locksCapture }
            captureLog.record(
                opr.id,
                CaptureMeta(
                    timeMs = System.currentTimeMillis(),
                    trust = trustNow.level.name,
                    reasons = trustNow.reasons,
                    confirmedWarnings = warnings,
                    source = CaptureMeta.SOURCE_MANUAL,
                    gpsOffsetM = here?.let { Geo.distanceM(it.latitude, it.longitude, lat, lng).toFloat() },
                ),
            )
            if (_selectedPoleId.value == opr.id) _selectedPoleId.value = null
            _captureResult.send(CaptureResult.Placed(opr.name))
        }
    }

    // the same "%.5f" text the upload has always sent
    private suspend fun storePosition(opr: OprDisplayable, lat: Double, lng: Double) {
        repository.updatePillarCoords(
            opr.id,
            String.format(Locale.US, "%.5f", lat),
            String.format(Locale.US, "%.5f", lng),
        )
    }

    /** Span from the previous captured pole and offset from where GIS has this pole. */
    private fun plausibilityWarnings(opr: OprDisplayable, lat: Double, lng: Double): List<String> {
        val warnings = mutableListOf<String>()
        val poles = _oprDisplayable.value
        val index = poles.indexOfFirst { it.id == opr.id }
        val previous = poles.take(index.coerceAtLeast(0)).lastOrNull { it.hasCoordinates }
        val lowVoltage = args.category.contains("0,4") || args.category.contains("0.4")
        val maxSpan = if (lowVoltage) CapturePolicy.MAX_SPAN_04_M else CapturePolicy.MAX_SPAN_10_M
        if (previous != null) {
            val span = Geo.distanceM(previous.lat.toDouble(), previous.lng.toDouble(), lat, lng)
            if (span > maxSpan) {
                warnings += "Прогон від «${previous.name}» — ${span.toInt()} м, більше, ніж буває на такій лінії"
            }
        }
        gisPoles.value.firstOrNull { it.name == opr.name }?.let { gis ->
            val offset = Geo.distanceM(gis.lat.toDouble(), gis.lng.toDouble(), lat, lng)
            if (offset > CapturePolicy.MAX_GIS_OFFSET_M) warnings += "${offset.toInt()} м від позиції цієї опори в ГІС"
        }
        return warnings
    }

    fun saveCoordinates(opr: OprDisplayable, lat: String, lng: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePillarCoords(opr.id, lat, lng)
        }
    }


    /*fun getLineList10() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getLineList10().collectLatest {
                _lineList10.emit(it.map { opr -> opr.toObjectDisplayable() })
            }
        }
    }*/

    //функция получения списка ТП или ПС по конкретной категории напряжения(10 или 0.4)
    //fun getTpList(category: String) = repository.getTpList(category)


}