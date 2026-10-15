package com.poe.poegps.feature.presentation.capture

import android.location.Location
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.GpsTracker
import com.poe.poegps.core.location.TrustAssessor
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureLog
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.data.repository.GeoRepository
import com.poe.poegps.feature.presentation.model.OprDisplayable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CaptureResult {
    data class Captured(val pole: String, val accuracy: Float?, val belowGate: Boolean) : CaptureResult()
    data class Placed(val pole: String) : CaptureResult()
    object NoFix : CaptureResult()
    data class Locked(val trust: TrustState) : CaptureResult()
    data class NeedsAccuracy(val accuracy: Float?) : CaptureResult()
    /** The position passed the gate but looks wrong for this object; the crew confirms or cancels. */
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

/** A GPS position that passed the gate, with what is logged about it. */
data class GpsReading(
    val location: Location,
    val accuracy: Float?,
    val satellites: Int,
    val trust: TrustState,
)

/**
 * The capture machinery shared by the line editor (B3) and the TP screen (B7): GPS readings, the
 * trust verdict on them, the accuracy gate and the on-phone capture log. One per screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSession(
    scope: CoroutineScope,
    gpsTracker: GpsTracker,
    geoRepository: GeoRepository,
    private val captureLog: CaptureLog,
) {
    private val restart = MutableStateFlow(0)
    private val assessor = TrustAssessor()

    val gps: StateFlow<GpsFix> = restart
        .flatMapLatest { gpsTracker.fixes() }
        .stateIn(scope, SharingStarted.WhileSubscribed(2_000), GpsFix())

    /** Whether the current position can be trusted; jamming and spoofing lock capture. */
    val trust: StateFlow<TrustState> = gps
        .map { assessor.update(it.toSample()) }
        .stateIn(scope, SharingStarted.WhileSubscribed(2_000), TrustState())

    /** How each object on this phone was captured, by [CaptureLog] key. */
    val log: StateFlow<Map<String, CaptureMeta>> = captureLog.entries

    init {
        scope.launch { geoRepository.branchArea().collect { assessor.setBranch(it) } }
        scope.launch { captureLog.load() }
    }

    /** Start listening again, e.g. after the location permission was granted. */
    fun restartGps() {
        restart.value++
    }

    /** The current fix if it may be stored, or why not: no fix, jamming/spoofing, or (unless [belowGate]) accuracy. */
    fun read(belowGate: Boolean): Pair<GpsReading?, CaptureResult?> {
        val fix = gps.value
        val location = fix.current
        val trustNow = trust.value
        val accuracy = fix.accuracy
        return when {
            location == null -> null to CaptureResult.NoFix
            trustNow.level.locksCapture -> null to CaptureResult.Locked(trustNow)
            !belowGate && (accuracy == null || accuracy > CapturePolicy.ACCURACY_GATE_M) ->
                null to CaptureResult.NeedsAccuracy(accuracy)
            else -> GpsReading(location, accuracy, fix.satellitesUsed, trustNow) to null
        }
    }

    suspend fun recordGps(key: String, reading: GpsReading, confirmedWarnings: List<String>) = captureLog.record(
        key,
        CaptureMeta(
            timeMs = System.currentTimeMillis(),
            accuracyM = reading.accuracy,
            satellites = reading.satellites,
            trust = reading.trust.level.name,
            reasons = reading.trust.reasons,
            belowAccuracyGate = reading.accuracy == null || reading.accuracy > CapturePolicy.ACCURACY_GATE_M,
            confirmedWarnings = confirmedWarnings,
        ),
    )

    suspend fun recordManual(key: String, lat: Double, lng: Double, confirmedWarnings: List<String>) {
        val trustNow = trust.value
        // GPS under jamming/spoofing says nothing about where the object is
        val here = gps.value.current?.takeIf { !trustNow.level.locksCapture }
        captureLog.record(
            key,
            CaptureMeta(
                timeMs = System.currentTimeMillis(),
                trust = trustNow.level.name,
                reasons = trustNow.reasons,
                confirmedWarnings = confirmedWarnings,
                source = CaptureMeta.SOURCE_MANUAL,
                gpsOffsetM = here?.let { Geo.distanceM(it.latitude, it.longitude, lat, lng).toFloat() },
            ),
        )
    }

    suspend fun forget(key: String) = captureLog.forget(key)
}

class CaptureSessionFactory @Inject constructor(
    private val gpsTracker: GpsTracker,
    private val geoRepository: GeoRepository,
    private val captureLog: CaptureLog,
) {
    fun create(scope: CoroutineScope) = CaptureSession(scope, gpsTracker, geoRepository, captureLog)
}
