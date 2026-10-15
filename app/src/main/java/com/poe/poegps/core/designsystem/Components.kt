package com.poe.poegps.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The big 72dp orange/amber action: "Зняти координати". Disabled = waiting state of B13. */
@Composable
fun PoePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    // on the grey ground (not a white sheet) surfaceMuted vanishes; pass tabTrack there
    disabledBackground: Color = PoeTheme.colors.surfaceMuted,
) {
    val c = PoeTheme.colors
    val bg = if (enabled) c.action else disabledBackground
    val fg = if (enabled) c.onAction else c.muted
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(if (enabled) Modifier.shadow(14.dp, PoeShapes.primary, spotColor = c.action) else Modifier)
            .clip(PoeShapes.primary)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
        Text(text, style = if (enabled) PoeTheme.type.action else PoeTheme.type.actionWaiting, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 52dp outlined button: "Очистити координати", "Видалити" (danger). */
@Composable
fun PoeOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
) {
    val c = PoeTheme.colors
    val fg = if (danger) c.danger else c.ink
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(PoeShapes.tile)
            .border(1.5.dp, if (danger) c.dangerOutline else c.outlineVariant, PoeShapes.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = PoeTheme.type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Segmented tabs on a track: "Лінії ПЛ / ТП/РП / КЛ/КВ". */
@Composable
fun PoeSegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PoeTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PoeShapes.field)
            .background(c.tabTrack)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .then(if (selected) Modifier.shadow(1.dp, PoeShapes.chip) else Modifier)
                    .clip(PoeShapes.chip)
                    .background(if (selected) c.tabSelected else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = if (selected) PoeTheme.type.tabSelected else PoeTheme.type.tab,
                    color = if (selected) c.ink else if (c.isDark) c.muted else c.inkSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 40dp filter chip: "6-10 кВ", "0,4 кВ"; dashed outline for the "Абон." toggle. */
@Composable
fun PoeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    dashed: Boolean = false,
) {
    val c = PoeTheme.colors
    val fg = when {
        selected -> c.onInverse
        dashed -> c.inkSecondary
        else -> c.ink
    }
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(PoeShapes.pill)
            .then(
                when {
                    selected -> Modifier.background(c.inverse)
                    dashed -> Modifier.dashedBorder(c.outline, 1.5.dp, 20.dp)
                    else -> Modifier.border(1.5.dp, c.outline, PoeShapes.pill)
                }
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = if (icon != null) 14.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = if (selected) PoeTheme.type.label else PoeTheme.type.labelRegular, color = fg)
    }
}

enum class BadgeTone { Pending, Done, Neutral }

/** 28dp status pill: "6 не надіслано", "Готово". */
@Composable
fun PoeStatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = PoeTheme.colors
    val (bg, fg) = when (tone) {
        BadgeTone.Pending -> c.actionContainer to c.onActionContainer
        BadgeTone.Done -> c.gpsContainer to c.onGpsContainer
        BadgeTone.Neutral -> c.surfaceMuted to c.ink
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(PoeShapes.pill)
            .background(bg)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, style = PoeTheme.type.badge, color = fg, maxLines = 1)
    }
}

/** List card (r24, 16dp padding); draws a hairline border in dark. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PoeTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PoeShapes.card)
            .background(c.surface)
            .border(1.dp, c.cardBorder, PoeShapes.card)
            .then(if (onClick != null) Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** 54dp search field with an optional counter on the right ("3 з 186"). */
@Composable
fun PoeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Пошук",
    trailing: String? = null,
) {
    val c = PoeTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(PoeShapes.field)
            .background(c.surface)
            .border(1.dp, c.cardBorder, PoeShapes.field)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PoeIcons.Search, contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = PoeTheme.type.input, color = c.muted)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = PoeTheme.type.input.copy(color = c.ink),
                cursorBrush = SolidColor(c.action),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (trailing != null) Text(trailing, style = PoeTheme.type.captionRegular, color = c.muted)
    }
}

/** Bottom sheet surface with the drag handle; content is laid out in a column with 14dp gaps. */
@Composable
fun PoeSheet(
    modifier: Modifier = Modifier,
    gap: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = PoeTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, PoeShapes.sheet)
            .clip(PoeShapes.sheet)
            .background(c.surface)
            .then(if (c.isDark) Modifier.border(1.dp, c.cardBorder, PoeShapes.sheet) else Modifier)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.outlineVariant)
        )
        content()
    }
}

/** 68dp row in the pole actions sheet: icon tile, title, subtitle, chevron. */
@Composable
fun PoeActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackground: Color = PoeTheme.colors.iconTile,
    iconTint: Color = PoeTheme.colors.onIconTile,
    iconBorder: Color? = null,
) {
    val c = PoeTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(PoeShapes.row)
            .background(c.surfaceMuted)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(PoeShapes.chip)
                .background(iconBackground)
                .then(if (iconBorder != null) Modifier.border(2.dp, iconBorder, PoeShapes.chip) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PoeTheme.type.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = PoeTheme.type.captionRegular, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(PoeIcons.ChevronRight, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
    }
}

/** Small tile in the line sheet: "Точність 2.8 м". */
@Composable
fun PoeStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PoeTheme.colors.ink,
    suffix: String? = null,
) {
    val c = PoeTheme.colors
    Column(
        modifier = modifier
            .clip(PoeShapes.tile)
            .background(c.surfaceMuted)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = PoeTheme.type.small, color = c.muted, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = PoeTheme.type.statValue, color = valueColor, maxLines = 1)
            if (suffix != null) Text(suffix, style = PoeTheme.type.statSuffix, color = c.muted, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** 48dp round button floating over the map; `filled` = the dark/amber "+" variant. */
@Composable
fun PoeOverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val c = PoeTheme.colors
    val bg = if (!filled) c.overlay else if (c.isDark) c.action else c.inverse
    val fg = if (!filled) c.ink else if (c.isDark) c.onAction else c.onInverse
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(if (c.isDark) 0.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(22.dp))
    }
}

/** Title pill next to the back button over the map: line name + progress line. */
@Composable
fun PoeOverlayTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val c = PoeTheme.colors
    Column(
        modifier = modifier
            .height(48.dp)
            .shadow(if (c.isDark) 0.dp else 6.dp, PoeShapes.pill)
            .clip(PoeShapes.pill)
            .background(c.overlay)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = PoeTheme.type.titleSmall, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = PoeTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 36dp GPS status pill over the map: "±2.8 м · 14 супутн.". */
@Composable
fun PoeGpsPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = PoeTheme.colors
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(PoeShapes.pill)
            .background(c.gpsStrong)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PoeIcons.Crosshair, contentDescription = null, tint = c.onGpsStrong, modifier = Modifier.size(16.dp))
        Text(text, style = PoeTheme.type.captionStrong, color = c.onGpsStrong)
    }
}

/** Uppercase orange label above a big title: "НАСТУПНА ОПОРА". */
@Composable
fun PoeOverline(text: String, modifier: Modifier = Modifier, color: Color = PoeTheme.colors.actionText) {
    Text(text.uppercase(), style = PoeTheme.type.overline, color = color, modifier = modifier)
}

fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
    )
}

