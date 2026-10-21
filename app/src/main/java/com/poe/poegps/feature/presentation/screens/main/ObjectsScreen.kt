package com.poe.poegps.feature.presentation.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.BadgeTone
import com.poe.poegps.core.designsystem.PoeCard
import com.poe.poegps.core.designsystem.PoeChip
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeSearchField
import com.poe.poegps.core.designsystem.PoeSegmentedTabs
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.domain.model.LineClearSummary
import com.poe.poegps.feature.domain.UploadScope
import com.poe.poegps.feature.domain.UploadSummary
import com.poe.poegps.feature.domain.isTapName
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.presentation.capture.downloadedLabel
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import kotlinx.coroutines.delay

/** A branch from the filials string array: "2300  Полтавська філія МЕМ". */
data class Filial(val code: String, val name: String) {
    /** What fits on the profile chip: the name without the word "філія". */
    val shortName: String get() = name.replace("філія", "").replace(Regex("\\s+"), " ").trim()

    companion object {
        fun parse(item: String) = Filial(item.take(2), item.drop(4).trim())
    }
}

/** B2 · Об'єкти: object type tabs, voltage and abonent filters, live search, line cards and the sync button. */
@Composable
fun ObjectsScreen(
    state: MainUiState,
    filials: List<Filial>,
    uploading: Boolean,
    onTypeChange: (ObjectType) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpen: (ObjectDisplayable) -> Unit,
    onUpload: (UploadScope) -> Unit,
    uploadSummary: suspend (ObjectDisplayable) -> UploadSummary,
    onDeleteTap: (ObjectDisplayable) -> Unit,
    onClearLine: (ObjectDisplayable) -> Unit,
    clearSummary: suspend (ObjectDisplayable) -> LineClearSummary,
    onDownload: () -> Unit,
    onUploadAll: () -> Unit,
    onFilialSelected: (String) -> Unit,
    /** When the branch data was last downloaded completely; null = never. */
    downloadedAt: Long? = null,
    // false while B1 is up: the login form asks for the branch itself
    askFilial: Boolean = true,
    /** Under the search field: the app update offer. */
    banner: @Composable () -> Unit = {},
) {
    val c = PoeTheme.colors
    val type = state.objectType
    var pickFilial by rememberSaveable { mutableStateOf(false) }
    var syncOpen by rememberSaveable { mutableStateOf(false) }
    // the field owns its text: round-tripping it through the async ui state would reset the cursor
    var query by rememberSaveable { mutableStateOf(state.query) }
    val filial = filials.firstOrNull { it.code == state.filial }
    val listState = rememberLazyListState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(c.ground)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // the title never wraps; on narrow phones the chip gives up width and ellipsizes
                    Text("Об'єкти", style = PoeTheme.type.screenTitle, color = c.ink, maxLines = 1, softWrap = false)
                    Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterEnd) {
                        ProfileChip(filial, onClick = { pickFilial = true })
                    }
                }
                PoeSegmentedTabs(
                    options = listOf("Лінії ПЛ", "ТП/РП", "КЛ/КВ"),
                    selectedIndex = type.tab,
                    onSelect = { onTypeChange(objectTypeOf(it, type.isHighVoltage, type.isAbon)) },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type.tab != TAB_TP) {
                        PoeChip("6-10 кВ", type.isHighVoltage, { onTypeChange(objectTypeOf(type.tab, true, type.isAbon)) })
                        PoeChip("0,4 кВ", !type.isHighVoltage, { onTypeChange(objectTypeOf(type.tab, false, type.isAbon)) })
                    }
                    Spacer(Modifier.weight(1f))
                    PoeChip(
                        "Абон.", type.isAbon,
                        { onTypeChange(objectTypeOf(type.tab, type.isHighVoltage, !type.isAbon)) },
                        icon = PoeIcons.Person, dashed = true,
                    )
                }
                PoeSearchField(
                    value = query,
                    onValueChange = { query = it; onQueryChange(it) },
                    trailing = if (state.query.isNotBlank() && state.total > 0) "${state.items.size} з ${state.total}" else null,
                )
                banner()
            }

            when {
                state.total == 0 && state.query.isEmpty() -> EmptyBranch(
                    filial = filial,
                    downloading = state.download.isRunning,
                    onDownload = onDownload,
                    modifier = Modifier.weight(1f),
                )
                state.items.isEmpty() -> Text(
                    "Нічого не знайдено за «${state.query.trim()}»",
                    style = PoeTheme.type.body, color = c.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp + navBottom),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { "${it.obj.tplnr}|${it.obj.name}|${it.obj.id}" }) { item ->
                        ObjectCard(
                            item = item,
                            isTp = type.tab == TAB_TP,
                            onOpen = { onOpen(item.obj) },
                            onUpload = onUpload,
                            uploadSummary = { uploadSummary(item.obj) },
                            onDeleteTap = { onDeleteTap(item.obj) },
                            onClearLine = { onClearLine(item.obj) },
                            clearSummary = { clearSummary(item.obj) },
                        )
                    }
                }
            }
        }

        // the actions themselves are one tap further, in the sheet: a thumb scrolling the list kept hitting them
        if (state.total > 0 || state.unsent > 0) {
            SyncButton(
                busy = state.download.isRunning || uploading,
                unsent = state.unsent,
                onClick = { syncOpen = true },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 16.dp),
            )
        }
        if (syncOpen) {
            SyncSheet(
                download = state.download,
                downloadedAt = downloadedAt,
                unsent = state.unsent,
                uploading = uploading,
                onDownload = { syncOpen = false; onDownload() },
                onUpload = { syncOpen = false; onUploadAll() },
                onClose = { syncOpen = false },
            )
        }
    }

    // the branch has to be chosen before anything can be downloaded
    if (pickFilial || (askFilial && state.filial == "00")) {
        FilialDialog(
            filials = filials,
            selected = state.filial,
            dismissible = state.filial != "00",
            onSelect = { onFilialSelected(it); pickFilial = false },
            onDismiss = { pickFilial = false },
        )
    }
}

