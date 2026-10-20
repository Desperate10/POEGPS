package com.poe.poegps.feature.presentation.screens.tpeditor

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoeOverlayIconButton
import com.poe.poegps.core.designsystem.PoeOverlayTitle
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustLevel
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.presentation.capture.CapturePolicy
import com.poe.poegps.feature.presentation.map.LineMap
import com.poe.poegps.feature.presentation.map.MapConfig
import com.poe.poegps.feature.presentation.map.rememberSatellite
import com.poe.poegps.feature.presentation.map.MapFocus
import com.poe.poegps.feature.presentation.map.MapGps
import com.poe.poegps.feature.presentation.map.MapPole
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.ADJUST_STEPS
import com.poe.poegps.feature.presentation.screens.editor.AccuracyGauge
import com.poe.poegps.feature.presentation.screens.editor.AdjustOverlay
import com.poe.poegps.feature.presentation.screens.editor.GpsStatus
import com.poe.poegps.feature.presentation.screens.editor.TrustLock
import com.poe.poegps.feature.presentation.screens.editor.TrustWarning
import com.poe.poegps.feature.presentation.screens.editor.format1
import com.poe.poegps.feature.presentation.screens.editor.format5
import com.poe.poegps.feature.presentation.screens.editor.hasCoordinates
import com.poe.poegps.feature.presentation.screens.editor.offsetBy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** B7 · ТП/РП — зйомка точки: one point per substation, taken at its door. */
@Composable
fun TpEditorScreen(
    tp: OprDisplayable?,
    title: String,
    tplnr: String,
    progress: LineProgress?,
    meta: CaptureMeta?,
    gps: GpsFix,
    trust: TrustState,
    gisLines: String?,
    onBack: () -> Unit,
    onCapture: (belowGate: Boolean) -> Unit,
    onPlace: (lat: Double, lng: Double) -> Unit,
    onGpsProblem: () -> Unit,
) {
    val c = PoeTheme.colors
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var sheetHeight by remember { mutableIntStateOf(0) }
    var recenter by remember { mutableIntStateOf(0) }
    var satellite by rememberSatellite()
    var adjusting by rememberSaveable { mutableStateOf(false) }
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val here = gps.current

    fun focusOn(point: Pair<Double, Double>, zoom: Double?) {
        focus = MapFocus(point.first, point.second, zoom, (focus?.seq ?: 0) + 1, animate = false)
    }

    fun startAdjust() {
        val start = when {
            tp?.hasCoordinates == true -> tp.lat.toDouble() to tp.lng.toDouble()
            here != null && !trust.level.locksCapture -> here.latitude to here.longitude
            else -> null
        }
        adjusting = true
        start?.let { focusOn(it, 19.0) }
    }

    Box(Modifier.fillMaxSize().background(c.mapGround)) {
        LineMap(
            tplnr = tplnr,
            colors = c,
            gisLines = gisLines,
            gisPoles = emptyList(),
            poles = listOfNotNull(tp?.takeIf { it.hasCoordinates }?.let {
                MapPole(it.id, it.name, it.lat.toDouble(), it.lng.toDouble(), selected = true)
            }),
            gps = here?.let { MapGps(it.latitude, it.longitude, it.takeIf { l -> l.hasAccuracy() }?.accuracy) },
            recenter = recenter,
            topInset = 120.dp + statusTop,
            bottomInset = (with(density) { sheetHeight.toDp() } - 24.dp).coerceAtLeast(0.dp),
            onPoleClick = {},
            modifier = Modifier.fillMaxSize(),
            focus = focus,
            onCenterChange = { lat, lng -> center = lat to lng },
            satellite = satellite,
        )

        if (adjusting && tp != null) {
            BackHandler { adjusting = false }
            AdjustOverlay(
                pole = tp,
                center = center,
                gps = gps,
                trust = trust,
                previous = null,
                step = ADJUST_STEPS[stepIndex],
                topInset = 120.dp + statusTop,
                bottomInset = (with(density) { sheetHeight.toDp() } - 24.dp).coerceAtLeast(0.dp),
                onSheetHeight = { sheetHeight = it },
                onStep = { stepIndex = (stepIndex + 1) % ADJUST_STEPS.size },
                onNudge = { north, east -> center?.let { focusOn(offsetBy(it, north, east), null) } },
                onToGps = { here?.let { focusOn(it.latitude to it.longitude, null) } },
                onSave = {
                    center?.let { (lat, lng) -> onPlace(lat, lng) }
                    adjusting = false
                },
                onClose = { adjusting = false },
            )
            return@Box
        }

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PoeOverlayIconButton(PoeIcons.Back, "Назад до об'єктів", onBack)
            PoeOverlayTitle(title, "ТП/РП · $tplnr", Modifier.weight(1f))
        }
        GpsStatus(gps, trust, onGpsProblem, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 68.dp, end = 14.dp))

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = with(density) { sheetHeight.toDp() } + 10.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                MapConfig.attribution(satellite), style = PoeTheme.type.small, color = c.muted,
                modifier = Modifier.weight(1f).clip(PoeShapes.pill).background(c.overlay.copy(alpha = 0.8f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            PoeOverlayIconButton(
                PoeIcons.Layers,
                if (satellite) "Показати карту" else "Показати супутник",
                { satellite = !satellite },
                filled = satellite,
            )
            if (here != null) {
                Spacer(Modifier.width(8.dp))
                PoeOverlayIconButton(PoeIcons.Crosshair, "Показати мою позицію", { recenter++ })
            }
        }

        PoeSheet(Modifier.align(Alignment.BottomCenter).onSizeChanged { sheetHeight = it.height }) {
            TpSheet(tp, title, progress, meta, gps, trust, onCapture, ::startAdjust, onGpsProblem)
        }
    }
}

