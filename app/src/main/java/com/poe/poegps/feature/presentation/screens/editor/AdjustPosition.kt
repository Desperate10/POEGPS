package com.poe.poegps.feature.presentation.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeOutlinedButton
import com.poe.poegps.core.designsystem.PoeOverlayIconButton
import com.poe.poegps.core.designsystem.PoeOverlayTitle
import com.poe.poegps.core.designsystem.PoePrimaryButton
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeSheet
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.location.Geo
import com.poe.poegps.core.location.GpsFix
import com.poe.poegps.core.location.TrustState
import com.poe.poegps.feature.presentation.model.OprDisplayable
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt

/** Arrow steps in metres; 1 m is about the precision of the five-decimal coordinates the upload sends. */
val ADJUST_STEPS = listOf(1f, 5f)

/** (lat, lng) moved [north] and [east] metres. */
fun offsetBy(point: Pair<Double, Double>, north: Double, east: Double): Pair<Double, Double> {
    val (lat, lng) = point
    return (lat + north / 111_320.0) to (lng + east / (111_320.0 * cos(lat * PI / 180)))
}

/**
 * B6 · Уточнити позицію: everything drawn over the map while a pole is placed by hand. The pin sits
 * at the centre of the visible map; the crew drags the map under it or nudges with the arrows.
 */