@Composable
private fun ProfileChip(filial: Filial?, onClick: () -> Unit) {
    val c = PoeTheme.colors
    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(max = 200.dp)
            .clip(PoeShapes.pill)
            .background(c.surface)
            .border(1.dp, c.cardBorder, PoeShapes.pill)
            .clickable(role = Role.Button, onClickLabel = "Змінити філію", onClick = onClick)
            .semantics { contentDescription = "Філія: ${filial?.name ?: "не вибрано"}" }
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (c.isDark) c.action else c.inverse),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                filial?.code ?: "—",
                style = PoeTheme.type.monoStrong,
                color = if (c.isDark) c.onAction else c.onInverse,
            )
        }
        Text(
            filial?.shortName ?: "Філія",
            style = PoeTheme.type.labelRegular, color = c.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ObjectCard(
    item: ObjectItem,
    isTp: Boolean,
    onOpen: () -> Unit,
    onUpload: (UploadScope) -> Unit,
    uploadSummary: suspend () -> UploadSummary,
    onDeleteTap: () -> Unit,
    onClearLine: () -> Unit,
    clearSummary: suspend () -> LineClearSummary,
) {
    val c = PoeTheme.colors
    val obj = item.obj
    val p = item.progress?.takeIf { it.total > 0 }
    val gis = item.gisPoles
    var menu by remember { mutableStateOf(false) }
    var confirmUpload by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Box {
        PoeCard(onClick = onOpen, onLongClick = { menu = true }) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(obj.name, style = PoeTheme.type.cardTitle, color = c.ink)
                    val meta = listOfNotNull(obj.tplnr, obj.category.takeIf { it.isNotBlank() && !isTp })
                        .joinToString(" · ") + if (p == null && gis == 0 && !isTp) " · ще не розпочато" else ""
                    Text(meta, style = PoeTheme.type.monoSmall, color = c.muted)
                }
                when {
                    p != null && p.unsent > 0 ->
                        PoeStatusBadge(if (isTp) "не надіслано" else "${p.unsent} не надіслано", BadgeTone.Pending)
                    p != null && p.isComplete ->
                        PoeStatusBadge(if (isTp) "Знято" else "Готово", BadgeTone.Done, icon = PoeIcons.Check)
                    isTp && p == null -> PoeStatusBadge(if (obj.hasGisPoint) "координати з ГІС" else "без координат", BadgeTone.Neutral)
                }
            }
            if (gis > 0 && !isTp) {
                // GIS already has this line's poles: the job is checking them, not drawing the line from scratch
                val checked = p?.captured ?: 0
                GisLineGraphic(p, gis)
                // one text that wraps as a whole: the GIS caption is too long to share a row
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.ink, fontWeight = PoeTheme.type.rowTitle.fontWeight)) { append("$gis") }
                        append(" ${plural(gis, "опора", "опори", "опор")} в ГІС (з відпайками)")
                        if (checked > 0) append(" · уточнено $checked")
                        if (p != null && p.captured > 0 && p.unsent == 0) append(" · усі надіслано")
                    },
                    style = PoeTheme.type.caption, color = c.inkSecondaryOrMuted(),
                )
            } else if (p != null && !isTp) {
                LineProgressGraphic(p)
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = c.ink, fontWeight = PoeTheme.type.rowTitle.fontWeight)) { append("${p.captured}") }
                            append(" / ${p.total} опор")
                        },
                        style = PoeTheme.type.caption, color = c.inkSecondaryOrMuted(),
                        modifier = Modifier.weight(1f),
                    )
                    if (p.captured > 0 && p.unsent == 0) {
                        Text("усі надіслано", style = PoeTheme.type.caption, color = c.inkSecondaryOrMuted())
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        when {
                            isTp -> "Вивантажити цей об'єкт"
                            isTapName(obj.name) -> "Вивантажити відпайку"
                            else -> "Вивантажити лінію"
                        },
                        style = PoeTheme.type.labelRegular,
                    )
                },
                leadingIcon = { Icon(PoeIcons.Upload, null, Modifier.size(20.dp)) },
                onClick = { menu = false; confirmUpload = true },
            )
            // a line with poles on the phone can be started over; a TP has only its one point
            if (!isTp && p != null) {
                DropdownMenuItem(
                    text = { Text("Очистити лінію на телефоні", style = PoeTheme.type.labelRegular, color = c.danger) },
                    leadingIcon = { Icon(PoeIcons.Trash, null, Modifier.size(20.dp), tint = c.danger) },
                    onClick = { menu = false; confirmClear = true },
                )
            }
            // trunk lines come from GIS and can't be removed; only taps created on the phone can
            if (!isTp && !obj.name.contains("ПЛ")) {
                DropdownMenuItem(
                    text = { Text("Видалити відпайку", style = PoeTheme.type.labelRegular, color = c.danger) },
                    leadingIcon = { Icon(PoeIcons.Trash, null, Modifier.size(20.dp), tint = c.danger) },
                    onClick = { menu = false; onDeleteTap() },
                )
            }
        }
    }
    if (confirmClear && p != null) {
        val summary by produceState<LineClearSummary?>(null) { value = clearSummary() }
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = c.surface,
            title = { Text("Очистити лінію?", style = PoeTheme.type.sheetTitle, color = c.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(obj.name, style = PoeTheme.type.rowTitle, color = c.ink)
                    val s = summary
                    Text(
                        when {
                            s == null -> "…"
                            s.taps > 0 -> "З телефона буде видалено всі опори цієї лінії та її " +
                                "${s.taps} ${plural(s.taps, "відпайки", "відпайок", "відпайок")} разом із самими відпайками"
                            else -> "З телефона буде видалено всі опори цієї лінії"
                        } + (s?.unsent?.takeIf { it > 0 }
                            ?.let { ", серед них $it ${plural(it, "не надіслана", "не надіслані", "не надісланих")} — їх не можна буде відновити" }
                            ?: "") +
                            ". У ГІС нічого не зміниться. Коли відкриєте лінію знову, опори з ГІС завантажаться заново.",
                        style = PoeTheme.type.body, color = c.muted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearLine() }, enabled = summary != null) {
                    Text("Очистити", style = PoeTheme.type.button, color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Скасувати", style = PoeTheme.type.button, color = c.ink)
                }
            },
        )
    }
    if (confirmUpload) {
        // a tap goes up alone; a main line asks whether its taps go too; a TP or cable object goes whole
        val tap = !isTp && isTapName(obj.name)
        val summary by produceState<UploadSummary?>(null) { if (!isTp) value = uploadSummary() }
        val s = summary
        val choose = !isTp && !tap && s != null && s.others > 0
        val onlyThis = UploadScope(obj.tplnr, obj.name)
        val whole = UploadScope(obj.tplnr)
        fun changes(n: Int) = "$n ${plural(n, "зміна", "зміни", "змін")}"
        AlertDialog(
            onDismissRequest = { confirmUpload = false },
            containerColor = c.surface,
            title = {
                Text(
                    when {
                        tap -> "Вивантажити відпайку?"
                        isTp -> "Вивантажити об'єкт?"
                        else -> "Вивантажити лінію?"
                    },
                    style = PoeTheme.type.sheetTitle, color = c.ink,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(obj.name, style = PoeTheme.type.rowTitle, color = c.ink)
                    Text(
                        when {
                            isTp -> p?.unsent?.takeIf { it > 0 }?.let { "Буде надіслано в ГІС: ${changes(it)}." }
                                ?: "На телефоні немає нових змін цього об'єкта."
                            s == null -> "…"
                            tap && s.line > 0 -> "Буде надіслано в ГІС лише цю відпайку: ${changes(s.line)}. " +
                                "Основна лінія та інші відпайки не надсилаються."
                            tap -> "На телефоні немає нових змін цієї відпайки."
                            choose -> "Ця лінія: ${changes(s.line)}.\n" +
                                "Відпайки: ${changes(s.others)} у ${s.otherLines} " +
                                "${plural(s.otherLines, "відпайці", "відпайках", "відпайках")}.\n" +
                                "Надіслати лише лінію чи разом з відпайками?"
                            s.line > 0 -> "Буде надіслано в ГІС: ${changes(s.line)}."
                            else -> "На телефоні немає нових змін цієї лінії."
                        },
                        style = PoeTheme.type.body, color = c.muted,
                    )
                }
            },
            confirmButton = {
                if (choose) {
                    // three buttons don't fit one row on a narrow phone: stacked, the cancel one last
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { confirmUpload = false; onUpload(whole) }) {
                            Text("З відпайками", style = PoeTheme.type.button, color = c.actionText)
                        }
                        TextButton(onClick = { confirmUpload = false; onUpload(onlyThis) }, enabled = s!!.line > 0) {
                            Text("Лише лінію", style = PoeTheme.type.button, color = if (s.line > 0) c.actionText else c.muted)
                        }
                        TextButton(onClick = { confirmUpload = false }) {
                            Text("Скасувати", style = PoeTheme.type.button, color = c.ink)
                        }
                    }
                } else {
                    TextButton(
                        onClick = { confirmUpload = false; onUpload(if (tap) onlyThis else whole) },
                        enabled = isTp || s != null,
                    ) {
                        Text("Вивантажити", style = PoeTheme.type.button, color = c.actionText)
                    }
                }
            },
            dismissButton = if (choose) null else {
                {
                    TextButton(onClick = { confirmUpload = false }) {
                        Text("Скасувати", style = PoeTheme.type.button, color = c.ink)
                    }
                }
            },
        )
    }
}

