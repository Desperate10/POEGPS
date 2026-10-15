package com.poe.poegps.feature.presentation.screens.main

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeSearchField
import com.poe.poegps.core.designsystem.PoeSegmentedTabs
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeStatusBadge
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType

/** A branch from the filials string array: "2300  Полтавська філія МЕМ". */
data class Filial(val code: String, val name: String) {
    /** What fits on the profile chip: the name without the word "філія". */
    val shortName: String get() = name.replace("філія", "").replace(Regex("\\s+"), " ").trim()

    companion object {
        fun parse(item: String) = Filial(item.take(2), item.drop(4).trim())
    }
}

/** B2 · Об'єкти: object type tabs, voltage and abonent filters, live search, line cards and the sync dock. */
@Composable
fun ObjectsScreen(
    state: MainUiState,
    filials: List<Filial>,
    uploading: Boolean,
    onTypeChange: (ObjectType) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpen: (ObjectDisplayable) -> Unit,
    onUploadOne: (ObjectDisplayable) -> Unit,
    onDeleteTap: (ObjectDisplayable) -> Unit,
    onDownload: () -> Unit,
    onUploadAll: () -> Unit,
    onFilialSelected: (String) -> Unit,
) {
    val c = PoeTheme.colors
    val type = state.objectType
    var pickFilial by rememberSaveable { mutableStateOf(false) }
    // the field owns its text: round-tripping it through the async ui state would reset the cursor
    var query by rememberSaveable { mutableStateOf(state.query) }
    val filial = filials.firstOrNull { it.code == state.filial }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize().background(c.ground)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Об'єкти", style = PoeTheme.type.screenTitle, color = c.ink, modifier = Modifier.weight(1f))
                    ProfileChip(filial, onClick = { pickFilial = true })
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { "${it.obj.tplnr}|${it.obj.name}|${it.obj.id}" }) { item ->
                        ObjectCard(
                            item = item,
                            isTp = type.tab == TAB_TP,
                            onOpen = { onOpen(item.obj) },
                            onUpload = { onUploadOne(item.obj) },
                            onDeleteTap = { onDeleteTap(item.obj) },
                        )
                    }
                }
            }
        }

        if (state.total > 0 || state.unsent > 0) {
            SyncDock(
                download = state.download,
                unsent = state.unsent,
                uploading = uploading,
                onDownload = onDownload,
                onUpload = onUploadAll,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }

    // the branch has to be chosen before anything can be downloaded
    if (pickFilial || state.filial == "00") {
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
    onUpload: () -> Unit,
    onDeleteTap: () -> Unit,
) {
    val c = PoeTheme.colors
    val obj = item.obj
    val p = item.progress?.takeIf { it.total > 0 }
    var menu by remember { mutableStateOf(false) }

    Box {
        PoeCard(onClick = onOpen, onLongClick = { menu = true }) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(obj.name, style = PoeTheme.type.cardTitle, color = c.ink)
                    val meta = listOfNotNull(obj.tplnr, obj.category.takeIf { it.isNotBlank() && !isTp })
                        .joinToString(" · ") + if (p == null && !isTp) " · ще не розпочато" else ""
                    Text(meta, style = PoeTheme.type.monoSmall, color = c.muted)
                }
                when {
                    p != null && p.unsent > 0 ->
                        PoeStatusBadge(if (isTp) "не надіслано" else "${p.unsent} не надіслано", BadgeTone.Pending)
                    p != null && p.isComplete ->
                        PoeStatusBadge(if (isTp) "Знято" else "Готово", BadgeTone.Done, icon = PoeIcons.Check)
                    isTp && p == null -> PoeStatusBadge("без координат", BadgeTone.Neutral)
                }
            }
            if (p != null && !isTp) {
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
                text = { Text("Вивантажити цей об'єкт", style = PoeTheme.type.labelRegular) },
                leadingIcon = { Icon(PoeIcons.Upload, null, Modifier.size(20.dp)) },
                onClick = { menu = false; onUpload() },
            )
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

private const val MAX_DOTS = 30
private val ZIGZAG = listOf(22, 14, 18, 10, 16, 12, 20, 14, 18, 24, 20, 26, 22)

@Composable
private fun SyncDock(
    download: UploadState,
    unsent: Int,
    uploading: Boolean,
    onDownload: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PoeTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(20.dp, PoeShapes.card, spotColor = c.ink)
            .clip(PoeShapes.card)
            .background(c.dock)
            .border(1.dp, c.dockBorder, PoeShapes.card)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(56.dp)
                .clip(PoeShapes.field)
                .clickable(enabled = !download.isRunning, role = Role.Button, onClick = onDownload),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (download.isRunning) {
                CircularProgressIndicator(Modifier.size(18.dp), color = c.onDock, strokeWidth = 2.dp)
                val pct = (download as? UploadState.Progress)?.percentage
                Text(if (pct != null) "$pct%" else "Завантаження", style = PoeTheme.type.tab, color = c.onDock)
            } else {
                Icon(PoeIcons.Download, null, tint = c.onDock, modifier = Modifier.size(20.dp))
                Text("Завантажити", style = PoeTheme.type.tab, color = c.onDock)
            }
        }
        Row(
            Modifier
                .weight(1.25f)
                .height(56.dp)
                .clip(PoeShapes.field)
                .background(if (unsent > 0) c.action else c.onDock.copy(alpha = 0.12f))
                .clickable(enabled = !uploading, role = Role.Button, onClick = onUpload),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (unsent > 0) c.onAction else c.onDock
            if (uploading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
                Text("Вивантаження", style = PoeTheme.type.titleSmall, color = fg)
            } else {
                if (!c.isDark) Icon(PoeIcons.Upload, null, tint = fg, modifier = Modifier.size(20.dp))
                Text("Вивантажити", style = PoeTheme.type.titleSmall, color = fg)
                if (unsent > 0) {
                    Box(
                        Modifier
                            .heightIn(min = 24.dp)
                            .widthIn(min = 24.dp)
                            .clip(PoeShapes.pill)
                            .background(if (c.isDark) c.onAction else c.surface)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$unsent", style = PoeTheme.type.captionStrong, color = if (c.isDark) c.action else c.actionText)
                    }
                }
            }
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
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
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
private fun FilialDialog(
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
