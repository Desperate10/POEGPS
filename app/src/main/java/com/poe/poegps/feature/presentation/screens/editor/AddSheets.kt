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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOverlayIconButton
import com.poe.poegps.core.designsystem.PoeOverline
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeSearchField
import com.poe.poegps.core.designsystem.PoeSegmentedTabs
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.location.Geo
import com.poe.poegps.feature.domain.GisTap
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserType
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** Where an already captured point comes from (B8's four tiles, B9's lists). */
enum class PickSource(val tileLabel: String, val tileValue: String, val title: String, val subtitle: String) {
    POLE_10("Опора з лінії", "6-10 кВ", "Існуюча опора", "з ліній 6-10 кВ"),
    POLE_04("Опора з лінії", "0,4 кВ", "Існуюча опора", "з ліній 0,4 кВ"),
    TP("Точка лінії", "ТП/РП", "ТП/РП", "лінія починається або закінчується на ТП"),
    ABON_TP("Точка лінії", "Абон. ТП", "Абонентська ТП", "ТП абонента");

    val isTp get() = this == TP || this == ABON_TP
}

/** A GIS point offered in B9; [line] is the line it belongs to (empty for TPs). */
data class PickItem(
    val key: String,
    val name: String,
    val line: String,
    val lat: String,
    val lng: String,
    val category: String,
    val isAbon: Boolean,
    /** The GIS object of a pole ("" for TPs). */
    val tplnr: String = "",
) {
    val hasCoordinates: Boolean
        get() = lat.toDoubleOrNull()?.let { it != 0.0 } == true && lng.toDoubleOrNull()?.let { it != 0.0 } == true
}

/** What B8–B11 need from the screen's owner. */
interface AddActions {
    /** [afterId]: put it right after that pole instead of at the end of the line. */
    fun addPole(name: String, afterId: Int? = null)
    fun addExisting(source: PickSource, item: PickItem, afterId: Int? = null)
    fun lines(source: PickSource): Flow<List<ObjectDisplayable>>
    fun candidates(source: PickSource, line: ObjectDisplayable?): Flow<List<PickItem>>
    suspend fun nearby(source: PickSource, lat: Double, lng: Double): List<PickItem>
    /** [gisName]: the exact name of a tap GIS already has, instead of a generated one. */
    fun createTap(from: OprDisplayable, firstPole: String, open: Boolean, gisName: String?)
    fun saveRecloser(pole: OprDisplayable, type: RecloserType, name: String)
}

/**
 * B8 suggestions after [last]: the next number first, then "13б" (GIS marks extra poles with б), "13/1" and the one after next.
 * Keeps the last name's prefix ("Опора 13" → "Опора 14"); names already on the line are skipped.
 */
internal fun nextPoleNames(last: String?, taken: Collection<String>): List<String> {
    val trimmed = last?.trim().orEmpty()
    if (trimmed.isEmpty()) return listOf("1", "2").filter { it !in taken }
    val match = Regex("^(.*?)(\\d+)(\\D*)$").find(trimmed) ?: return emptyList()
    val prefix = match.groupValues[1]
    val number = match.groupValues[2].toLongOrNull() ?: return emptyList()
    val base = prefix + match.groupValues[2]
    return listOf("$prefix${number + 1}", "${base}б", "$base/1", "$prefix${number + 2}")
        .filter { it !in taken }
        .distinct()
}