@Composable
private fun com.poe.poegps.core.designsystem.PoeColors.inkSecondaryOrMuted() =
    if (isDark) muted else inkSecondary

private val LineProgress.isComplete get() = captured == total && unsent == 0

/**
 * The little line drawing on a card: captured poles filled, the rest hollow.
 * Long lines and finished ones collapse into a bar.
 */
@Composable
private fun LineProgressGraphic(p: LineProgress) {
    val c = PoeTheme.colors
    val label = "${p.captured} з ${p.total} опор знято"
    if (p.isComplete) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(PoeShapes.pill).background(c.gps).semantics { contentDescription = label })
        return
    }
    if (p.total > MAX_DOTS) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(PoeShapes.pill).background(c.schemePending).semantics { contentDescription = label }) {
            Box(Modifier.fillMaxWidth(p.captured / p.total.toFloat()).height(8.dp).clip(PoeShapes.pill).background(c.action))
        }
        return
    }
    Canvas(Modifier.fillMaxWidth().height(34.dp).semantics { contentDescription = label }) {
        val n = p.total
        val step = if (n > 1) (size.width - 12.dp.toPx()) / (n - 1) else 0f
        val pts = List(n) { i ->
            Offset(6.dp.toPx() + i * step, ZIGZAG[i % ZIGZAG.size].dp.toPx())
        }
        fun path(from: Int, to: Int) = Path().apply {
            moveTo(pts[from].x, pts[from].y)
            for (i in from + 1..to) lineTo(pts[i].x, pts[i].y)
        }
        if (n > 1) drawPath(path(0, n - 1), c.schemePending, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        if (p.captured > 1) {
            drawPath(path(0, p.captured - 1), c.action, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        pts.forEachIndexed { i, o ->
            if (i < p.captured) {
                drawCircle(c.action, 3.5.dp.toPx(), o)
            } else {
                drawCircle(c.surface, 3.dp.toPx(), o)
                drawCircle(c.outline, 3.dp.toPx(), o, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/** A line GIS already has: a full light bar, with the share of poles checked on site drawn over it. */
@Composable
private fun GisLineGraphic(p: LineProgress?, gisPoles: Int) {
    val c = PoeTheme.colors
    if (p != null && p.isComplete) {
        LineProgressGraphic(p)
        return
    }
    val checked = p?.captured ?: 0
    val of = maxOf(gisPoles, p?.total ?: 0)
    val label = "$gisPoles ${plural(gisPoles, "опора", "опори", "опор")} в ГІС з відпайками, уточнено $checked"
    Box(Modifier.fillMaxWidth().height(8.dp).clip(PoeShapes.pill).background(c.action.copy(alpha = 0.35f)).semantics { contentDescription = label }) {
        if (checked > 0) {
            Box(Modifier.fillMaxWidth((checked / of.toFloat()).coerceAtMost(1f)).height(8.dp).clip(PoeShapes.pill).background(c.action))
        }
    }
}

private const val MAX_DOTS = 30
private const val ARM_DELAY_MS = 600L
private val ZIGZAG = listOf(22, 14, 18, 10, 16, 12, 20, 14, 18, 24, 20, 26, 22)

/** The way into upload and download: one round button with the count of unsent changes. */
@Composable
private fun SyncButton(busy: Boolean, unsent: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PoeTheme.colors
    val label = if (unsent > 0) "Синхронізація, не надіслано $unsent" else "Синхронізація"
    Box(modifier) {
        Box(
            Modifier
                .size(60.dp)
                .shadow(16.dp, CircleShape, spotColor = c.ink)
                .clip(CircleShape)
                .background(c.dock)
                .border(1.dp, c.dockBorder, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Відкрити синхронізацію", onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = c.onDock, strokeWidth = 2.5.dp)
            } else {
                Icon(PoeIcons.Sync, null, tint = c.onDock, modifier = Modifier.size(26.dp))
            }
        }
        if (unsent > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .heightIn(min = 24.dp)
                    .widthIn(min = 24.dp)
                    .clip(PoeShapes.pill)
                    .background(c.action)
                    .border(2.dp, c.ground, PoeShapes.pill)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$unsent", style = PoeTheme.type.captionStrong, color = c.onAction)
            }
        }
    }
}

/** Download and upload, each with what it will do, behind the sync button. */
@Composable
private fun SyncSheet(
    download: UploadState,
    downloadedAt: Long?,
    unsent: Int,
    uploading: Boolean,
    onDownload: () -> Unit,
    onUpload: () -> Unit,
    onClose: () -> Unit,
) {
    val c = PoeTheme.colors
    BackHandler(onBack = onClose)
    // the upload button opens right under the sync button: a double tap there must not upload
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ARM_DELAY_MS)
        armed = true
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(c.ink.copy(alpha = 0.4f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        // taps on the sheet itself must not reach the scrim
        PoeSheet(Modifier.align(Alignment.BottomCenter).clickable(remember { MutableInteractionSource() }, indication = null) {}) {
            Text("Синхронізація", style = PoeTheme.type.sheetTitle, color = c.ink)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Дані філії", style = PoeTheme.type.rowTitle, color = c.ink)
                Text(
                    downloadedAt?.let { "Оновлено ${downloadedLabel(it)}" } ?: "Ще не завантажено",
                    style = PoeTheme.type.captionRegular, color = c.muted,
                )
            }
            if (download.isRunning) {
                val pct = (download as? UploadState.Progress)?.percentage
                Row(
                    Modifier.fillMaxWidth().height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = c.ink, strokeWidth = 2.dp)
                    Text(if (pct != null) "Завантаження $pct%" else "Завантаження…", style = PoeTheme.type.button, color = c.ink)
                }
            } else {
                PoeOutlinedButton("Завантажити дані філії", { if (armed) onDownload() }, Modifier.fillMaxWidth(), icon = PoeIcons.Download)
            }

            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Зміни на телефоні", style = PoeTheme.type.rowTitle, color = c.ink)
                Text(
                    if (unsent > 0) "Не надіслано: $unsent" else "Усе надіслано",
                    style = PoeTheme.type.captionRegular, color = if (unsent > 0) c.actionText else c.muted,
                )
            }
            PoePrimaryButton(
                text = if (uploading) "Вивантаження…" else "Вивантажити в ГІС",
                onClick = { if (armed) onUpload() },
                icon = PoeIcons.Upload,
                enabled = unsent > 0 && !uploading,
            )
        }
    }
}

/** B14: nothing downloaded for this branch yet. */
@Composable
private fun EmptyBranch(
    filial: Filial?,
    downloading: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PoeTheme.colors
    Column(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text("Даних ще немає", style = PoeTheme.type.sheetTitle, color = c.ink)
        Text(
            "Завантажте лінії, ТП і опори ${filial?.name?.let { "філії «$it»" } ?: "своєї філії"} один раз — далі все працює без мережі.",
            style = PoeTheme.type.body, color = c.inkSecondaryOrMuted(),
        )
        Spacer(Modifier.height(4.dp))
        PoePrimaryButton(
            text = if (downloading) "Завантаження…" else "Завантажити дані філії",
            onClick = onDownload,
            icon = PoeIcons.Download,
            enabled = !downloading && filial != null,
        )
        Text(
            "Потрібен Wi-Fi або мобільний інтернет",
            style = PoeTheme.type.caption, color = c.muted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun FilialDialog(
    filials: List<Filial>,
    selected: String?,
    dismissible: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PoeTheme.colors
    var choice by remember { mutableStateOf(selected?.takeIf { it != "00" }) }
    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        containerColor = c.surface,
        title = { Text("Оберіть філію", style = PoeTheme.type.statValue, color = c.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(filials, key = { it.code }) { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(PoeShapes.chip)
                            .selectable(selected = choice == f.code, role = Role.RadioButton, onClick = { choice = f.code })
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = choice == f.code, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = c.action, unselectedColor = c.outline),
                        )
                        Text(f.code, style = PoeTheme.type.mono, color = c.muted)
                        Text(f.name, style = PoeTheme.type.labelRegular, color = c.ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { choice?.let(onSelect) }, enabled = choice != null) {
                Text("Вибрати", style = PoeTheme.type.button, color = if (choice != null) c.actionText else c.muted)
            }
        },
        dismissButton = if (dismissible) {
            { TextButton(onClick = onDismiss) { Text("Скасувати", style = PoeTheme.type.button, color = c.ink) } }
        } else null,
    )
}

private const val TAB_LINES = 0
private const val TAB_TP = 1
private const val TAB_KL = 2

private val ObjectType.tab
    get() = when (this) {
        ObjectType.TP, ObjectType.TPABON -> TAB_TP
        ObjectType.KLKV04, ObjectType.KLKV10, ObjectType.KLKVABON04, ObjectType.KLKVABON10 -> TAB_KL
        else -> TAB_LINES
    }

private val ObjectType.isHighVoltage
    get() = this == ObjectType.LINE10 || this == ObjectType.LINEABON10 ||
        this == ObjectType.KLKV10 || this == ObjectType.KLKVABON10

private val ObjectType.isAbon get() = name.contains("ABON")

private fun objectTypeOf(tab: Int, highVoltage: Boolean, abon: Boolean): ObjectType = when (tab) {
    TAB_TP -> if (abon) ObjectType.TPABON else ObjectType.TP
    TAB_KL -> when {
        highVoltage && abon -> ObjectType.KLKVABON10
        highVoltage -> ObjectType.KLKV10
        abon -> ObjectType.KLKVABON04
        else -> ObjectType.KLKV04
    }
    else -> when {
        highVoltage && abon -> ObjectType.LINEABON10
        highVoltage -> ObjectType.LINE10
        abon -> ObjectType.LINEABON04
        else -> ObjectType.LINE04
    }
}

private val UploadState.isRunning get() = this is UploadState.Loading || this is UploadState.Progress
