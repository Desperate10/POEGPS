package com.poe.poegps.feature.presentation.screens.editor

import android.location.Location
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
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
import com.poe.poegps.feature.presentation.map.LineMap
import com.poe.poegps.feature.presentation.map.MapConfig
import com.poe.poegps.feature.presentation.map.MapGps
import com.poe.poegps.feature.presentation.map.MapPole
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.main.plural
import java.util.Locale
import kotlin.math.roundToInt

enum class AddOption(val title: String, val subtitle: String) {
    NEW("Нова опора", "Опори ще немає в ГІС"),
    EXISTING_10("Існуюча опора 6-10 кВ", "Опора іншої лінії, вже є в ГІС"),
    EXISTING_04("Існуюча опора 0,4 кВ", "Опора іншої лінії, вже є в ГІС"),
    TP("ТП/РП", "Лінія починається або закінчується на ТП"),
    ABON_TP("Абонентська ТП", "ТП абонента"),
}

/** Everything the pole actions (B4) can do; the fragment opens the existing dialogs for most of them. */
interface PoleActions {
    fun capture(pole: OprDisplayable)
    fun createTap(pole: OprDisplayable)
    fun addRecloser(pole: OprDisplayable)
    fun clearCoordinates(pole: OprDisplayable)
    fun delete(pole: OprDisplayable)
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
    gisLines: String?,
    gisPoles: List<Pair<Double, Double>>,
    onBack: () -> Unit,
    onAdd: (AddOption) -> Unit,
    onSelect: (Int?) -> Unit,
    onGpsProblem: () -> Unit,
    actions: PoleActions,
) {
    val c = PoeTheme.colors
    val density = LocalDensity.current
    var sheetHeight by remember { mutableIntStateOf(0) }
    var recenter by remember { mutableIntStateOf(0) }
    var actionsFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    val captured = poles.filter { it.hasCoordinates }
    val target = poles.firstOrNull { it.id == selectedId } ?: poles.firstOrNull { !it.hasCoordinates }
    val targetIndex = poles.indexOf(target)
    // the span is measured from the captured pole just before the target
    val previous = (if (targetIndex >= 0) poles.take(targetIndex) else poles).lastOrNull { it.hasCoordinates }
    val here = gps.current
    val span = if (previous != null && here != null) distance(previous, here) else null

    Box(Modifier.fillMaxSize().background(c.mapGround)) {
        LineMap(
            tplnr = tplnr,
            colors = c,
            gisLines = gisLines,
            gisPoles = gisPoles,
            poles = captured.map { MapPole(it.id, it.shortLabel, it.lat.toDouble(), it.lng.toDouble(), it.id == target?.id) },
            gps = here?.let { MapGps(it.latitude, it.longitude, it.takeIf { l -> l.hasAccuracy() }?.accuracy) },
            recenter = recenter,
            topInset = 120.dp,
            bottomInset = with(density) { sheetHeight.toDp() } - 24.dp,
            onPoleClick = { actionsFor = it },
            modifier = Modifier.fillMaxSize(),
        )

        // header over the map
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PoeOverlayIconButton(PoeIcons.Back, "Назад до об'єктів", onBack)
            PoeOverlayTitle(
                title = lineName,
                subtitle = "${captured.size} з ${poles.size} опор" + if (category.isNotBlank()) " · $category" else "",
                modifier = Modifier.weight(1f),
            )
            PoeOverlayIconButton(PoeIcons.Plus, "Додати опору", { showAdd = true }, filled = true)
        }
        GpsStatus(gps, onGpsProblem, Modifier.align(Alignment.TopEnd).padding(top = 68.dp, end = 14.dp))

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
                poles.isEmpty() -> EmptyLine(onAdd = { showAdd = true })
                target == null -> LineDone(poles.size, onAdd = { showAdd = true }, onAll = { showAll = true })
                else -> NextPole(
                    pole = target,
                    gps = gps,
                    span = span,
                    upcoming = poles.drop(targetIndex + 1).filter { !it.hasCoordinates }.take(2),
                    total = poles.size,
                    onCapture = { actions.capture(target) },
                    onSelect = onSelect,
                    onAll = { showAll = true },
                    onGpsProblem = onGpsProblem,
                )
            }
        }
    }

    poles.firstOrNull { it.id == actionsFor }?.let { pole ->
        PoleActionsSheet(pole, gps, actions, onDismiss = { actionsFor = null })
    }
    if (showAdd) {
        AddSheet(onPick = { showAdd = false; onAdd(it) }, onDismiss = { showAdd = false })
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
private fun GpsStatus(gps: GpsFix, onProblem: () -> Unit, modifier: Modifier = Modifier) {
    val c = PoeTheme.colors
    val accuracy = gps.accuracy
    val (text, strong) = when {
        !gps.permission -> "Немає дозволу на GPS" to false
        !gps.enabled -> "GPS вимкнено" to false
        gps.current == null -> "Пошук супутників…" to false
        accuracy != null && gps.satellitesUsed > 0 -> "±${accuracy.format1()} м · ${gps.satellitesUsed} супутн." to true
        accuracy != null -> "±${accuracy.format1()} м" to true
        else -> "GPS" to true
    }
    val problem = !gps.permission || !gps.enabled
    Row(
        modifier
            .height(36.dp)
            .clip(PoeShapes.pill)
            .background(if (strong) c.gpsStrong else c.overlay)
            .then(if (problem) Modifier.clickable(role = Role.Button, onClick = onProblem) else Modifier)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (strong) {
            Icon(PoeIcons.Crosshair, null, tint = c.onGpsStrong, modifier = Modifier.size(16.dp))
        } else {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (problem) c.action else c.gps))
        }
        Text(text, style = PoeTheme.type.captionStrong, color = if (strong) c.onGpsStrong else if (problem) c.actionText else c.gpsText)
    }
}

