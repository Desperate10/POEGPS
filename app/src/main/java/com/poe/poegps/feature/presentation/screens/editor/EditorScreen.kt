package com.poe.poegps.feature.presentation.screens.editor

import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.poe.poegps.feature.domain.GisTap
import com.poe.poegps.feature.domain.gisTapsAt
import com.poe.poegps.feature.presentation.map.lineShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
import com.poe.poegps.core.location.Geo
import com.poe.poegps.feature.presentation.capture.CapturePolicy
import com.poe.poegps.core.designsystem.PoeActionRow
import com.poe.poegps.core.designsystem.PoeColors
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoeOverlayIconButton
import com.poe.poegps.core.designsystem.PoeOverlayTitle
import com.poe.poegps.core.designsystem.PoeOverline
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeStatTile
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustLevel
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.data.local.CaptureMeta
import com.poe.poegps.feature.presentation.map.LineMap
import com.poe.poegps.feature.presentation.map.MapConfig
import com.poe.poegps.feature.presentation.map.MapFocus
import com.poe.poegps.feature.presentation.map.MapGps
import com.poe.poegps.feature.presentation.map.MapGisPole
import com.poe.poegps.feature.presentation.map.MapPole
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.main.plural
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Everything the pole actions (B4) can do; taps and reclosers go through [AddActions]. */
interface PoleActions {
    fun capture(pole: OprDisplayable, belowGate: Boolean)
    fun clearCoordinates(pole: OprDisplayable)
    fun delete(pole: OprDisplayable)
    fun placeManually(pole: OprDisplayable, lat: Double, lng: Double)

    /** Where GIS has [pole] (lat to lng) when it came from GIS and was moved since; null otherwise. */
    fun gisPosition(pole: OprDisplayable): Pair<Double, Double>? = null

    /** Undoes moves and re-captures of a GIS pole. */
    fun restoreGisPosition(pole: OprDisplayable) {}
}

