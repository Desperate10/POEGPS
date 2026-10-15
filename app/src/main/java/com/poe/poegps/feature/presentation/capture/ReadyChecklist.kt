package com.poe.poegps.feature.presentation.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.location.GpsFix
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * B12 · Дозволи і GPS, over a capture screen. Shown while [show] and location isn't ready (precise
 * permission + GPS on); it closes by itself once both are fine. [gps] only triggers the re-check:
 * permission and the GPS switch are read from the system so the screen never flashes on entry.
 */
@Composable
fun ReadyChecklist(
    show: Boolean,
    gps: GpsFix,
    downloadedAt: Long?,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    // also re-read on resume: coming back from the system settings
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val state = remember(gps, lifecycle) { context.locationState() }
    LaunchedEffect(state.ready) { if (state.ready) onDismiss() }
    if (!show || state.ready) return

    BackHandler(onBack = onBack)
    val c = PoeTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.ground)
            // the map underneath must not pan
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(132.dp).clip(CircleShape).background(c.gpsContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PoeIcons.Crosshair, null, tint = c.gps, modifier = Modifier.size(64.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Майже готово до зйомки",
                style = PoeTheme.type.screenTitle.copy(fontSize = 38.sp, lineHeight = 40.sp), color = c.ink,
            )
            Text(
                "Геолокація працює лише поки відкрита лінія або ТП. Фонового стеження немає.",
                style = PoeTheme.type.body.copy(fontSize = 15.sp), color = c.inkSecondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckRow(
                ok = state.precise,
                title = "Точна геолокація",
                subtitle = when {
                    state.precise -> "Дозволено"
                    state.coarse -> "Дозволено лише приблизну — потрібна точна"
                    else -> "Потрібна, щоб знімати координати"
                },
                action = if (state.precise) null else "Дозволити",
                onAction = onGrant,
            )
            CheckRow(
                ok = state.gpsOn,
                title = if (state.gpsOn) "GPS увімкнено" else "GPS вимкнено",
                subtitle = if (state.gpsOn) "Супутники шукатимуться на лінії" else "Без нього координати не зняти",
                action = if (state.gpsOn) null else "Увімкнути",
                onAction = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            )
            CheckRow(
                ok = downloadedAt != null,
                title = "Дані філії на пристрої",
                subtitle = downloadedAt?.let { "Оновлено ${downloadedLabel(it)}" }
                    ?: "Ще не завантажено — «Завантажити» на екрані об'єктів",
                // not a blocker: lines already on the phone can be captured
                warn = false,
            )
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PoePrimaryButton("Почати зйомку", onDismiss, enabled = false, disabledBackground = c.tabTrack)
            Text(
                "Відкриється сама, щойно ${if (!state.precise) "дозвіл буде надано" else "GPS увімкнеться"}",
                style = PoeTheme.type.caption, color = c.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // e.g. to place poles by hand (B6) or just to look at the line
            Text(
                "Переглянути без GPS",
                style = PoeTheme.type.label.copy(textDecoration = TextDecoration.Underline), color = c.actionText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CheckRow(
    ok: Boolean,
    title: String,
    subtitle: String,
    action: String? = null,
    onAction: () -> Unit = {},
    warn: Boolean = true,
) {
    val c = PoeTheme.colors
    val alarm = !ok && warn
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(PoeShapes.row)
            .background(c.surface)
            .border(if (alarm) 2.5.dp else 1.dp, if (alarm) c.action else c.cardBorder, PoeShapes.row)
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (icon: ImageVector, bg, tint) = when {
            ok -> Triple(PoeIcons.Check, c.gpsContainer, c.gps)
            alarm -> Triple(PoeIcons.Alert, c.actionContainer, c.onActionContainer)
            else -> Triple(PoeIcons.Download, c.surfaceMuted, c.muted)
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PoeTheme.type.rowTitle, color = c.ink)
            Text(subtitle, style = PoeTheme.type.captionRegular, color = if (alarm) c.actionText else c.muted)
        }
        if (action != null) {
            Box(
                Modifier
                    .height(48.dp)
                    .clip(PoeShapes.chip)
                    .background(c.action)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(action, style = PoeTheme.type.button, color = c.onAction) }
        }
    }
}

private data class LocationState(val precise: Boolean, val coarse: Boolean, val gpsOn: Boolean) {
    val ready get() = precise && gpsOn
}

private fun Context.locationState(): LocationState {
    fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationState(
        precise = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        gpsOn = manager.isProviderEnabled(LocationManager.GPS_PROVIDER),
    )
}

/** "сьогодні о 08:42", "вчора о 17:10", "12.10 о 08:42". */
private fun downloadedLabel(timeMs: Long): String {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
    val day = when {
        DateUtils.isToday(timeMs) -> "сьогодні"
        DateUtils.isToday(timeMs + DateUtils.DAY_IN_MILLIS) -> "вчора"
        else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(timeMs))
    }
    return "$day о $time"
}
