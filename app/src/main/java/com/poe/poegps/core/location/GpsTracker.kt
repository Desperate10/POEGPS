package com.poe.poegps.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

/** What the GPS chip knows right now. */
data class GpsFix(
    val permission: Boolean = false,
    val enabled: Boolean = false,
    val location: Location? = null,
    val satellitesUsed: Int = 0,
    val satellitesVisible: Int = 0,
    /** C/N0 of every satellite heard, dB-Hz. */
    val cn0: List<Float> = emptyList(),
    /** Receiver gain, dB, where the chip reports it. */
    val agcDb: Double? = null,
) {
    /** A fix younger than [MAX_FIX_AGE_MS]; older ones mean the receiver lost the sky. */
    val current: Location?
        get() = location?.takeIf {
            SystemClock.elapsedRealtime() - it.elapsedRealtimeNanos / 1_000_000 < MAX_FIX_AGE_MS
        }

    val accuracy: Float? get() = current?.takeIf { it.hasAccuracy() }?.accuracy

    /** This reading as plain numbers for [TrustAssessor]. */
    fun toSample(): GnssSample {
        val l = current
        return GnssSample(
            elapsedMs = SystemClock.elapsedRealtime(),
            lat = l?.latitude,
            lng = l?.longitude,
            accuracyM = l?.takeIf { it.hasAccuracy() }?.accuracy,
            fixTimeMs = l?.time,
            wallClockMs = System.currentTimeMillis(),
            isMock = l?.let {
                if (Build.VERSION.SDK_INT >= 31) it.isMock else @Suppress("DEPRECATION") it.isFromMockProvider
            } == true,
            satellitesUsed = satellitesUsed,
            satellitesVisible = satellitesVisible,
            cn0 = cn0,
            agcDb = agcDb,
        )
    }

    companion object {
        const val MAX_FIX_AGE_MS = 10_000L
    }
}

/** GPS-only location plus satellite counts, as a cold flow that stops listening when collection stops. */
@Singleton
class GpsTracker @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun fixes(): Flow<GpsFix> = callbackFlow {
        if (!hasPermission()) {
            trySend(GpsFix(permission = false))
            awaitClose()
            return@callbackFlow
        }
        var fix = GpsFix(permission = true, enabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        trySend(fix)

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                fix = fix.copy(enabled = true, location = location)
                trySend(fix)
            }

            override fun onProviderEnabled(provider: String) {
                fix = fix.copy(enabled = true)
                trySend(fix)
            }

            override fun onProviderDisabled(provider: String) {
                fix = fix.copy(enabled = false, location = null, satellitesUsed = 0, satellitesVisible = 0)
                trySend(fix)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        val gnss = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                var used = 0
                val cn0 = ArrayList<Float>(status.satelliteCount)
                for (i in 0 until status.satelliteCount) {
                    if (status.usedInFix(i)) used++
                    status.getCn0DbHz(i).takeIf { it > 0f }?.let(cn0::add)
                }
                fix = fix.copy(satellitesUsed = used, satellitesVisible = status.satelliteCount, cn0 = cn0)
                trySend(fix)
            }
        }
        val measurements = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                val agc = agcOf(event) ?: return
                fix = fix.copy(agcDb = agc)
                trySend(fix)
            }
        }

        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_MS, 0f, listener, Looper.getMainLooper())
        manager.registerGnssStatusCallback(gnss, Handler(Looper.getMainLooper()))
        manager.registerGnssMeasurementsCallback(measurements, Handler(Looper.getMainLooper()))
        awaitClose {
            manager.removeUpdates(listener)
            manager.unregisterGnssStatusCallback(gnss)
            manager.unregisterGnssMeasurementsCallback(measurements)
        }
    }.conflate()

    // AGC moved from each measurement to the event in API 34; older chips may not report it at all
    @Suppress("DEPRECATION")
    private fun agcOf(event: GnssMeasurementsEvent): Double? = when {
        Build.VERSION.SDK_INT >= 34 -> event.gnssAutomaticGainControls.map { it.levelDb }
        Build.VERSION.SDK_INT >= 26 -> event.measurements.filter { it.hasAutomaticGainControlLevelDb() }
            .map { it.automaticGainControlLevelDb }
        else -> emptyList()
    }.takeIf { it.isNotEmpty() }?.average()

    private companion object {
        const val UPDATE_MS = 1000L
    }
}
