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
)

/**
 * Capture metadata kept on the phone only: the GIS database gets no new columns, and the upload
 * contract is unchanged. Keyed by the line_pillar row id.
 */
@Singleton
class CaptureLog @Inject constructor(@ApplicationContext context: Context) {

    private val file = File(context.filesDir, "capture_log.json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<Map<Int, CaptureMeta>>(emptyMap())
    val entries: StateFlow<Map<Int, CaptureMeta>> = _entries
    private var loaded = false

    suspend fun load() = mutex.withLock { ensureLoaded() }

    suspend fun record(poleId: Int, meta: CaptureMeta) = update { it + (poleId to meta) }

    suspend fun forget(poleId: Int) = update { it - poleId }

    private suspend fun update(change: (Map<Int, CaptureMeta>) -> Map<Int, CaptureMeta>) = mutex.withLock {
        ensureLoaded()
        val next = change(_entries.value)
        _entries.value = next
        withContext(Dispatchers.IO) { file.writeText(json.encodeToString(next.mapKeys { it.key.toString() })) }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        _entries.value = withContext(Dispatchers.IO) {
            runCatching {
                json.decodeFromString<Map<String, CaptureMeta>>(file.readText()).mapKeys { it.key.toInt() }
            }.getOrDefault(emptyMap())
        }
        loaded = true
    }
}
