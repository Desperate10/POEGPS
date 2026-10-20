package com.poe.poegps.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Stroke icons from the canvas (24x24 viewBox). They are drawn in black and tinted by Icon(),
 * so pass the colour with `tint`.
 */
object PoeIcons {
    val Back = icon("back", 2.4f, "M15 5l-7 7 7 7")
    val Close = icon("close", 2.6f, "M6 6l12 12M18 6 6 18")
    val Plus = icon("plus", 2.4f, "M12 5v14M5 12h14")
    val Search = icon("search", 2.2f, circle(11f, 11f, 7f) + "M20 20l-4-4")
    val Download = icon("download", 2.2f, "M12 4v11M7 10l5 5 5-5M5 20h14")
    val Upload = icon("upload", 2.4f, "M12 20V9M7 14l5-5 5 5M5 4h14")
    val Sync = icon("sync", 2.4f, "M8 4v15M4 15l4 4 4-4M16 20V5M12 9l4-4 4 4")
    val Check = icon("check", 2.8f, "M5 12l5 5 9-10")
    val ChevronRight = icon("chevron_right", 2.4f, "M9 6l6 6-6 6")
    val ChevronDown = icon("chevron_down", 2.6f, "M8 10l4 4 4-4")
    val Person = icon("person", 2.2f, circle(12f, 8f, 4f) + "M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6")
    val Capture = icon("capture", 2.4f, circle(12f, 10f, 3f) + "M12 21s-7-6.2-7-11a7 7 0 0 1 14 0c0 4.8-7 11-7 11z")
    val Crosshair = icon("crosshair", 2.4f, "M12 2v4M12 18v4M2 12h4M18 12h4" + circle(12f, 12f, 5f))
    val Tap = icon("tap", 2.2f, "M6 3v8a5 5 0 0 0 5 5h8M15 12l4 4-4 4")
    val Recloser = icon("recloser", 2.2f, "M3 12h5M16 12h5M8 12l7-5")
    val Retake = icon("retake", 2.2f, "M3 12a9 9 0 0 1 15.5-6.2L21 8M21 3v5h-5M21 12a9 9 0 0 1-15.5 6.2L3 16M3 21v-5h5")
    val Move = icon("move", 2.2f, "M12 2v20M2 12h20M12 2l-3 3M12 2l3 3M12 22l-3-3M12 22l3-3M2 12l3-3M2 12l3 3M22 12l-3-3M22 12l-3 3")
    val Trash = icon("trash", 2.2f, "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Clock = icon("clock", 2.4f, circle(12f, 12f, 9f) + "M12 7v5l3 2")
    val Alert = icon("alert", 2.6f, circle(12f, 12f, 9f) + "M12 7v6M12 16.5v0.5")
    // B6 arrow pad
    val NudgeUp = icon("nudge_up", 2.8f, "M6 15l6-6 6 6")
    val NudgeDown = icon("nudge_down", 2.8f, "M6 9l6 6 6-6")
    val NudgeLeft = icon("nudge_left", 2.8f, "M15 6l-6 6 6 6")
    val NudgeRight = icon("nudge_right", 2.8f, "M9 6l6 6-6 6")
    val Edit = icon("edit", 2.4f, "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    val Undo = icon("undo", 2.4f, "M9 14L4 9l5-5M4 9h10.5a5.5 5.5 0 0 1 0 11H11")
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun icon(name: String, strokeWidth: Float, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
