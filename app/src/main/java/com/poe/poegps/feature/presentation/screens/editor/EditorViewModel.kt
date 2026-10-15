package com.poe.poegps.feature.presentation.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureLog
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.data.repository.GeoRepository
import com.poe.poegps.feature.presentation.capture.CapturePolicy
import com.poe.poegps.feature.presentation.capture.CaptureResult
import com.poe.poegps.feature.presentation.capture.CaptureSessionFactory
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
import com.poe.poegps.feature.presentation.model.PillarType
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import com.poe.poegps.feature.presentation.model.RecloserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import com.poe.poegps.feature.domain.GisPoint
import com.poe.poegps.feature.domain.LineOrdering
import com.poe.poegps.feature.presentation.map.lineShapes
import com.poe.poegps.feature.presentation.screens.main.plural
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed class InsertResult {
    object Success : InsertResult()
    object Duplicate : InsertResult()
    object Failure : InsertResult() // For any other unexpected error
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    private val geoRepository: GeoRepository,
    captureSessionFactory: CaptureSessionFactory,
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


    /** One-off messages for the snackbar: tap created, recloser saved. */
    private val _notices = Channel<String>(Channel.BUFFERED)
    val notices = _notices.receiveAsFlow()

    private val _captureResult = Channel<CaptureResult>()
    val captureResult = _captureResult.receiveAsFlow()

    private val session = captureSessionFactory.create(viewModelScope)
    val gps: StateFlow<GpsFix> = session.gps
    val trust: StateFlow<TrustState> = session.trust

    /** How each pole on this phone was captured (kept on the phone only), by pole id. */
    val captureMeta: StateFlow<Map<Int, CaptureMeta>> = session.log
        .map { log -> log.mapNotNull { (key, meta) -> key.toIntOrNull()?.let { it to meta } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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
            _lineName.flatMapLatest {
                repository.getSavedPillars(it)
            }.collectLatest { list ->
                _oprDisplayable.value = list.map { opr -> opr.toOprDisplayable() }
            }
        }
        importFromGis()
    }

    /**
     * An existing line opened with nothing on this phone: load its GIS poles in line order (from its
     * GIS geometry, else by pole number) as "as in GIS" and already sent. Any later change sends the
     * whole line, so the server rebuilds its geometry complete.
     */
    private fun importFromGis() = viewModelScope.launch(Dispatchers.IO) {
        val highVoltage = when (args.objectType) {
            ObjectType.LINE04, ObjectType.LINEABON04 -> false
            ObjectType.LINE10, ObjectType.LINEABON10 -> true
            else -> return@launch
        }
        val line = args.pltxt
        if (repository.getSavedPillars(line).first().isNotEmpty()) return@launch
        val gis = (if (highVoltage) repository.getPillar10List(args.tplnr) else repository.getPillar04List(args.tplnr))
            .first()
            .filter { p -> p.lat.toDoubleOrNull()?.let { it != 0.0 } == true && p.lng.toDoubleOrNull() != null }
        if (gis.isEmpty()) return@launch
        // the cached branch lines arrive within moments; without them the order falls back to pole numbers
        val json = withTimeoutOrNull(GEOMETRY_WAIT_MS) { _gisLines.filterNotNull().first() }
        val points = gis.map { GisPoint(it.name, it.lat.toDouble(), it.lng.toDouble()) }
        val ordered = LineOrdering.order(line, points, json?.let { lineShapes(it, args.tplnr) }.orEmpty())
        val pillarType = if (CABLE_LINE.containsMatchIn(line)) PillarType.KL else PillarType.PILLAR
        val poles = ordered.poles.map { point ->
            // the GIS row itself: its coordinates go back to the server exactly as GIS has them
            val src = gis[points.indexOfFirst { it === point }]
            Pillar(
                id = 0, tplnr = args.tplnr, name = src.name, parentName = line, category = args.category,
                wire = src.wire, pillarType = pillarType.name, lat = src.lat, lng = src.lng,
                isAbon = args.isAbon, recloser = "", isSent = true, fromGis = true,
            )
        }
        val loaded = repository.importLine(args.tplnr, line, poles)
        if (loaded > 0) {
            _notices.send(
                "З ГІС завантажено $loaded ${plural(loaded, "опору", "опори", "опор")}" +
                    if (ordered.fromShape) "" else " · порядок за номерами, перевірте"
            )
        }
    }

    fun setLineName(lineName: String) {
        _lineName.value = lineName
        _selectedPoleId.value = null
    }

    /** B8: a pole GIS doesn't have yet, appended to the line or put right after the pole [afterId]. */
    fun addPole(name: String, afterId: Int? = null) {
        val line = _lineName.value
        val pillarType = if (CABLE_LINE.containsMatchIn(line)) PillarType.KL else PillarType.PILLAR
        addPillarToDisplay(
            OprDisplayable(
                tplnr = args.tplnr,
                name = name.trim(),
                parentName = line,
                category = args.category,
                pillarType = pillarType.name,
                isAbon = args.isAbon,
            ),
            afterId,
        )
    }

    /** B9: GIS poles of other lines (or TPs) with coordinates, for the "by name" list. */
    fun candidates(source: PickSource, line: ObjectDisplayable?): Flow<List<PickItem>> = when (source) {
        PickSource.TP, PickSource.ABON_TP -> repository.getTpList(source == PickSource.ABON_TP).map { tps ->
            tps.map { PickItem(it.tplnr + it.id, it.name, "", it.lat, it.lng, "", it.isAbon) }
        }
        PickSource.POLE_10, PickSource.POLE_04 -> when {
            line == null -> flowOf(emptyList())
            source == PickSource.POLE_10 -> repository.getPillar10List(line.tplnr)
            else -> repository.getPillar04List(line.tplnr)
        }.map { list -> list.map { it.toPickItem(line?.name.orEmpty()) }.filter { it.hasCoordinates }.sortedWith(POLE_ORDER) }
    }.flowOn(Dispatchers.IO)

    /** B9 "Поруч зі мною": candidates within [NEARBY_RADIUS_M] of ([lat], [lng]), nearest first. */
    suspend fun nearby(source: PickSource, lat: Double, lng: Double): List<PickItem> = withContext(Dispatchers.IO) {
        val all = when (source) {
            PickSource.POLE_10, PickSource.POLE_04 -> {
                val dLat = NEARBY_RADIUS_M / 111_320.0
                val dLng = NEARBY_RADIUS_M / (111_320.0 * cos(Math.toRadians(lat)))
                repository.getPillarsIn(source == PickSource.POLE_10, lat - dLat, lat + dLat, lng - dLng, lng + dLng)
                    .map { it.toPickItem(it.parentName) }
            }
            else -> candidates(source, null).first()
        }
        all.filter { it.hasCoordinates }
            .map { it to Geo.distanceM(lat, lng, it.lat.toDouble(), it.lng.toDouble()) }
            .filter { it.second <= NEARBY_RADIUS_M }
            .sortedBy { it.second }
            .take(NEARBY_LIMIT)
            .map { it.first }
    }

    fun getLines(source: PickSource): Flow<List<ObjectDisplayable>> =
        if (source == PickSource.POLE_10) getLineList10() else getLineList04()

    /** B9: copies an existing GIS point (pole of another line, or a TP) into this line. */
    fun addExisting(source: PickSource, item: PickItem, afterId: Int? = null) {
        val isTp = source == PickSource.TP || source == PickSource.ABON_TP
        addPillarToDisplay(
            OprDisplayable(
                id = 0,
                tplnr = args.tplnr,
                name = item.name,
                parentName = _lineName.value,
                category = if (isTp) args.category else item.category,
                isAbon = item.isAbon,
                pillarType = if (isTp) "" else PillarType.PILLAR.name,
                lat = item.lat,
                lng = item.lng,
            ),
            afterId,
        )
    }

    /** B10: a recloser or disconnector at [pole], placed at the pole's coordinates. */
    fun saveRecloser(pole: OprDisplayable, type: RecloserType, name: String) {
        val trimmed = name.trim()
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecloserToPillar(pole.id, trimmed)
            repository.saveRecloser(
                RecloserDisplayable(
                    tplnr = pole.tplnr, name = trimmed, opr = pole.name, type = type, lat = pole.lat, lng = pole.lng,
                ).toDomainModel()
            )
            _notices.send("$type «$trimmed» збережено · ${poleTitle(pole.name)}")
        }
    }

    /**
     * B11: a tap from [from] whose first pole is [firstPole]. The name keeps the generated format the
     * server receives as the line name. [open] switches the editor to the tap, as the old dialog always did.
     */
    fun createTap(from: OprDisplayable, firstPole: String, open: Boolean) {
        val first = firstPole.trim()
        val name = tapName(from.name, first)
        createOtp(
            from.copy(parentName = name),
            OprDisplayable(
                tplnr = from.tplnr,
                name = first,
                parentName = name,
                category = from.category,
                pillarType = PillarType.PILLAR.name,
                isAbon = from.isAbon,
            )
        )
        if (open) setLineName(name)
        viewModelScope.launch { _notices.send("Відпайку створено: $name") }
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

    fun addPillarToDisplay(obj: OprDisplayable, afterId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowId = repository.savePillar(obj.toDomainModel(), afterId)
                if (rowId > 0L) {
                    _insertResultChannel.send(InsertResult.Success)
                } else {
                    // the DAO returns 0 when the line already has a pole with this name (Room: -1 on IGNORE)
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
            // the tap's own copy of its first pole: unsent, so the new tap uploads whole
            repository.copyPillarForOtp(pillarStart.copy(id = 0, isSent = false, fromGis = false).toDomainModel())
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
            session.forget(CaptureLog.poleKey(pillar.id))
        }
    }

    fun deleteOpr(pillar: OprDisplayable?) {
        viewModelScope.launch(Dispatchers.IO) {
            pillar?.toDomainModel()?.let { repository.deletePillar(it) }
            pillar?.let { session.forget(CaptureLog.poleKey(it.id)) }
        }
    }

    fun getParentName(tplnr: String) {
        viewModelScope.launch {
            _parentObjectName.value = repository.getParentName(tplnr)
        }
    }

    fun restartGps() = session.restartGps()

    companion object {
        /** КЛ/КВ lines get cable "poles" (PillarType.KL), as the old add dialog decided. */
        private val CABLE_LINE = Regex("К[ВЛ]-(0|6|10)", RegexOption.IGNORE_CASE)
        const val NEARBY_RADIUS_M = 1_000.0
        private const val GEOMETRY_WAIT_MS = 5_000L
        private const val NEARBY_LIMIT = 30

        fun tapName(fromPole: String, firstPole: String) = "Відп. від оп. $fromPole до оп. $firstPole"
    }

    fun selectPole(id: Int?) {
        _selectedPoleId.value = id
    }

    /**
     * Stores the current GPS fix as [opr]'s position. In order: a fix is needed, jamming/spoofing
     * refuses, the accuracy gate waits unless [belowGate], and implausible positions ask for
     * confirmation unless [confirmed].
     */
    fun capture(opr: OprDisplayable, belowGate: Boolean = false, confirmed: Boolean = false) {
        val (reading, blocked) = session.read(belowGate)
        viewModelScope.launch(Dispatchers.IO) {
            if (reading == null) {
                _captureResult.send(blocked ?: CaptureResult.NoFix)
                return@launch
            }
            val lat = reading.location.latitude
            val lng = reading.location.longitude
            val warnings = plausibilityWarnings(opr, lat, lng)
            if (!confirmed && warnings.isNotEmpty()) {
                _captureResult.send(CaptureResult.NeedsConfirm(opr, belowGate, warnings))
                return@launch
            }
            storePosition(opr, lat, lng)
            session.recordGps(CaptureLog.poleKey(opr.id), reading, warnings)
            // the button moves on to the next pole without coordinates
            if (_selectedPoleId.value == opr.id) _selectedPoleId.value = null
            _captureResult.send(CaptureResult.Captured(poleTitle(opr.name), reading.accuracy, belowGate))
        }
    }

    /**
     * B6: the crew puts the pole on the map by hand, e.g. when GPS is jammed or spoofed. The same
     * span and GIS checks apply; the capture log records it as manual, with its offset from GPS.
     */
    fun placeManually(opr: OprDisplayable, lat: Double, lng: Double, confirmed: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val warnings = plausibilityWarnings(opr, lat, lng)
            if (!confirmed && warnings.isNotEmpty()) {
                _captureResult.send(CaptureResult.NeedsConfirm(opr, belowGate = false, warnings, manual = lat to lng))
                return@launch
            }
            storePosition(opr, lat, lng)
            session.recordManual(CaptureLog.poleKey(opr.id), lat, lng, warnings)
            if (_selectedPoleId.value == opr.id) _selectedPoleId.value = null
            _captureResult.send(CaptureResult.Placed(poleTitle(opr.name)))
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
                warnings += "Прогон від «${poleTitle(previous.name)}» — ${span.toInt()} м, більше, ніж буває на такій лінії"
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

private fun com.poe.poegps.feature.domain.model.Pillar.toPickItem(line: String) =
    PickItem("$tplnr#$id", name, line, lat, lng, category, isAbon)

/** "2", "13а", "13/1", "16": by the leading number, then the rest; names without one go last. */
private val POLE_ORDER = compareBy<PickItem>(
    { Regex("\\d+").find(it.name)?.value?.toLongOrNull() ?: Long.MAX_VALUE },
    { it.name },
)
