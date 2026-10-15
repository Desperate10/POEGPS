package com.poe.poegps.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** Corner radii used across the canvas. */
object PoeShapes {
    val chip = RoundedCornerShape(14.dp)       // tabs, small buttons, icon tiles
    val tile = RoundedCornerShape(16.dp)       // stat tiles, outlined buttons
    val field = RoundedCornerShape(18.dp)      // search field, tab track
    val row = RoundedCornerShape(20.dp)        // action rows
    val primary = RoundedCornerShape(22.dp)    // 72dp primary action
    val card = RoundedCornerShape(24.dp)       // list cards, dock
    val sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    val pill = RoundedCornerShape(percent = 50)
}

object PoeTheme {
    val colors: PoeColors
        @Composable @ReadOnlyComposable get() = LocalPoeColors.current
    val type: PoeTypography
        @Composable @ReadOnlyComposable get() = LocalPoeTypography.current
}

/** Follows the system light/dark setting. */
@Composable
fun PoeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkPoeColors else LightPoeColors
    val type = PoeTypography()
    CompositionLocalProvider(
        LocalPoeColors provides colors,
        LocalPoeTypography provides type,
    ) {
        // Material 3 components (dialogs, text fields, ripples) pick up the same palette
        MaterialTheme(colorScheme = colors.toMaterial()) {
            CompositionLocalProvider(LocalTextStyle provides type.body, content = content)
        }
    }
}

private fun PoeColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = action,
        onPrimary = onAction,
        primaryContainer = actionContainer,
        onPrimaryContainer = onActionContainer,
        secondary = gps,
        onSecondary = if (isDark) mapGround else onGpsStrong,
        secondaryContainer = gpsContainer,
        onSecondaryContainer = onGpsContainer,
        background = ground,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = muted,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceMuted,
        inverseSurface = inverse,
        inverseOnSurface = onInverse,
        outline = outline,
        outlineVariant = outlineVariant,
        error = danger,
        scrim = ink.copy(alpha = 0.4f),
    )
}
