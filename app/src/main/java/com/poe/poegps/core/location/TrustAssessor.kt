package com.poe.poegps.core.location

import kotlin.math.abs
import kotlin.math.sqrt

/** How far a GPS position can be trusted for capture. */
enum class TrustLevel(val label: String, val locksCapture: Boolean) {
    RELIABLE("Надійно", false),
    SUSPICIOUS("Підозріло", false),
    JAMMING("Глушіння", true),
    SPOOFING("Підміна", true),
}

data class TrustState(
    val level: TrustLevel = TrustLevel.RELIABLE,
    val reasons: List<String> = emptyList(),
)

/** One reading from the receiver, free of Android types so the checks can be unit tested. */
data class GnssSample(
    val elapsedMs: Long,
    val lat: Double?,
    val lng: Double?,
    val accuracyM: Float?,
    /** UTC time the receiver reports for the fix. */
    val fixTimeMs: Long?,
    /** The phone's own clock when the sample was taken. */
    val wallClockMs: Long,
    val isMock: Boolean,
    val satellitesUsed: Int,
    val satellitesVisible: Int,
    /** C/N0 of every satellite the receiver can hear, dB-Hz. */
    val cn0: List<Float>,
    /** Receiver gain, dB; drops when a jammer raises the noise floor. Null when the chip doesn't report it. */
    val agcDb: Double?,
)

/**
 * Judges each GPS sample against what genuine reception looks like. Evidence of jamming or
 * spoofing holds the verdict for a while so a single clean sample can't unlock capture.
 *
 * The thresholds are deliberately conservative: a false alarm stops a crew's work, a missed one
 * only means the server's own checks (oblast bounds, 10 km from the branch) have to catch it.
 */
class TrustAssessor(private var branch: Area? = null) {

    private var lastFix: GnssSample? = null
    private var lastGoodFixMs: Long? = null
    private var agcBaseline: Double? = null
    private val agcHistory = ArrayDeque<Double>()
    private var noFixSinceMs: Long? = null
    private var held: TrustState? = null
    private var heldUntilMs = 0L

    fun setBranch(area: Area?) {
        branch = area
    }

    fun update(s: GnssSample): TrustState {
        val spoofing = mutableListOf<String>()
        val jamming = mutableListOf<String>()
        val suspicious = mutableListOf<String>()
        val hasFix = s.lat != null && s.lng != null

        // spoofing: the OS says so, or the position moves faster than a car can
        if (s.isMock) spoofing += "Позицію підставляє інший застосунок (фіктивне місцезнаходження)"
        val prev = lastFix
        if (hasFix && prev != null && prev.lat != null && prev.lng != null) {
            val dt = (s.elapsedMs - prev.elapsedMs) / 1000.0
            if (dt > 0) {
                val d = Geo.distanceM(prev.lat, prev.lng, s.lat!!, s.lng!!)
                val slack = (prev.accuracyM ?: 0f) + (s.accuracyM ?: 0f)
                val speed = (d - slack).coerceAtLeast(0.0) / dt
                if (speed > MAX_SPEED_MPS && d > MIN_JUMP_M) {
                    spoofing += "Позиція стрибнула на ${d.toInt()} м за ${"%.0f".format(dt)} с"
                }
            }
        }

        // clock: GNSS time far from the phone's (network-set) clock
        if (s.fixTimeMs != null && abs(s.fixTimeMs - s.wallClockMs) > MAX_CLOCK_SKEW_MS) {
            suspicious += "Час GPS не збігається з часом телефона"
        }

        // signal strength: a simulator transmits every satellite at the same strong level
        if (s.cn0.size >= MIN_SATS_FOR_CN0) {
            val mean = s.cn0.average()
            val sd = sqrt(s.cn0.map { (it - mean) * (it - mean) }.average())
            if (mean > UNIFORM_CN0_MIN_MEAN && sd < UNIFORM_CN0_MAX_SD) {
                suspicious += "Сигнали всіх супутників однаково сильні"
            }
        }

        // jamming: the receiver hears satellites but can't use them, or its gain collapses
        s.agcDb?.let { agc ->
            agcHistory.addLast(agc)
            if (agcHistory.size > AGC_WINDOW) agcHistory.removeFirst()
            if (hasFix && (s.accuracyM ?: Float.MAX_VALUE) < GOOD_ACCURACY_M && agcHistory.size >= AGC_WINDOW / 2) {
                agcBaseline = agcHistory.sorted()[agcHistory.size / 2]
            }
            val base = agcBaseline
            if (base != null && base - agc > AGC_DROP_DB) jamming += "Різко зріс рівень радіошуму"
        }
        if (hasFix) {
            noFixSinceMs = null
            if ((s.accuracyM ?: Float.MAX_VALUE) < GOOD_ACCURACY_M) lastGoodFixMs = s.elapsedMs
        } else {
            if (noFixSinceMs == null) noFixSinceMs = s.elapsedMs
            val lostFor = s.elapsedMs - (noFixSinceMs ?: s.elapsedMs)
            val recentlyGood = lastGoodFixMs?.let { s.elapsedMs - it < RECENT_FIX_MS } == true
            val weak = s.cn0.isNotEmpty() && s.cn0.average() < JAMMED_CN0_MAX
            if (recentlyGood && lostFor > LOST_FIX_MS && s.satellitesVisible >= 4 && s.satellitesUsed == 0 && weak) {
                jamming += "Супутники видно, але сигнал заглушено"
            }
        }

        // where: well outside the branch
        val area = branch
        if (hasFix && area != null && !Geo.contains(area, s.lat!!, s.lng!!)) {
            val out = Geo.distanceToEdgeM(area, s.lat, s.lng)
            if (out > BRANCH_MARGIN_M) suspicious += "За межами філії на ${"%.1f".format(out / 1000)} км"
        }

        if (hasFix) lastFix = s

        val now = when {
            spoofing.isNotEmpty() -> TrustState(TrustLevel.SPOOFING, spoofing + jamming + suspicious)
            jamming.isNotEmpty() -> TrustState(TrustLevel.JAMMING, jamming + suspicious)
            suspicious.isNotEmpty() -> TrustState(TrustLevel.SUSPICIOUS, suspicious)
            else -> TrustState()
        }
        if (now.level.locksCapture) {
            held = now
            heldUntilMs = s.elapsedMs + HOLD_MS
            return now
        }
        val h = held
        if (h != null && s.elapsedMs < heldUntilMs) return h.copy(reasons = (h.reasons + now.reasons).distinct())
        held = null
        return now
    }

    companion object {
        const val MAX_SPEED_MPS = 60.0          // ~215 km/h: no crew vehicle does this between two fixes
        const val MIN_JUMP_M = 150.0
        const val MAX_CLOCK_SKEW_MS = 2 * 60_000L
        const val MIN_SATS_FOR_CN0 = 6
        const val UNIFORM_CN0_MIN_MEAN = 42.0
        const val UNIFORM_CN0_MAX_SD = 1.5
        const val GOOD_ACCURACY_M = 20f
        const val AGC_WINDOW = 20
        const val AGC_DROP_DB = 10.0
        const val RECENT_FIX_MS = 5 * 60_000L
        const val LOST_FIX_MS = 20_000L
        const val JAMMED_CN0_MAX = 20.0
        const val BRANCH_MARGIN_M = 2_000.0
        const val HOLD_MS = 60_000L
    }
}