@Composable
private fun ColumnScope.NextPole(
    pole: OprDisplayable,
    gps: GpsFix,
    span: Float?,
    upcoming: List<OprDisplayable>,
    total: Int,
    onCapture: () -> Unit,
    onSelect: (Int?) -> Unit,
    onAll: () -> Unit,
    onGpsProblem: () -> Unit,
) {
    val c = PoeTheme.colors
    val here = gps.current
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PoeOverline(if (pole.hasCoordinates) "Перезняти опору" else "Наступна опора")
            Text(pole.name, style = PoeTheme.type.hero, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            if (here != null) "${here.latitude.format5()}\n${here.longitude.format5()}" else "—\n—",
            style = PoeTheme.type.mono, color = c.muted, textAlign = TextAlign.End,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PoeStatTile(
            "Точність", gps.accuracy?.let { "${it.format1()} м" } ?: "—", Modifier.weight(1f),
            valueColor = if (gps.accuracy != null) c.gpsText else c.muted,
        )
        PoeStatTile(
            "Супутники", "${gps.satellitesUsed}", Modifier.weight(1f),
            suffix = if (gps.satellitesVisible > 0) "/${gps.satellitesVisible}" else null,
        )
        PoeStatTile("Прогон", span?.let { "${it.roundToInt()} м" } ?: "—", Modifier.weight(1f))
    }
    val waiting = when {
        !gps.permission -> "Дозвольте доступ до GPS"
        !gps.enabled -> "Увімкніть GPS"
        here == null -> "Чекаємо на GPS"
        else -> null
    }
    PoePrimaryButton(
        text = waiting ?: if (pole.hasCoordinates) "Перезняти координати" else "Зняти координати",
        onClick = onCapture,
        icon = if (waiting == null) PoeIcons.Capture else PoeIcons.Clock,
        enabled = waiting == null,
    )
    if (!gps.permission || !gps.enabled) {
        TextButton(onClick = onGpsProblem, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (!gps.permission) "Надати дозвіл" else "Відкрити налаштування GPS", style = PoeTheme.type.label, color = c.actionText)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Далі:", style = PoeTheme.type.caption, color = c.muted, modifier = Modifier.weight(1f))
        upcoming.forEach { p -> SmallButton(p.chipLabel, onClick = { onSelect(p.id) }) }
        SmallButton("Усі $total", onClick = onAll, outlined = true, trailing = PoeIcons.ChevronDown)
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
private fun ColumnScope.LineDone(total: Int, onAdd: () -> Unit, onAll: () -> Unit) {
    val c = PoeTheme.colors
    PoeOverline("Лінію знято", color = c.gpsText)
    Text("Усі $total ${plural(total, "опора має", "опори мають", "опор мають")} координати", style = PoeTheme.type.title, color = c.ink)
    Text("Вивантажте лінію на головному екрані або додайте ще опору.", style = PoeTheme.type.body, color = c.muted)
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
private fun PoleActionsSheet(pole: OprDisplayable, gps: GpsFix, actions: PoleActions, onDismiss: () -> Unit) {
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
                    Text(pole.name, style = PoeTheme.type.sheetTitle, color = c.ink)
                    Text(
                        if (pole.hasCoordinates) "${pole.lat}, ${pole.lng}" else "без координат",
                        style = PoeTheme.type.mono, color = c.inkSecondary,
                    )
                }
                when {
                    !pole.hasCoordinates -> PoeStatusBadge("не знято", BadgeTone.Neutral, Modifier.padding(top = 6.dp))
                    pole.isSent -> PoeStatusBadge("надіслано", BadgeTone.Done, Modifier.padding(top = 6.dp), icon = PoeIcons.Check)
                    else -> PoeStatusBadge("не надіслано", BadgeTone.Pending, Modifier.padding(top = 6.dp))
                }
            }
            val facts = listOfNotNull(
                pole.wire.takeIf { it.isNotBlank() }?.let { "Провід $it" },
                pole.recloserName.takeIf { it.isNotBlank() }?.let { "Апарат: $it" },
            )
            if (facts.isNotEmpty()) {
                Text(facts.joinToString("  ·  "), style = PoeTheme.type.caption, color = c.muted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PoeActionRow(
                    PoeIcons.Tap, "Створити відпайку", "Нова гілка від цієї опори",
                    onClick = { onDismiss(); actions.createTap(pole) },
                    iconBackground = c.action, iconTint = c.onAction,
                )
                PoeActionRow(
                    PoeIcons.Recloser, "Реклоузер / роз'єднувач",
                    pole.recloserName.ifBlank { "Комутаційний апарат на опорі" },
                    onClick = { onDismiss(); actions.addRecloser(pole) },
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
                    onClick = { onDismiss(); actions.capture(pole) },
                    iconBackground = if (c.isDark) c.gpsContainer else c.gps,
                    iconTint = if (c.isDark) c.gps else c.onGpsStrong,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pole.hasCoordinates) {
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
            title = { Text("Видалити ${pole.name}?", style = PoeTheme.type.statValue, color = c.ink) },
            text = { Text("Опору буде прибрано з лінії на цьому телефоні.", style = PoeTheme.type.body, color = c.muted) },
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
private fun AddSheet(onPick: (AddOption) -> Unit, onDismiss: () -> Unit) {
    val c = PoeTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = PoeShapes.sheet,
        dragHandle = { SheetHandle(c) },
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Додати до лінії", style = PoeTheme.type.title, color = c.ink, modifier = Modifier.padding(bottom = 8.dp))
            AddOption.entries.forEach { option ->
                PoeActionRow(
                    icon = if (option == AddOption.NEW) PoeIcons.Plus else PoeIcons.Capture,
                    title = option.title,
                    subtitle = option.subtitle,
                    onClick = { onPick(option) },
                    iconBackground = if (option == AddOption.NEW) c.action else c.iconTile,
                    iconTint = if (option == AddOption.NEW) c.onAction else c.onIconTile,
                )
            }
        }
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
            "Опори лінії · ${poles.count { it.hasCoordinates }} з ${poles.size}",
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
                                pole.hasCoordinates && pole.isSent -> c.gps
                                pole.hasCoordinates -> c.action
                                else -> c.surface
                            }
                        ).border(1.5.dp, if (pole.hasCoordinates) Color.Transparent else c.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${i + 1}", style = PoeTheme.type.badge,
                            color = when {
                                pole.hasCoordinates && pole.isSent -> if (c.isDark) c.mapGround else c.onGpsStrong
                                pole.hasCoordinates -> c.onAction
                                else -> c.muted
                            },
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pole.name, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                !pole.hasCoordinates -> "без координат"
                                pole.isSent -> "${pole.lat}, ${pole.lng} · надіслано"
                                else -> "${pole.lat}, ${pole.lng} · не надіслано"
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
private fun SheetHandle(c: PoeColors) {
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

private val OprDisplayable.chipLabel: String
    get() = name.replace("Опора", "Оп.")

private fun distance(from: OprDisplayable, to: Location): Float {
    val out = FloatArray(1)
    Location.distanceBetween(from.lat.toDouble(), from.lng.toDouble(), to.latitude, to.longitude, out)
    return out[0]
}

private fun Float.format1() = String.format(Locale.US, "%.1f", this)
private fun Double.format5() = String.format(Locale.US, "%.5f", this)