/** B3 · Лінія: map with the line, the next pole to capture and the GPS readout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    tplnr: String,
    lineName: String,
    category: String,
    poles: List<OprDisplayable>,
    selectedId: Int?,
    gps: GpsFix,
    trust: TrustState,
    captureMeta: Map<Int, CaptureMeta>,
    gisLines: String?,
    gisPoles: List<MapGisPole>,
    onBack: () -> Unit,
    onSelect: (Int?) -> Unit,
    onGpsProblem: () -> Unit,
    actions: PoleActions,
    adds: AddActions,
) {
    val c = PoeTheme.colors
    val density = LocalDensity.current
    var sheetHeight by remember { mutableIntStateOf(0) }
    var recenter by remember { mutableIntStateOf(0) }
    var actionsFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    // B8/B9 opened from a pole's actions: the new pole goes right after this one (else at the end)
    var addAfter by rememberSaveable { mutableStateOf<Int?>(null) }
    // B9 picker, B10 recloser and B11 tap sheets (pole ids)
    var pickSource by rememberSaveable { mutableStateOf<PickSource?>(null) }
    var recloserFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var tapFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    // B6: the pole being placed by hand, the point under the pin, and camera moves for the map
    var adjustFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var focus by remember { mutableStateOf<MapFocus?>(null) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }

    val captured = poles.filter { it.hasCoordinates }
    // poles loaded from GIS have coordinates but aren't surveyed until the crew re-takes or moves them
    val surveyed = captured.count { !it.fromGis }
    val lineNames = remember(poles) { poles.map { it.name.trim() }.toSet() }
    val target = poles.firstOrNull { it.id == selectedId } ?: poles.firstOrNull { !it.hasCoordinates }
    val targetIndex = poles.indexOf(target)
    // the span is measured from the captured pole just before the target
    val previous = (if (targetIndex >= 0) poles.take(targetIndex) else poles).lastOrNull { it.hasCoordinates }
    val here = gps.current
    val span = if (previous != null && here != null) distance(previous, here) else null
    val adjusting = poles.firstOrNull { it.id == adjustFor }
    val previousOf = { pole: OprDisplayable -> poles.take(poles.indexOf(pole).coerceAtLeast(0)).lastOrNull { it.hasCoordinates } }

    fun focusOn(point: Pair<Double, Double>, zoom: Double?, animate: Boolean) {
        focus = MapFocus(point.first, point.second, zoom, (focus?.seq ?: 0) + 1, animate)
    }

    // start where the pole is, else at a trustworthy GPS fix, else at the previous pole
    fun startAdjust(pole: OprDisplayable) {
        actionsFor = null
        adjustFor = pole.id
        val start = when {
            pole.hasCoordinates -> pole.lat.toDouble() to pole.lng.toDouble()
            here != null && !trust.level.locksCapture -> here.latitude to here.longitude
            else -> previousOf(pole)?.let { it.lat.toDouble() to it.lng.toDouble() }
        }
        // instant: the sheet switching to B6 changes the map padding, which would cut an animation short
        start?.let { focusOn(it, 19.0, animate = false) }
    }

    Box(Modifier.fillMaxSize().background(c.mapGround)) {
        LineMap(
            tplnr = tplnr,
            colors = c,
            gisLines = gisLines,
            gisPoles = remember(gisPoles, lineNames) { gisPoles.map { if (it.label.trim() in lineNames) it.copy(label = "") else it } },
            poles = captured.map {
                MapPole(it.id, it.shortLabel, it.lat.toDouble(), it.lng.toDouble(), it.id == target?.id, fromGis = it.fromGis)
            },
            gps = here?.let { MapGps(it.latitude, it.longitude, it.takeIf { l -> l.hasAccuracy() }?.accuracy) },
            recenter = recenter,
            topInset = 120.dp,
            bottomInset = (with(density) { sheetHeight.toDp() } - 24.dp).coerceAtLeast(0.dp),
            onPoleClick = { if (adjusting == null) actionsFor = it },
            modifier = Modifier.fillMaxSize(),
            focus = focus,
            onCenterChange = { lat, lng -> center = lat to lng },
            // the span being measured: from the pole before the one to capture; none when nothing is next
            gpsLinkFrom = if (target != null) previous?.let { it.lat.toDouble() to it.lng.toDouble() } else null,
        )

        if (adjusting != null) {
            BackHandler { adjustFor = null }
            AdjustOverlay(
                pole = adjusting,
                center = center,
                gps = gps,
                trust = trust,
                previous = previousOf(adjusting),
                step = ADJUST_STEPS[stepIndex],
                topInset = 120.dp,
                bottomInset = (with(density) { sheetHeight.toDp() } - 24.dp).coerceAtLeast(0.dp),
                onSheetHeight = { sheetHeight = it },
                onStep = { stepIndex = (stepIndex + 1) % ADJUST_STEPS.size },
                onNudge = { north, east -> center?.let { focusOn(offsetBy(it, north, east), null, animate = false) } },
                onToGps = { here?.let { focusOn(it.latitude to it.longitude, null, animate = false) } },
                onSave = {
                    center?.let { (lat, lng) -> actions.placeManually(adjusting, lat, lng) }
                    adjustFor = null
                },
                onClose = { adjustFor = null },
            )
            return@Box
        }

        // header over the map
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PoeOverlayIconButton(PoeIcons.Back, "Назад до об'єктів", onBack)
            PoeOverlayTitle(
                title = lineName,
                subtitle = (if (poles.any { it.fromGis }) "уточнено $surveyed з ${poles.size}" else "$surveyed з ${poles.size} опор") +
                    if (category.isNotBlank()) " · $category" else "",
                modifier = Modifier.weight(1f),
            )
            PoeOverlayIconButton(PoeIcons.Plus, "Додати опору", { addAfter = null; showAdd = true }, filled = true)
        }
        GpsStatus(gps, trust, onGpsProblem, Modifier.align(Alignment.TopEnd).padding(top = 68.dp, end = 14.dp))

        // above the sheet: attribution and "back to me"
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = with(density) { sheetHeight.toDp() } + 10.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                MapConfig.ATTRIBUTION, style = PoeTheme.type.small, color = c.muted,
                modifier = Modifier.weight(1f).clip(PoeShapes.pill).background(c.overlay.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
            )
            if (here != null) PoeOverlayIconButton(PoeIcons.Crosshair, "Показати мою позицію", { recenter++ })
        }

        PoeSheet(
            Modifier.align(Alignment.BottomCenter).onSizeChanged { sheetHeight = it.height },
        ) {
            when {
                poles.isEmpty() -> EmptyLine(onAdd = { addAfter = null; showAdd = true })
                target == null -> LineDone(poles, onAdd = { addAfter = null; showAdd = true }, onAll = { showAll = true })
                else -> NextPole(
                    pole = target,
                    gps = gps,
                    trust = trust,
                    span = span,
                    upcoming = poles.drop(targetIndex + 1).filter { !it.hasCoordinates }.take(2),
                    total = poles.size,
                    onCapture = { belowGate -> actions.capture(target, belowGate) },
                    onManual = { startAdjust(target) },
                    onSelect = onSelect,
                    onAll = { showAll = true },
                    onGpsProblem = onGpsProblem,
                )
            }
        }

        pickSource?.let { source ->
            ExistingPicker(
                source = source,
                lineName = lineName,
                lastPole = poles.lastOrNull()?.name,
                taken = poles.map { it.name.trim() }.toSet(),
                tplnr = tplnr,
                here = here,
                actions = adds,
                afterId = addAfter,
                onAdded = { pickSource = null; addAfter = null },
                onClose = { pickSource = null; showAdd = true },
            )
        }
    }

    poles.firstOrNull { it.id == actionsFor }?.let { pole ->
        PoleActionsSheet(
            pole, gps, captureMeta[pole.id], actions,
            onTap = { tapFor = pole.id },
            onRecloser = { recloserFor = pole.id },
            onInsertAfter = { addAfter = pole.id; showAdd = true },
            onRetake = { onSelect(pole.id) },
            onAdjust = { startAdjust(pole) },
            onDismiss = { actionsFor = null },
        )
    }
    if (showAdd) {
        NewPoleSheet(
            lineName = lineName,
            poles = poles,
            after = poles.firstOrNull { it.id == addAfter },
            onCreate = { showAdd = false; adds.addPole(it, addAfter); addAfter = null },
            onPick = { showAdd = false; pickSource = it },
            onDismiss = { showAdd = false; addAfter = null },
        )
    }
    poles.firstOrNull { it.id == recloserFor }?.let { pole ->
        RecloserSheet(
            pole, lineName,
            onSave = { type, name -> recloserFor = null; adds.saveRecloser(pole, type, name) },
            onDismiss = { recloserFor = null },
        )
    }
    poles.firstOrNull { it.id == tapFor }?.let { pole ->
        // parsing the branch GeoJSON takes a moment: off the main thread, the sheet fills in when ready
        val gisTaps by produceState(emptyList<GisTap>(), gisLines, pole.id, lineName) {
            val json = gisLines ?: return@produceState
            value = withContext(Dispatchers.Default) {
                gisTapsAt(pole.name, pole.lat.toDoubleOrNull()?.takeIf { it != 0.0 }, pole.lng.toDoubleOrNull(),
                    lineShapes(json, tplnr), exclude = setOf(lineName))
            }
        }
        TapSheet(
            pole, lineName, gisTaps,
            onCreate = { first, open, gisName -> tapFor = null; adds.createTap(pole, first, open, gisName) },
            onDismiss = { tapFor = null },
        )
    }
    if (showAll) {
        AllPolesSheet(
            poles = poles,
            targetId = target?.id,
            onPick = { showAll = false; onSelect(it.id) },
            onMore = { showAll = false; actionsFor = it.id },
            onDismiss = { showAll = false },
        )
    }
}

@Composable
internal fun GpsStatus(gps: GpsFix, trust: TrustState, onProblem: () -> Unit, modifier: Modifier = Modifier) {
    val c = PoeTheme.colors
    val accuracy = gps.accuracy
    val problem = !gps.permission || !gps.enabled
    val alarm = trust.level != TrustLevel.RELIABLE
    val text = when {
        !gps.permission -> "Немає дозволу на GPS"
        !gps.enabled -> "GPS вимкнено"
        trust.level.locksCapture -> "${trust.level.label} GPS"
        gps.current == null -> "Пошук супутників…"
        alarm -> "${trust.level.label} · ±${accuracy?.format1() ?: "?"} м"
        accuracy == null || accuracy > CapturePolicy.ACCURACY_GATE_M -> "±${accuracy?.roundToInt() ?: "?"} м · шукаємо точніше"
        gps.satellitesUsed > 0 -> "±${accuracy.format1()} м · ${gps.satellitesUsed} супутн."
        else -> "±${accuracy.format1()} м"
    }
    val strong = !problem && !alarm && accuracy != null && accuracy <= CapturePolicy.ACCURACY_GATE_M
    val (bg, fg) = when {
        trust.level.locksCapture -> c.action to c.onAction
        alarm -> c.actionContainer to c.onActionContainer
        strong -> c.gpsStrong to c.onGpsStrong
        problem -> c.overlay to c.actionText
        else -> c.overlay to c.gpsText
    }
    Row(
        modifier
            .height(36.dp)
            .clip(PoeShapes.pill)
            .background(bg)
            .then(if (problem) Modifier.clickable(role = Role.Button, onClick = onProblem) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            alarm -> Icon(PoeIcons.Alert, null, tint = fg, modifier = Modifier.size(16.dp))
            strong -> Icon(PoeIcons.Crosshair, null, tint = fg, modifier = Modifier.size(16.dp))
            else -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (problem) c.action else c.gps))
        }
        Text(text, style = PoeTheme.type.captionStrong, color = fg)
    }
}

@Composable
private fun ColumnScope.NextPole(
    pole: OprDisplayable,
    gps: GpsFix,
    trust: TrustState,
    span: Float?,
    upcoming: List<OprDisplayable>,
    total: Int,
    onCapture: (belowGate: Boolean) -> Unit,
    onManual: () -> Unit,
    onSelect: (Int?) -> Unit,
    onAll: () -> Unit,
    onGpsProblem: () -> Unit,
) {
    val c = PoeTheme.colors
    val here = gps.current
    val accuracy = gps.accuracy
    val locked = trust.level.locksCapture
    val gateOpen = here != null && accuracy != null && accuracy <= CapturePolicy.ACCURACY_GATE_M
    val waiting = gps.permission && gps.enabled && !locked && !gateOpen

    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PoeOverline(
                if (pole.hasCoordinates) "Перезняти опору" else "Наступна опора",
                color = if (gateOpen && !locked) c.actionText else c.muted,
            )
            // the pole number is what the crew checks against the plate: smaller, never "Опора …"
            ShrinkToFitText(pole.title, PoeTheme.type.hero, c.ink)
        }
        if (waiting) {
            Text(
                "${gps.satellitesUsed} / ${gps.satellitesVisible}\nсупутн.",
                style = PoeTheme.type.label, color = c.muted, textAlign = TextAlign.End,
            )
        } else {
            Text(
                if (here != null) "${here.latitude.format5()}\n${here.longitude.format5()}" else "—\n—",
                style = PoeTheme.type.mono, color = c.muted, textAlign = TextAlign.End,
            )
        }
    }

    when {
        locked -> TrustLock(trust, onManual)
        waiting -> AccuracyGauge(accuracy)
        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PoeStatTile(
                "Точність", accuracy?.let { "${it.format1()} м" } ?: "—", Modifier.weight(1f),
                valueColor = if (accuracy != null) c.gpsText else c.muted,
            )
            PoeStatTile(
                "Супутники", "${gps.satellitesUsed}", Modifier.weight(1f),
                suffix = if (gps.satellitesVisible > 0) "/${gps.satellitesVisible}" else null,
            )
            PoeStatTile("Прогон", span?.let { "${it.roundToInt()} м" } ?: "—", Modifier.weight(1f))
        }
    }
    if (trust.level == TrustLevel.SUSPICIOUS) TrustWarning(trust)

    val blocker = when {
        !gps.permission -> "Дозвольте доступ до GPS"
        !gps.enabled -> "Увімкніть GPS"
        locked -> "Знімання вимкнено"
        here == null -> "Чекаємо на GPS"
        !gateOpen -> "Чекаємо точності ≤ ${CapturePolicy.ACCURACY_GATE_M.toInt()} м"
        else -> null
    }
    PoePrimaryButton(
        text = blocker ?: if (pole.hasCoordinates) "Перезняти координати" else "Зняти координати",
        onClick = { onCapture(false) },
        icon = if (blocker == null) PoeIcons.Capture else PoeIcons.Clock,
        enabled = blocker == null,
    )
    when {
        !gps.permission || !gps.enabled -> TextButton(onClick = onGpsProblem, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (!gps.permission) "Надати дозвіл" else "Відкрити налаштування GPS", style = PoeTheme.type.label, color = c.actionText)
        }
        // B13: the crew may knowingly take a worse fix, e.g. under trees right at the pole
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
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Далі:", style = PoeTheme.type.caption, color = c.muted, modifier = Modifier.weight(1f))
            upcoming.forEach { p -> SmallButton(p.chipLabel, onClick = { onSelect(p.id) }) }
            SmallButton("Усі $total", onClick = onAll, outlined = true, trailing = PoeIcons.ChevronDown)
        }
    }
}

/** B13: how far the fix is from the capture gate. */
@Composable
internal fun AccuracyGauge(accuracy: Float?) {
    val c = PoeTheme.colors
    val gate = CapturePolicy.ACCURACY_GATE_M
    val scaleMax = 20f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Точність", style = PoeTheme.type.tabSelected, color = c.ink, modifier = Modifier.weight(1f))
            Text(accuracy?.let { "±${it.roundToInt()} м" } ?: "—", style = PoeTheme.type.statValue, color = c.gpsText)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(c.surfaceMuted)
                .semantics { contentDescription = "Точність ${accuracy?.roundToInt() ?: "невідома"} метрів, потрібно не гірше $gate" },
        ) {
            Box(Modifier.fillMaxWidth(gate / scaleMax).fillMaxHeight().background(c.gpsContainer))
            Box(
                Modifier
                    .fillMaxWidth(((accuracy ?: scaleMax) / scaleMax).coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .background(c.gps.copy(alpha = 0.25f))
            )
            Box(Modifier.fillMaxWidth(gate / scaleMax).fillMaxHeight().wrapContentWidth(Alignment.End)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(c.ink))
            }
        }
        Row {
            Text("≤ ${gate.toInt()} м — можна знімати", style = PoeTheme.type.small, color = c.muted, modifier = Modifier.weight(1f))
            Text("відійдіть від дерев і будівель", style = PoeTheme.type.small, color = c.muted)
        }
    }
}

