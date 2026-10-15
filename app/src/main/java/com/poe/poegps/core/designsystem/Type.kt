package com.poe.poegps.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.poe.poegps.R

// Bundled, not downloadable: the app must render correctly offline.
// Golos Text weights are static instances cut from the variable font, so they also work on API 24-25.
val GolosText = FontFamily(
    Font(R.font.golos_text_regular, FontWeight.Normal),
    Font(R.font.golos_text_medium, FontWeight.Medium),
    Font(R.font.golos_text_semibold, FontWeight.SemiBold),
    Font(R.font.golos_text_bold, FontWeight.Bold),
    Font(R.font.golos_text_extrabold, FontWeight.ExtraBold),
    Font(R.font.golos_text_black, FontWeight.Black),
)

val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

private fun golos(size: Int, weight: FontWeight, spacing: Double = 0.0, lineHeight: Double? = null) =
    TextStyle(
        fontFamily = GolosText,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = spacing.em,
        lineHeight = lineHeight?.let { (size * it).sp } ?: TextStyle.Default.lineHeight,
    )

/** Text styles taken from the canvas; names describe the role, sizes are in the comments. */
@Immutable
data class PoeTypography(
    val display: TextStyle = golos(168, FontWeight.Black, -0.07, 0.82),     // accepted count on the sync result
    val title: TextStyle = golos(26, FontWeight.ExtraBold, -0.02, 1.15),
    val hero: TextStyle = golos(52, FontWeight.Black, -0.045, 0.95),        // "Опора 13" in the line sheet
    val screenTitle: TextStyle = golos(44, FontWeight.Black, -0.04, 1.0),   // "Об'єкти"
    val sheetTitle: TextStyle = golos(40, FontWeight.Black, -0.04, 0.95),   // pole actions sheet
    val cardTitle: TextStyle = golos(18, FontWeight.ExtraBold, -0.01),
    val action: TextStyle = golos(20, FontWeight.ExtraBold),               // 72dp primary button
    val actionWaiting: TextStyle = golos(18, FontWeight.ExtraBold),        // same button while disabled
    val statValue: TextStyle = golos(20, FontWeight.ExtraBold),
    val rowTitle: TextStyle = golos(16, FontWeight.ExtraBold),
    val input: TextStyle = golos(16, FontWeight.SemiBold),
    val titleSmall: TextStyle = golos(15, FontWeight.ExtraBold),           // title pill over the map
    val tab: TextStyle = golos(15, FontWeight.SemiBold),
    val tabSelected: TextStyle = golos(15, FontWeight.Bold),
    val button: TextStyle = golos(15, FontWeight.Bold),
    val label: TextStyle = golos(14, FontWeight.Bold),                      // selected chip
    val labelRegular: TextStyle = golos(14, FontWeight.SemiBold),           // unselected chip
    val statSuffix: TextStyle = golos(14, FontWeight.ExtraBold),
    val body: TextStyle = golos(14, FontWeight.Medium, lineHeight = 1.4),
    val overline: TextStyle = golos(13, FontWeight.Bold, 0.06),            // "НАСТУПНА ОПОРА"
    val caption: TextStyle = golos(13, FontWeight.SemiBold),
    val captionStrong: TextStyle = golos(13, FontWeight.Bold),
    val captionRegular: TextStyle = golos(13, FontWeight.Medium),
    val badge: TextStyle = golos(12, FontWeight.Bold),
    val small: TextStyle = golos(12, FontWeight.SemiBold),
    val mono: TextStyle = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.5.sp),
    val monoStrong: TextStyle = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    val monoSmall: TextStyle = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)

internal val LocalPoeTypography = staticCompositionLocalOf { PoeTypography() }
