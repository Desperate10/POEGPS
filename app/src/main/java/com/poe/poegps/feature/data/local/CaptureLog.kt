package com.poe.poegps.feature.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** How one pole's coordinates were taken. */
@Serializable
data class CaptureMeta(
    val timeMs: Long,
    val accuracyM: Float? = null,
    val satellites: Int = 0,
    /** TrustLevel name at the moment of capture. */
    val trust: String,
    val reasons: List<String> = emptyList(),
    /** Taken before accuracy reached the gate, on the crew's explicit request. */
    val belowAccuracyGate: Boolean = false,
    /** Plausibility warnings the crew confirmed past ("span too long", "far from GIS"). */
    val confirmedWarnings: List<String> = emptyList(),
    /** "gps", or "manual" when placed on the map (B6). */
    val source: String = SOURCE_GPS,
    /** For a manual placement: how far it is from the GPS position at the time, if there was one. */
    val gpsOffsetM: Float? = null,
) {
    companion object {
        const val SOURCE_GPS = "gps"
        const val SOURCE_MANUAL = "manual"
    }
}

/**
 * Capture metadata kept on the phone only: the GIS database gets no new columns, and the upload
 * contract is unchanged. Keys: [poleKey] for a line_pillar row, [tpKey] for a TP.
 */
@Singleton
class CaptureLog @Inject constructor(@ApplicationContext context: Context) {

    private val file = File(context.filesDir, "capture_log.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<Map<String, CaptureMeta>>(emptyMap())
    val entries: StateFlow<Map<String, CaptureMeta>> = _entries
    private var loaded = false

    suspend fun load() = mutex.withLock { ensureLoaded() }

    suspend fun record(key: String, meta: CaptureMeta) = update { it + (key to meta) }

    suspend fun forget(key: String) = update { it - key }

    suspend fun forgetAll(keys: Collection<String>) = update { it - keys.toSet() }

    private suspend fun update(change: (Map<String, CaptureMeta>) -> Map<String, CaptureMeta>) = mutex.withLock {
        ensureLoaded()
        val next = change(_entries.value)
        _entries.value = next
        withContext(Dispatchers.IO) { file.writeText(json.encodeToString(next)) }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        _entries.value = withContext(Dispatchers.IO) {
            runCatching {
                json.decodeFromString<Map<String, CaptureMeta>>(file.readText())
            }.getOrDefault(emptyMap())
        }
        loaded = true
    }

    companion object {
        fun poleKey(id: Int) = id.toString()
        fun tpKey(tplnr: String) = "tp:$tplnr"
    }
}