/** Jamming or spoofing: capture is refused and the crew is told why. */
@Composable
internal fun TrustLock(trust: TrustState, onManual: () -> Unit) {
    val c = PoeTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PoeShapes.row)
            .background(c.actionContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(PoeIcons.Alert, null, tint = c.onActionContainer, modifier = Modifier.size(20.dp))
            Text(
                if (trust.level == TrustLevel.JAMMING) "Схоже на глушіння GPS" else "Схоже на підміну GPS",
                style = PoeTheme.type.rowTitle, color = c.onActionContainer,
            )
        }
        trust.reasons.take(3).forEach { Text("• $it", style = PoeTheme.type.captionRegular, color = c.ink) }
        Text(
            "Координати зараз не знімаються, щоб не записати хибну позицію. Перевірка повторюється сама — " +
                "відійдіть від джерела перешкод або зачекайте. Або поставте опору на мапі вручну.",
            style = PoeTheme.type.captionRegular, color = c.muted,
        )
        PoeOutlinedButton("Поставити вручну на мапі", onManual, Modifier.fillMaxWidth().padding(top = 4.dp), icon = PoeIcons.Move)
    }
}

/** Something looks off, but the position may still be right: capture stays allowed. */
@Composable
internal fun TrustWarning(trust: TrustState) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PoeShapes.tile)
            .background(c.actionContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PoeIcons.Alert, null, tint = c.onActionContainer, modifier = Modifier.size(18.dp))
        Text(
            trust.reasons.joinToString("; ").ifEmpty { "Позиція GPS виглядає підозріло" },
            style = PoeTheme.type.captionRegular, color = c.onActionContainer,
        )
    }
}