/** B8 · Нова опора, with the way into B9 for points GIS already has. */
@Composable
internal fun NewPoleSheet(
    lineName: String,
    poles: List<OprDisplayable>,
    onCreate: (String) -> Unit,
    onPick: (PickSource) -> Unit,
    onDismiss: () -> Unit,
    // opened from a pole's actions: the new pole goes right after it, and numbering follows it
    after: OprDisplayable? = null,
) {
    val c = PoeTheme.colors
    // number on from the last pole, not from a TP added as the line's end point (pillarType "")
    val last = after ?: poles.lastOrNull { it.pillarType.isNotEmpty() }
    val taken = remember(poles) { poles.map { it.name.trim() }.toSet() }
    val suggestions = remember(last?.name, taken) { nextPoleNames(last?.name, taken) }
    var name by rememberSaveable { mutableStateOf(suggestions.firstOrNull().orEmpty()) }
    val duplicate = name.trim() in taken

    FormSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Нова опора", style = sheetHeading, color = c.ink, modifier = Modifier.weight(1f))
            Text(
                lineName.shortLineName() + (last?.let { " · після ${it.name.shortPoleName()}" } ?: ""),
                style = PoeTheme.type.caption, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp),
            )
        }
        Field("Назва / номер") {
            FormInput(
                value = name,
                onValueChange = { name = it },
                style = PoeTheme.type.hero.copy(fontSize = 34.sp),
                height = 72.dp,
                trailing = when {
                    duplicate -> "вже є на лінії"
                    name == suggestions.firstOrNull() -> "наступний номер"
                    else -> null
                },
                trailingColor = if (duplicate) c.danger else c.inkSecondary,
                autoFocus = false,
            )
        }
        if (suggestions.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.drop(1).take(3).forEach { s ->
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(PoeShapes.pill)
                            .background(c.surfaceMuted)
                            .clickable(role = Role.Button, onClickLabel = "Назвати $s") { name = s }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(s.poleNumber(), style = PoeTheme.type.label, color = c.ink, maxLines = 1) }
                }
            }
        }
        PoePrimaryButton(
            text = if (name.isBlank()) "Вкажіть назву" else "Створити опору ${name.trim().poleNumber()}",
            onClick = { onCreate(name) },
            icon = PoeIcons.Plus,
            enabled = name.isNotBlank() && !duplicate,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(c.outlineVariant))
            Text("або додати вже зняту точку", style = PoeTheme.type.captionStrong, color = c.muted)
            Box(Modifier.weight(1f).height(1.dp).background(c.outlineVariant))
        }
        PickSource.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { source ->
                    Column(
                        Modifier
                            .weight(1f)
                            .height(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surfaceMuted)
                            .clickable(role = Role.Button) { onPick(source) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(source.tileLabel, style = PoeTheme.type.small, color = c.muted)
                        Text(source.tileValue, style = PoeTheme.type.cardTitle, color = c.ink)
                    }
                }
            }
        }
    }
}

