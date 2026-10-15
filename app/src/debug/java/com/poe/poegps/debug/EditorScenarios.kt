package com.poe.poegps.debug

import android.location.Location
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustLevel
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.EditorScreen
import com.poe.poegps.feature.presentation.screens.editor.PoleActions

/**
 * The line editor in GPS states the emulator can't produce (it always reports ±5 m and never
 * looks jammed): `--es screen gps-waiting | gps-suspicious | gps-spoofing | gps-jamming`.
 */
@Composable
fun EditorScenario(name: String, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    var selected by remember { mutableStateOf<Int?>(null) }
    val poles = List(24) { i ->
        val captured = i < 18
        OprDisplayable(
            id = i + 1, tplnr = "02-PS15-L12", name = "Опора ${i + 1}", parentName = "Л-12",
            category = "0,4 кВ", isAbon = false, pillarType = "Опора",
            lat = if (captured) "%.5f".format(java.util.Locale.US, 49.587 + (i + 1) * 0.0004) else "0.0",
            lng = if (captured) "%.5f".format(java.util.Locale.US, 34.554 + (i + 1) * 0.0005) else "0.0",
            isSent = i < 12,
        )
    }
    val here = Location("gps").apply {
        latitude = 49.5949; longitude = 34.5639
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        time = System.currentTimeMillis()
        accuracy = if (name == "gps-waiting") 18f else 2.8f
    }
    val gps = when (name) {
        "gps-jamming" -> GpsFix(permission = true, enabled = true, satellitesUsed = 0, satellitesVisible = 9)
        "gps-waiting" -> GpsFix(permission = true, enabled = true, location = here, satellitesUsed = 4, satellitesVisible = 22)
        else -> GpsFix(permission = true, enabled = true, location = here, satellitesUsed = 14, satellitesVisible = 22)
    }
    val trust = when (name) {
        "gps-suspicious" -> TrustState(TrustLevel.SUSPICIOUS, listOf("Час GPS не збігається з часом телефона"))
        "gps-spoofing" -> TrustState(
            TrustLevel.SPOOFING,
            listOf("Позицію підставляє інший застосунок (фіктивне місцезнаходження)", "Позиція стрибнула на 1240 м за 2 с"),
        )
        "gps-jamming" -> TrustState(TrustLevel.JAMMING, listOf("Супутники видно, але сигнал заглушено"))
        else -> TrustState()
    }
    // the real screen sits below the status bar (MainActivity pads it); do the same here
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        EditorScreen(
            tplnr = "02-PS15-L12",
            lineName = "ПЛ-0,4 кВ Л-12 · Кохнівка",
            category = "0,4 кВ",
            poles = poles,
            selectedId = selected,
            gps = gps,
            trust = trust,
            captureMeta = emptyMap(),
            gisLines = null,
            gisPoles = emptyList(),
            onBack = onExit,
            onAdd = {},
            onSelect = { selected = it },
            onGpsProblem = {},
            actions = object : PoleActions {
                override fun capture(pole: OprDisplayable, belowGate: Boolean) {}
                override fun createTap(pole: OprDisplayable) {}
                override fun addRecloser(pole: OprDisplayable) {}
                override fun clearCoordinates(pole: OprDisplayable) {}
                override fun delete(pole: OprDisplayable) {}
                override fun placeManually(pole: OprDisplayable, lat: Double, lng: Double) {}
            },
        )
    }
}