@Composable
private fun ColumnScope.EmptyLine(onAdd: () -> Unit) {
    val c = PoeTheme.colors
    PoeOverline("Лінія без опор")
    Text("Опор ще немає", style = PoeTheme.type.sheetTitle, color = c.ink)
    Text("Додайте першу опору лінії — нову або існуючу з ГІС.", style = PoeTheme.type.body, color = c.muted)
    PoePrimaryButton("Додати опору", onAdd, icon = PoeIcons.Plus)
}

@Composable
private fun ColumnScope.LineDone(poles: List<OprDisplayable>, onAdd: () -> Unit, onAll: () -> Unit) {
    val c = PoeTheme.colors
    val total = poles.size
    val fromGis = poles.count { it.fromGis }
    val unsent = poles.count { it.hasCoordinates && !it.isSent }
    if (fromGis > 0) {
        // an existing line loaded from GIS: nothing to capture in order, the crew fixes what's wrong
        PoeOverline("Лінія з ГІС")
        Text(
            "Уточнено ${total - fromGis} з $total ${plural(total, "опори", "опор", "опор")}",
            style = PoeTheme.type.title, color = c.ink,
        )
        Text(
            if (unsent > 0) "Зміни не надіслано — вивантажте лінію на головному екрані."
            else "Торкніться опори на мапі, щоб перезняти або пересунути її. Пропущену опору додайте після сусідньої.",
            style = PoeTheme.type.body, color = c.muted,
        )
    } else {
        PoeOverline("Лінію знято", color = c.gpsText)
        Text("Усі $total ${plural(total, "опора має", "опори мають", "опор мають")} координати", style = PoeTheme.type.title, color = c.ink)
        Text("Вивантажте лінію на головному екрані або додайте ще опору.", style = PoeTheme.type.body, color = c.muted)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PoeOutlinedButton("Додати опору", onAdd, Modifier.weight(1f), icon = PoeIcons.Plus)
        PoeOutlinedButton("Усі опори", onAll, Modifier.weight(1f))
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, outlined: Boolean = false, trailing: ImageVector? = null) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .height(44.dp)
            .clip(PoeShapes.chip)
            .then(if (outlined) Modifier.border(1.5.dp, c.outlineVariant, PoeShapes.chip) else Modifier.background(c.surfaceMuted))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = PoeTheme.type.label, color = c.ink, maxLines = 1)
        if (trailing != null) Icon(trailing, null, tint = c.ink, modifier = Modifier.size(14.dp))
    }
}