/** B9 · existing GIS pole or TP: nearest first, or by line name. Drawn over the editor. */
@Composable
internal fun ExistingPicker(
    source: PickSource,
    lineName: String,
    lastPole: String?,
    taken: Set<String>,
    here: Location?,
    actions: AddActions,
    // the pole the new one goes after; null = the end of the line
    afterId: Int?,
    onAdded: () -> Unit,
    onClose: () -> Unit,
) {
    val c = PoeTheme.colors
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var line by remember { mutableStateOf<ObjectDisplayable?>(null) }
    var selected by remember { mutableStateOf<PickItem?>(null) }
    BackHandler { if (tab == 1 && line != null) line = null else onClose() }

    val origin = remember(here != null) { here }
    // "near me" is measured from where the crew stood when the picker opened (or at the first fix)
    val near by produceState<List<PickItem>?>(null, source, origin) {
        value = origin?.let { actions.nearby(source, it.latitude, it.longitude) }
    }
    LaunchedEffect(origin == null) { if (origin == null && tab == 0) tab = 1 }

    // swallows touches so the map underneath doesn't pan
    Column(Modifier.fillMaxSize().background(c.ground).pointerInput(Unit) { detectTapGestures { } }) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PoeOverlayIconButton(PoeIcons.Back, "Назад", { if (tab == 1 && line != null) line = null else onClose() })
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(source.title, style = PoeTheme.type.title.copy(fontSize = 22.sp), color = c.ink)
                    Text(
                        "${source.subtitle} · у ${lineName.shortLineName()}" + (lastPole?.let { " після ${it.shortPoleName()}" } ?: ""),
                        style = PoeTheme.type.caption, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PoeSegmentedTabs(
                listOf("Поруч зі мною", if (source.isTp) "За назвою" else "За назвою лінії"),
                tab, { tab = it; selected = null },
            )
        }

        Box(Modifier.weight(1f)) {
            when {
                tab == 0 && origin == null -> Hint("Чекаємо на GPS, щоб показати найближчі. Поки що шукайте за назвою.")
                tab == 0 -> when (val list = near) {
                    null -> Hint("Шукаємо поруч…")
                    else -> if (list.isEmpty()) {
                        Hint("У радіусі ${(EditorViewModel.NEARBY_RADIUS_M / 1000).roundToInt()} км немає ${if (source.isTp) "ТП" else "опор"} з координатами. Шукайте за назвою.")
                    } else {
                        PickList(list, here, selected, taken, onSelect = { selected = it })
                    }
                }
                !source.isTp && line == null -> {
                    val lines by remember(source) { actions.lines(source) }.collectAsState(initial = null)
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        PoeSearchField(query, { query = it }, Modifier.padding(top = 14.dp, bottom = 10.dp), placeholder = "Назва лінії")
                        val filtered = lines?.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
                        when {
                            filtered == null -> Hint("Завантаження…")
                            filtered.isEmpty() -> Hint("Ліній не знайдено")
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filtered, key = { it.tplnr + it.id }) { l ->
                                    ListRow(onClick = { line = l; selected = null }) {
                                        Text(l.name, style = PoeTheme.type.rowTitle, color = c.ink, modifier = Modifier.weight(1f))
                                        Icon(PoeIcons.ChevronRight, null, tint = c.muted, modifier = Modifier.size(20.dp))
                                    }
                                }
                                item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
                            }
                        }
                    }
                }
                else -> {
                    val items by remember(source, line) { actions.candidates(source, line) }.collectAsState(initial = null)
                    Column {
                        if (source.isTp) {
                            PoeSearchField(query, { query = it }, Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp), placeholder = "Назва ТП")
                        } else {
                            Text(
                                "${line?.name} · опори з координатами",
                                style = PoeTheme.type.captionStrong, color = c.muted,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                            )
                        }
                        val filtered = items?.filter { query.isBlank() || !source.isTp || it.name.contains(query.trim(), ignoreCase = true) }
                        when {
                            filtered == null -> Hint("Завантаження…")
                            filtered.isEmpty() -> Hint(if (source.isTp) "ТП з координатами не знайдено" else "У цієї лінії немає опор з координатами")
                            else -> PickList(filtered, here, selected, taken, onSelect = { selected = it }, showLine = source.isTp)
                        }
                    }
                }
            }

            // footer over the list, like the design's fade
            val pick = selected
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to c.ground.copy(alpha = 0f), 0.35f to c.ground))
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
            ) {
                PoePrimaryButton(
                    text = when {
                        pick == null -> if (source.isTp) "Виберіть ТП" else "Виберіть опору"
                        pick.line.isBlank() -> "Додати ${pick.name}"
                        else -> "Додати опору ${pick.name.poleNumber()} з ${pick.line.shortLineName()}"
                    },
                    onClick = { pick?.let { actions.addExisting(source, it, afterId); onAdded() } },
                    icon = if (pick != null) PoeIcons.Plus else null,
                    enabled = pick != null,
                    disabledBackground = c.tabTrack,
                )
            }
        }
    }
}

@Composable
private fun PickList(
    items: List<PickItem>,
    here: Location?,
    selected: PickItem?,
    taken: Set<String>,
    onSelect: (PickItem) -> Unit,
    // inside one line's list the header already names the line
    showLine: Boolean = true,
) {
    val c = PoeTheme.colors
    LazyColumn(
        Modifier.padding(horizontal = 16.dp).padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.key }) { item ->
            val isSelected = item.key == selected?.key
            val already = item.name.trim() in taken
            val distance = here?.let { Geo.distanceM(it.latitude, it.longitude, item.lat.toDouble(), item.lng.toDouble()) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(PoeShapes.row)
                    .background(c.surface)
                    .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) c.action else c.cardBorder, PoeShapes.row)
                    .selectable(selected = isSelected, enabled = !already, role = Role.RadioButton) { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (distance != null) {
                    Text(
                        formatDistance(distance),
                        style = PoeTheme.type.statValue.copy(fontSize = 17.sp),
                        color = if (isSelected) c.actionText else if (already) c.muted else c.ink,
                        modifier = Modifier.width(64.dp),
                        maxLines = 1,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        when {
                            item.line.isBlank() -> item.name
                            showLine -> "${poleTitle(item.name)} · ${item.line.shortLineName()}"
                            else -> poleTitle(item.name)
                        },
                        style = PoeTheme.type.rowTitle, color = if (already) c.muted else c.ink,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        // GIS keeps up to 14 decimals; 5 (about a metre) is what the screen needs
                        if (already) "вже є на цій лінії" else "${item.lat.coord5()} · ${item.lng.coord5()}",
                        style = if (already) PoeTheme.type.captionRegular else PoeTheme.type.monoSmall, color = c.muted, maxLines = 1,
                    )
                }
                if (isSelected) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(c.action), contentAlignment = Alignment.Center) {
                        Icon(PoeIcons.Check, null, tint = c.onAction, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        // room for the footer button
        item { Spacer(Modifier.navigationBarsPadding().height(120.dp)) }
    }
}