@Composable
private fun ColumnScope.TpSheet(
    tp: OprDisplayable?,
    title: String,
    progress: LineProgress?,
    meta: CaptureMeta?,
    gps: GpsFix,
    trust: TrustState,
    onCapture: (belowGate: Boolean) -> Unit,
    onManual: () -> Unit,
    onGpsProblem: () -> Unit,
) {
    val c = PoeTheme.colors
    val here = gps.current
    val accuracy = gps.accuracy
    val locked = trust.level.locksCapture
    val gateOpen = here != null && accuracy != null && accuracy <= CapturePolicy.ACCURACY_GATE_M
    val waiting = gps.permission && gps.enabled && !locked && !gateOpen
    val captured = tp?.hasCoordinates == true

    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "ТП/РП", style = PoeTheme.type.badge, color = c.onInverse,
                    modifier = Modifier.clip(PoeShapes.pill).background(c.inverse).padding(horizontal = 10.dp, vertical = 5.dp),
                )
                when {
                    progress != null && progress.unsent > 0 -> PoeStatusBadge("не надіслано", BadgeTone.Pending)
                    progress != null && progress.captured > 0 -> PoeStatusBadge("надіслано", BadgeTone.Done, icon = PoeIcons.Check)
                    captured -> PoeStatusBadge("координати з ГІС", BadgeTone.Neutral)
                    else -> PoeStatusBadge("без координат", BadgeTone.Neutral)
                }
            }
            Text(title, style = PoeTheme.type.hero, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            if (here != null) "${here.latitude.format5()}\n${here.longitude.format5()}" else "—\n—",
            style = PoeTheme.type.mono, color = c.muted, textAlign = TextAlign.End,
        )
    }

    when {
        locked -> TrustLock(trust, onManual)
        waiting -> AccuracyGauge(accuracy)
        else -> Text(
            "Станьте біля дверей ТП, щоб точка лягла на будівлю, а не на під'їзну дорогу.",
            style = PoeTheme.type.body, color = c.inkSecondary,
        )
    }
    if (trust.level == TrustLevel.SUSPICIOUS) TrustWarning(trust)

    meta?.takeIf { captured }?.let { m ->
        Text(
            listOfNotNull(
                "Знято ${SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(m.timeMs))}",
                m.accuracyM?.let { "±${it.format1()} м" },
                "вручну".takeIf { m.source == CaptureMeta.SOURCE_MANUAL },
            ).joinToString(" · "),
            style = PoeTheme.type.caption, color = c.muted,
        )
    }

    val blocker = when {
        !gps.permission -> "Дозвольте доступ до GPS"
        !gps.enabled -> "Увімкніть GPS"
        locked -> "Знімання вимкнено"
        here == null -> "Чекаємо на GPS"
        !gateOpen -> "Чекаємо точності ≤ ${CapturePolicy.ACCURACY_GATE_M.toInt()} м"
        else -> null
    }
    PoePrimaryButton(
        text = blocker ?: if (captured) "Перезняти координати ТП" else "Зняти координати ТП",
        onClick = { onCapture(false) },
        icon = if (blocker == null) PoeIcons.Capture else PoeIcons.Clock,
        enabled = blocker == null,
    )
    when {
        !gps.permission || !gps.enabled -> TextButton(onClick = onGpsProblem, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (!gps.permission) "Надати дозвіл" else "Відкрити налаштування GPS", style = PoeTheme.type.label, color = c.actionText)
        }
        !locked && here != null && accuracy != null && !gateOpen -> TextButton(
            onClick = { onCapture(true) },
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 44.dp),
        ) {
            Text(
                "Зняти зараз з похибкою ±${accuracy.roundToInt()} м",
                style = PoeTheme.type.label.copy(textDecoration = TextDecoration.Underline),
                color = c.actionText,
            )
        }
    }
    if (!locked) {
        PoeOutlinedButton("Поставити точку на мапі вручну", onManual, Modifier.fillMaxWidth(), icon = PoeIcons.Move)
    }
}