/** B4 · Дії з опорою. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoleActionsSheet(
    pole: OprDisplayable,
    gps: GpsFix,
    meta: CaptureMeta?,
    actions: PoleActions,
    onTap: () -> Unit,
    onRecloser: () -> Unit,
    onInsertAfter: () -> Unit,
    onRetake: () -> Unit,
    onAdjust: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = PoeShapes.sheet,
        dragHandle = { SheetHandle(c) },
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pole.title, style = PoeTheme.type.sheetTitle, color = c.ink)
                    Text(
                        if (pole.hasCoordinates) "${pole.lat.coord5()}, ${pole.lng.coord5()}" else "без координат",
                        style = PoeTheme.type.mono, color = c.inkSecondary,
                    )
                }
                when {
                    !pole.hasCoordinates -> PoeStatusBadge("не знято", BadgeTone.Neutral, Modifier.padding(top = 6.dp))
                    pole.fromGis -> PoeStatusBadge("з ГІС", BadgeTone.Neutral, Modifier.padding(top = 6.dp))
                    pole.isSent -> PoeStatusBadge("надіслано", BadgeTone.Done, Modifier.padding(top = 6.dp), icon = PoeIcons.Check)
                    else -> PoeStatusBadge("не надіслано", BadgeTone.Pending, Modifier.padding(top = 6.dp))
                }
            }
            val facts = listOfNotNull(
                meta?.takeIf { pole.hasCoordinates }?.let { m ->
                    "Знято ${SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(m.timeMs))}" +
                        (m.accuracyM?.let { " · ±${it.format1()} м" } ?: "")
                },
                meta?.takeIf { pole.hasCoordinates && it.source == CaptureMeta.SOURCE_MANUAL }?.let { m ->
                    "поставлено вручну" + (m.gpsOffsetM?.let { " · ${it.roundToInt()} м від GPS" } ?: "")
                },
                meta?.takeIf { pole.hasCoordinates && it.source == CaptureMeta.SOURCE_GPS && it.belowAccuracyGate }
                    ?.let { "до досягнення точності" },
                meta?.takeIf { pole.hasCoordinates }?.confirmedWarnings?.firstOrNull()?.let { "підтверджено: $it" },
                meta?.takeIf { pole.hasCoordinates && it.trust != TrustLevel.RELIABLE.name }
                    ?.let { m -> TrustLevel.entries.firstOrNull { it.name == m.trust }?.label?.let { "GPS: $it" } },
                pole.wire.takeIf { it.isNotBlank() }?.let { "Провід $it" },
                pole.recloserName.takeIf { it.isNotBlank() }?.let { "Апарат: $it" },
            )
            if (facts.isNotEmpty()) {
                Text(facts.joinToString("  ·  "), style = PoeTheme.type.caption, color = c.muted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PoeActionRow(
                    PoeIcons.Tap, "Створити відпайку", "Нова гілка від цієї опори",
                    onClick = { onDismiss(); onTap() },
                    iconBackground = c.action, iconTint = c.onAction,
                )
                PoeActionRow(
                    PoeIcons.Recloser, "Реклоузер / роз'єднувач",
                    pole.recloserName.ifBlank { "Комутаційний апарат на опорі" },
                    onClick = { onDismiss(); onRecloser() },
                )
                val accuracy = gps.accuracy
                PoeActionRow(
                    if (pole.hasCoordinates) PoeIcons.Retake else PoeIcons.Capture,
                    if (pole.hasCoordinates) "Перезняти координати" else "Зняти координати",
                    when {
                        gps.current == null -> "Немає позиції GPS"
                        accuracy != null -> "Зараз: ±${accuracy.format1()} м"
                        else -> "Поточна позиція GPS"
                    },
                    // makes it the sheet's target, where the accuracy gate and the checks apply
                    onClick = { onDismiss(); onRetake() },
                    iconBackground = if (c.isDark) c.gpsContainer else c.gps,
                    iconTint = if (c.isDark) c.gps else c.onGpsStrong,
                )
                PoeActionRow(
                    PoeIcons.Move,
                    if (pole.hasCoordinates) "Уточнити на мапі" else "Поставити на мапі",
                    "Пересунути позначку вручну",
                    onClick = onAdjust,
                    iconBackground = c.surface, iconTint = c.action, iconBorder = c.action,
                )
                actions.gisPosition(pole)?.let { (gisLat, gisLng) ->
                    val off = pole.lat.toDoubleOrNull()?.takeIf { it != 0.0 }?.let { lat ->
                        pole.lng.toDoubleOrNull()?.let { Geo.distanceM(lat, it, gisLat, gisLng) }
                    }
                    PoeActionRow(
                        PoeIcons.Undo, "Повернути як у ГІС",
                        off?.takeIf { it >= 0.1 }?.let { "Скасувати зміни · зараз за ${if (it < 10) String.format(Locale.US, "%.1f", it) else it.roundToInt()} м від ГІС" }
                            ?: "Скасувати зміни",
                        onClick = { onDismiss(); actions.restoreGisPosition(pole) },
                    )
                }
                PoeActionRow(
                    PoeIcons.Plus, "Додати опору після цієї", "Пропущена опора між цією і наступною",
                    onClick = { onDismiss(); onInsertAfter() },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // a GIS pole without coordinates would drop out of the line the server rebuilds
                if (pole.hasCoordinates && !pole.fromGis) {
                    PoeOutlinedButton("Очистити координати", { onDismiss(); actions.clearCoordinates(pole) }, Modifier.weight(1f))
                }
                PoeOutlinedButton("Видалити", { confirmDelete = true }, Modifier.weight(1f), icon = PoeIcons.Trash, danger = true)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            title = { Text("Видалити ${pole.title}?", style = PoeTheme.type.statValue, color = c.ink) },
            text = {
                Text(
                    // the server only edits what it receives: a pole left out stays in GIS, and so does its bend
                    if (pole.fromGis || pole.isSent) "Опору буде прибрано з лінії на цьому телефоні. У ГІС опора й лінія залишаться без змін: якщо опори насправді немає, її треба видалити в ГІС."
                    else "Опору буде прибрано з лінії на цьому телефоні.",
                    style = PoeTheme.type.body, color = c.muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDismiss(); actions.delete(pole) }) {
                    Text("Видалити", style = PoeTheme.type.button, color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Скасувати", style = PoeTheme.type.button, color = c.ink) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllPolesSheet(
    poles: List<OprDisplayable>,
    targetId: Int?,
    onPick: (OprDisplayable) -> Unit,
    onMore: (OprDisplayable) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = PoeShapes.sheet,
        dragHandle = { SheetHandle(c) },
    ) {
        Text(
            "Опори лінії · ${poles.count { it.hasCoordinates && !it.fromGis }} з ${poles.size}",
            style = PoeTheme.type.title, color = c.ink,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )
        LazyColumn(
            Modifier.heightIn(max = 560.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(poles, key = { _, p -> p.id }) { i, pole ->
                val isTarget = pole.id == targetId
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(PoeShapes.row)
                        .background(if (isTarget) c.actionContainer else c.surfaceMuted)
                        .clickable(role = Role.Button, onClickLabel = "Знімати цю опору", onClick = { onPick(pole) })
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(
                            when {
                                pole.fromGis -> c.surface
                                pole.hasCoordinates && pole.isSent -> c.gps
                                pole.hasCoordinates -> c.action
                                else -> c.surface
                            }
                        ).border(
                            if (pole.fromGis) 2.5.dp else 1.5.dp,
                            when {
                                pole.fromGis -> c.ink
                                pole.hasCoordinates -> Color.Transparent
                                else -> c.outline
                            },
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${i + 1}", style = PoeTheme.type.badge,
                            color = when {
                                pole.fromGis -> c.ink
                                pole.hasCoordinates && pole.isSent -> if (c.isDark) c.mapGround else c.onGpsStrong
                                pole.hasCoordinates -> c.onAction
                                else -> c.muted
                            },
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pole.title, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                !pole.hasCoordinates -> "без координат"
                                pole.fromGis -> "${pole.lat.coord5()}, ${pole.lng.coord5()} · з ГІС"
                                pole.isSent -> "${pole.lat.coord5()}, ${pole.lng.coord5()} · надіслано"
                                else -> "${pole.lat.coord5()}, ${pole.lng.coord5()} · не надіслано"
                            },
                            style = PoeTheme.type.captionRegular, color = c.muted, maxLines = 1,
                        )
                    }
                    IconButton(onClick = { onMore(pole) }) {
                        Icon(PoeIcons.ChevronRight, "Дії з опорою ${pole.name}", tint = c.muted, modifier = Modifier.size(20.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun SheetHandle(c: PoeColors) {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 14.dp)
            .size(width = 40.dp, height = 5.dp)
            .clip(PoeShapes.pill)
            .background(c.outlineVariant)
    )
}

val OprDisplayable.hasCoordinates: Boolean
    get() = lat.toDoubleOrNull()?.let { it != 0.0 } == true && lng.toDoubleOrNull()?.let { it != 0.0 } == true

/** "Опора 12" → "12" for map labels; anything without a number keeps its name. */
private val OprDisplayable.shortLabel: String
    get() = Regex("\\d+\\S*").find(name)?.value ?: name