/** B10 · Комутаційний апарат на опорі. */
@Composable
internal fun RecloserSheet(
    pole: OprDisplayable,
    lineName: String,
    onSave: (RecloserType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    var type by rememberSaveable { mutableStateOf(RecloserType.RECLOSER) }
    var name by rememberSaveable { mutableStateOf(pole.recloserName) }
    FormSheet(onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PoeOverline("${pole.title} · $lineName")
            Text("Комутаційний апарат", style = sheetHeading, color = c.ink)
        }
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecloserType.entries.forEach { t ->
                val on = t == type
                Column(
                    Modifier
                        .weight(1f)
                        .height(104.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (on) c.inverse else c.surfaceMuted)
                        .selectable(selected = on, role = Role.RadioButton) { type = t }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(PoeIcons.Recloser, null, tint = if (on) c.onInverse else c.ink, modifier = Modifier.size(26.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(t.toString(), style = PoeTheme.type.cardTitle, color = if (on) c.onInverse else c.ink)
                        Text(
                            if (t == RecloserType.RECLOSER) "автоматичний" else "ручний",
                            style = PoeTheme.type.small, color = if (on) c.onInverse.copy(alpha = 0.75f) else c.muted,
                        )
                    }
                }
            }
        }
        Field("Назва") {
            FormInput(name, { name = it }, style = PoeTheme.type.statValue, height = 60.dp)
        }
        Text(
            if (pole.hasCoordinates) "Координати візьмуться з опори ${pole.name.poleNumber()} · ${pole.lat}, ${pole.lng}"
            else "У опори ще немає координат — спершу зніміть її, щоб апарат отримав позицію.",
            style = PoeTheme.type.captionRegular, color = if (pole.hasCoordinates) c.muted else c.actionText,
        )
        PoePrimaryButton(
            text = when {
                !pole.hasCoordinates -> "Спершу зніміть опору"
                name.isBlank() -> "Вкажіть назву"
                else -> "Зберегти ${type.toString().lowercase()}"
            },
            onClick = { onSave(type, name) },
            enabled = pole.hasCoordinates && name.isNotBlank(),
        )
    }
}

/**
 * B11 · Нова відпайка. The name is generated: the server takes it as the tap's line name. When GIS
 * already draws taps from this pole ([gisTaps]), one can be picked to keep its exact GIS name, so the
 * upload edits that tap instead of adding a second one.
 */
@Composable
internal fun TapSheet(
    pole: OprDisplayable,
    lineName: String,
    gisTaps: List<GisTap>,
    onCreate: (firstPole: String, open: Boolean, gisName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    // no default: the crew reads the first pole's number off the pole
    var first by rememberSaveable { mutableStateOf("") }
    var open by rememberSaveable { mutableStateOf(true) }
    var gisName by rememberSaveable { mutableStateOf<String?>(null) }
    // when GIS has taps here, "a new one" must be picked on purpose: skipping the list makes duplicates
    var other by rememberSaveable { mutableStateOf(false) }
    val mustChoose = gisTaps.isNotEmpty() && gisName == null && !other
    FormSheet(onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PoeOverline("Від ${pole.name.shortPoleName()} · $lineName")
            Text("Нова відпайка", style = sheetHeading, color = c.ink)
        }
        if (gisTaps.isNotEmpty()) {
            Field("Уже є в ГІС від цієї опори") {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gisTaps.forEach { tap ->
                        TapChoice(tap.name, "взяти назву з ГІС", selected = gisName == tap.name) {
                            gisName = tap.name
                            other = false
                            if (first.isBlank()) first = tap.firstPole.orEmpty()
                        }
                    }
                    TapChoice("Інша, якої немає в ГІС", "назву буде створено", selected = other) {
                        gisName = null
                        other = true
                    }
                }
            }
        }
        Field("Назва відпайки") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, c.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    gisName ?: EditorViewModel.tapName(pole.name, first.trim().ifEmpty { "…" }),
                    style = PoeTheme.type.rowTitle.copy(fontSize = 18.sp), color = c.ink,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Field("Перша опора", Modifier.weight(1f)) {
                FormInput(
                    first, { first = it }, style = PoeTheme.type.statValue, height = 56.dp, filled = true,
                    autoFocus = true, placeholder = "номер",
                )
            }
            Field("Тип", Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(PoeShapes.tile).background(c.surfaceMuted).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { Text("Повітряна · ПЛ", style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 1) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = open, role = Role.Checkbox, onValueChange = { open = it }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (open) c.action else c.surface)
                    .border(2.dp, if (open) c.action else c.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { if (open) Icon(PoeIcons.Check, null, tint = c.onAction, modifier = Modifier.size(16.dp)) }
            Text("Одразу перейти до зйомки відпайки", style = PoeTheme.type.tabSelected, color = c.ink)
        }
        PoePrimaryButton(
            text = when {
                mustChoose -> "Виберіть відпайку вище"
                first.isBlank() -> "Вкажіть першу опору"
                else -> "Створити відпайку"
            },
            onClick = { onCreate(first, open, gisName) },
            icon = PoeIcons.Tap,
            enabled = first.isNotBlank() && !mustChoose,
        )
    }
}

