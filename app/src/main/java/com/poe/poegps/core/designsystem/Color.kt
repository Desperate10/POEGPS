package com.poe.poegps.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours of the redesign (canvas direction B "Daylight Topo" and its dark variant).
 * Screens use these roles, never raw hex values.
 */
@Immutable
data class PoeColors(
    val isDark: Boolean,
    // grounds and surfaces
    val ground: Color,          // screen background
    val mapGround: Color,       // background behind the map/scheme
    val surface: Color,         // cards, bottom sheets
    val surfaceMuted: Color,    // stat tiles and action rows inside a sheet
    val cardBorder: Color,      // only drawn in dark, transparent in light
    val overlay: Color,         // round buttons and title pill floating over the map
    val tabTrack: Color,
    val tabSelected: Color,
    // text
    val ink: Color,
    val inkSecondary: Color,
    val muted: Color,
    // lines
    val outline: Color,         // unselected chips
    val outlineVariant: Color,  // outlined buttons, sheet handle
    // inverse: selected chips
    val inverse: Color,
    val onInverse: Color,
    // neutral icon tile in action rows
    val iconTile: Color,
    val onIconTile: Color,
    // dock: the floating download/upload bar
    val dock: Color,
    val onDock: Color,
    val dockBorder: Color,
    // action (orange in light, amber in dark)
    val action: Color,
    val onAction: Color,
    val actionText: Color,
    val actionContainer: Color,
    val onActionContainer: Color,
    // GPS (blue in light, cyan in dark)
    val gps: Color,
    val gpsText: Color,
    val gpsContainer: Color,
    val onGpsContainer: Color,
    val gpsStrong: Color,       // GPS status pill over the map
    val onGpsStrong: Color,
    // destructive
    val danger: Color,
    val dangerOutline: Color,
    // scheme drawing
    val schemeGrid: Color,
    val schemePending: Color,   // poles and spans not captured yet
)

val LightPoeColors = PoeColors(
    isDark = false,
    ground = Color(0xFFEDF0EC),
    mapGround = Color(0xFFE3E8E3),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEDF0EC),
    cardBorder = Color.Transparent,
    overlay = Color(0xFFFFFFFF),
    tabTrack = Color(0xFFDDE3DE),
    tabSelected = Color(0xFFFFFFFF),
    ink = Color(0xFF0D1B16),
    inkSecondary = Color(0xFF3C4A44),
    muted = Color(0xFF4B5A54),
    outline = Color(0xFFB9C2BC),
    outlineVariant = Color(0xFFCBD3CD),
    inverse = Color(0xFF0D1B16),
    onInverse = Color(0xFFFFFFFF),
    iconTile = Color(0xFF0D1B16),
    onIconTile = Color(0xFFFFFFFF),
    dock = Color(0xFF0D1B16),
    onDock = Color(0xFFFFFFFF),
    dockBorder = Color.Transparent,
    action = Color(0xFFC2410C),
    onAction = Color(0xFFFFFFFF),
    actionText = Color(0xFF9A3412),
    actionContainer = Color(0xFFFDE7DC),
    onActionContainer = Color(0xFF9A3412),
    gps = Color(0xFF1F4FD1),
    gpsText = Color(0xFF1F3FA8),
    gpsContainer = Color(0xFFDCE6FB),
    onGpsContainer = Color(0xFF1F3FA8),
    gpsStrong = Color(0xFF1F4FD1),
    onGpsStrong = Color(0xFFFFFFFF),
    danger = Color(0xFFB42318),
    dangerOutline = Color(0xFFF2B8A0),
    schemeGrid = Color(0xFFD3DAD4),
    schemePending = Color(0xFFD5DBD6),
)

val DarkPoeColors = PoeColors(
    isDark = true,
    ground = Color(0xFF0B0D10),
    mapGround = Color(0xFF0E1115),
    surface = Color(0xFF15191E),
    surfaceMuted = Color(0xFF1E232A),
    cardBorder = Color(0xFF262B33),
    overlay = Color(0xFF1E232A),
    tabTrack = Color(0xFF15191E),
    tabSelected = Color(0xFF2A3039),
    ink = Color(0xFFF2F4F7),
    inkSecondary = Color(0xFFC9D0DA),
    muted = Color(0xFF9AA3B2),
    outline = Color(0xFF3A414C),
    outlineVariant = Color(0xFF3A414C),
    inverse = Color(0xFFF2F4F7),
    onInverse = Color(0xFF0B0D10),
    iconTile = Color(0xFF2A3039),
    onIconTile = Color(0xFFF2F4F7),
    dock = Color(0xFF1E232A),
    onDock = Color(0xFFF2F4F7),
    dockBorder = Color(0xFF2E3540),
    action = Color(0xFFFFB547),
    onAction = Color(0xFF1A1206),
    actionText = Color(0xFFFFB547),
    actionContainer = Color(0xFF2A1F0E),
    onActionContainer = Color(0xFFFFD08A),
    gps = Color(0xFF6FE3F5),
    gpsText = Color(0xFF6FE3F5),
    gpsContainer = Color(0xFF0F2A2F),
    onGpsContainer = Color(0xFF6FE3F5),
    gpsStrong = Color(0xFF0F2A2F),
    onGpsStrong = Color(0xFF6FE3F5),
    danger = Color(0xFFFF8A7A),
    dangerOutline = Color(0xFF5A2A24),
    schemeGrid = Color(0xFF181C22),
    schemePending = Color(0xFF2E3540),
)

internal val LocalPoeColors = staticCompositionLocalOf { LightPoeColors }