/** Poles are named by bare numbers ("17"); screens say "Опора 17". Other names (ТП-245) stay as they are. */
fun poleTitle(name: String): String = if (name.firstOrNull()?.isDigit() == true) "Опора $name" else name

val OprDisplayable.title: String
    get() = poleTitle(name)

private val OprDisplayable.chipLabel: String
    get() = if (name.firstOrNull()?.isDigit() == true) "Оп. $name" else name.replace("Опора", "Оп.")

private fun distance(from: OprDisplayable, to: Location): Float {
    val out = FloatArray(1)
    Location.distanceBetween(from.lat.toDouble(), from.lng.toDouble(), to.latitude, to.longitude, out)
    return out[0]
}

/** One line of [text] in [style], stepped down in size (to 60% at most) until it fits instead of being cut. */
@Composable
private fun ShrinkToFitText(text: String, style: TextStyle, color: Color) {
    var size by remember(text, style) { mutableStateOf(style.fontSize) }
    var fits by remember(text, style) { mutableStateOf(false) }
    Text(
        text,
        style = style.copy(fontSize = size, lineHeight = size * 1.05f),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && size.value > style.fontSize.value * 0.6f) size *= 0.9f else fits = true
        },
        // the oversized tries aren't drawn, so the title doesn't flicker
        modifier = Modifier.drawWithContent { if (fits) drawContent() },
    )
}

internal fun Float.format1() = String.format(Locale.US, "%.1f", this)

/** GIS keeps up to 14 decimals; 5 (about a metre) is what a screen needs. Stored values stay as they are. */
internal fun String.coord5(): String = toDoubleOrNull()?.let { String.format(Locale.US, "%.5f", it) } ?: this
internal fun Double.format5() = String.format(Locale.US, "%.5f", this)