@Composable
private fun TapChoice(title: String, note: String, selected: Boolean, onSelect: () -> Unit) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(PoeShapes.row)
            // the sheet is the same colour as a card: unselected rows need their own fill to read as options
            .background(if (selected) c.surface else c.surfaceMuted)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) c.action else c.outlineVariant, PoeShapes.row)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(note, style = PoeTheme.type.captionRegular, color = if (selected) c.actionText else c.muted)
        }
    }
}

private val sheetHeading: TextStyle
    @Composable get() = PoeTheme.type.sheetTitle.copy(fontSize = 32.sp, lineHeight = 34.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = PoeTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        shape = PoeShapes.sheet,
        dragHandle = { SheetHandle(c) },
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = PoeTheme.type.caption, color = PoeTheme.colors.muted)
        content()
    }
}

@Composable
private fun FormInput(
    value: String,
    onValueChange: (String) -> Unit,
    style: TextStyle,
    height: Dp,
    filled: Boolean = false,
    trailing: String? = null,
    trailingColor: androidx.compose.ui.graphics.Color = PoeTheme.colors.inkSecondary,
    autoFocus: Boolean = false,
    placeholder: String? = null,
) {
    val c = PoeTheme.colors
    val shape = RoundedCornerShape(if (height >= 72.dp) 20.dp else 18.dp)
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(if (filled) Modifier.background(c.surfaceMuted) else Modifier.border(2.dp, c.ink, shape))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, style = PoeTheme.type.input, color = c.muted, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = style.copy(color = c.ink),
                cursorBrush = SolidColor(c.action),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (trailing != null) Text(trailing, style = PoeTheme.type.caption, color = trailingColor, maxLines = 1)
    }
}

@Composable
private fun ListRow(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val c = PoeTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(PoeShapes.row)
            .background(c.surface)
            .border(1.dp, c.cardBorder, PoeShapes.row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text, style = PoeTheme.type.body, color = PoeTheme.colors.muted,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
    )
}

private fun formatDistance(m: Double): String = if (m < 1000) "${m.roundToInt()} м" else String.format(java.util.Locale.US, "%.1f км", m / 1000)

private val POLE_WORD = Regex("^Опора\\s+", RegexOption.IGNORE_CASE)

/** "Опора 13" or "13" → "оп. 13" for compact labels. */
private fun String.shortPoleName(): String = "оп. " + poleNumber()

/** "Опора 13" → "13", for "Створити опору 13". */
private fun String.poleNumber(): String = replace(POLE_WORD, "")

private val LINE_KIND = Regex("^(ПЛ|КЛ|КВ)-[\\d,.]+\\s*кВ\\s*", RegexOption.IGNORE_CASE)

/** "ПЛ-0,4 кВ Л-17 · Кохнівка" → "Л-17 · Кохнівка": the picker already says which voltage. */
private fun String.shortLineName(): String = replace(LINE_KIND, "").ifBlank { this }