@Composable
fun BoxScope.AdjustOverlay(
    pole: OprDisplayable,
    center: Pair<Double, Double>?,
    gps: GpsFix,
    trust: TrustState,
    previous: OprDisplayable?,
    step: Float,
    topInset: Dp,
    bottomInset: Dp,
    onSheetHeight: (Int) -> Unit,
    onStep: () -> Unit,
    onNudge: (north: Double, east: Double) -> Unit,
    onToGps: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val c = PoeTheme.colors
    // GPS under jamming/spoofing is no reference for where the pole is
    val gpsUsable = gps.current?.takeIf { !trust.level.locksCapture }

    Column(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PoeOverlayIconButton(PoeIcons.Close, "Скасувати і повернутись", onClose)
            PoeOverlayTitle("Уточнити позицію", pole.name, Modifier.weight(1f))
        }
        Row(
            Modifier
                .height(34.dp)
                .clip(PoeShapes.pill)
                .background(c.inverse.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(PoeIcons.Move, null, tint = c.onInverse, modifier = Modifier.size(16.dp))
            Text("Тягніть мапу або тисніть стрілки", style = PoeTheme.type.caption, color = c.onInverse)
        }
    }

    // the pin's tip marks the centre of the visible map
    Box(
        Modifier.fillMaxSize().padding(top = topInset, bottom = bottomInset),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .border(1.5.dp, c.surface, CircleShape)
        )
        Box(
            Modifier
                .offset(y = (-31).dp)
                .size(44.dp)
                .rotate(-45f)
                .shadow(8.dp, RoundedCornerShape(50, 50, 50, 10))
                .clip(RoundedCornerShape(50, 50, 50, 10))
                .background(c.action)
                .border(3.dp, c.surface, RoundedCornerShape(50, 50, 50, 10))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                Regex("\\d+\\S*").find(pole.name)?.value ?: "",
                style = PoeTheme.type.captionStrong,
                color = c.onAction,
                modifier = Modifier.rotate(45f),
            )
        }
        Box(Modifier.size(6.dp).clip(CircleShape).background(c.ink))
    }

    ArrowPad(
        step = step,
        onStep = onStep,
        onNudge = onNudge,
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = bottomInset + 16.dp),
    )

    PoeSheet(Modifier.align(Alignment.BottomCenter).onSizeChanged { onSheetHeight(it.height) }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(pole.name, style = PoeTheme.type.sheetTitle, color = c.ink)
            Text(
                center?.let { (lat, lng) -> String.format(Locale.US, "%.5f, %.5f", lat, lng) } ?: "—",
                style = PoeTheme.type.monoStrong, color = c.ink,
            )
        }
        val offset = if (gpsUsable != null && center != null) {
            Geo.distanceM(gpsUsable.latitude, gpsUsable.longitude, center.first, center.second)
        } else null
        when {
            offset != null && offset < 1.0 -> InfoChip(
                PoeIcons.Crosshair, "Позиція GPS" + (gps.accuracy?.let { String.format(Locale.US, " · ±%.1f м", it) } ?: ""),
                bg = c.gpsContainer, fg = c.onGpsContainer,
            )
            offset != null -> InfoChip(
                PoeIcons.Edit, "Вручну: на ${String.format(Locale.US, "%.1f", offset)} м від GPS (${direction(gpsUsable!!.latitude, gpsUsable.longitude, center!!)})",
                bg = c.actionContainer, fg = c.onActionContainer,
            )
            else -> InfoChip(
                PoeIcons.Edit,
                if (trust.level.locksCapture) "Вручну — GPS зараз не можна довіряти" else "Вручну — позиції GPS немає",
                bg = c.actionContainer, fg = c.onActionContainer,
            )
        }
        if (previous != null && center != null) {
            val span = Geo.distanceM(previous.lat.toDouble(), previous.lng.toDouble(), center.first, center.second)
            Text("Прогон від «${previous.name}»: ${span.roundToInt()} м", style = PoeTheme.type.caption, color = c.muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (gpsUsable != null) {
                PoeOutlinedButton("До GPS", onToGps, Modifier.height(64.dp), icon = PoeIcons.Crosshair)
            }
            PoePrimaryButton(
                "Зберегти позицію", onSave,
                Modifier.weight(1f).height(64.dp),
                icon = PoeIcons.Check,
                enabled = center != null,
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, bg: Color, fg: Color) {
    Row(
        Modifier.fillMaxWidth().clip(PoeShapes.tile).background(bg).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = PoeTheme.type.labelRegular, color = fg)
    }
}

@Composable
private fun ArrowPad(step: Float, onStep: () -> Unit, onNudge: (Double, Double) -> Unit, modifier: Modifier = Modifier) {
    val c = PoeTheme.colors
    val s = step.toDouble()
    Column(
        modifier
            .size(150.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(c.overlay)
            .border(1.dp, c.dockBorder, RoundedCornerShape(28.dp))
            .padding(3.dp),
    ) {
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f))
            Arrow(PoeIcons.NudgeUp, "Зсунути на північ на ${step.toInt()} м") { onNudge(s, 0.0) }
            Spacer(Modifier.weight(1f))
        }
        Row(Modifier.weight(1f)) {
            Arrow(PoeIcons.NudgeLeft, "Зсунути на захід на ${step.toInt()} м") { onNudge(0.0, -s) }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(PoeShapes.chip)
                    .background(c.surfaceMuted)
                    .clickable(role = Role.Button, onClickLabel = "Змінити крок", onClick = onStep),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("${step.toInt()} м", style = PoeTheme.type.statSuffix, color = c.actionText)
                Text("крок", style = PoeTheme.type.small, color = c.muted)
            }
            Arrow(PoeIcons.NudgeRight, "Зсунути на схід на ${step.toInt()} м") { onNudge(0.0, s) }
        }
        Row(Modifier.weight(1f)) {
            Spacer(Modifier.weight(1f))
            Arrow(PoeIcons.NudgeDown, "Зсунути на південь на ${step.toInt()} м") { onNudge(-s, 0.0) }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.Arrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(PoeShapes.chip)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = PoeTheme.colors.ink, modifier = Modifier.size(22.dp))
    }
}

/** "пн-сх" style direction from the GPS position to the pin. */
private fun direction(fromLat: Double, fromLng: Double, to: Pair<Double, Double>): String {
    val north = (to.first - fromLat) * 111_320.0
    val east = (to.second - fromLng) * 111_320.0 * cos(fromLat * PI / 180)
    val angle = atan2(east, north) * 180 / PI
    val names = listOf("пн", "пн-сх", "сх", "пд-сх", "пд", "пд-зх", "зх", "пн-зх")
    return names[(((angle + 360 + 22.5) % 360) / 45).toInt() % 8].takeIf { abs(north) + abs(east) > 0.1 } ?: "—"
}
