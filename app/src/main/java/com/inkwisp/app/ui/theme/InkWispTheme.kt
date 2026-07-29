package com.inkwisp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkwisp.app.R

val Paper = Color(0xFFF5F0E6)
val PaperElevated = Color(0xFFFBF7EF)
val Ink = Color(0xFF211F1B)
val MutedInk = Color(0xFF716C63)
val Vermilion = Color(0xFFB84C38)
val DarkPaper = Color(0xFF171613)
val DarkElevated = Color(0xFF211F1A)
val DarkInk = Color(0xFFF0EADF)

val WenKaiFamily = FontFamily(Font(R.font.lxgw_wenkai_lite_regular))
val NewsreaderFamily = FontFamily(Font(R.font.newsreader_variable))

private val LightColors = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color(0xFFFFFAF3),
    primaryContainer = Color(0xFFEED9D2),
    onPrimaryContainer = Color(0xFF5D2117),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceContainer = Color(0xFFF0EADF),
    surfaceContainerLow = PaperElevated,
    surfaceContainerHigh = Color(0xFFE9E2D6),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFCFC6B8),
    outlineVariant = Color(0xFFE2DACD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD98370),
    onPrimary = Color(0xFF40140D),
    primaryContainer = Color(0xFF6C3026),
    onPrimaryContainer = Color(0xFFFFDAD1),
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkPaper,
    onSurface = DarkInk,
    surfaceContainer = DarkElevated,
    surfaceContainerLow = Color(0xFF1B1A17),
    surfaceContainerHigh = Color(0xFF2B2823),
    onSurfaceVariant = Color(0xFFBBB3A8),
    outline = Color(0xFF514B43),
    outlineVariant = Color(0xFF34312C),
)

private fun inkTypography(fontFamily: FontFamily) = androidx.compose.material3.Typography(
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 39.sp,
        lineHeight = 45.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 27.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 29.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.5.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.35.sp,
    ),
)

private val InkShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun InkWispTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val isChinese = LocalConfiguration.current.locales[0].language == "zh"
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = inkTypography(if (isChinese) WenKaiFamily else NewsreaderFamily),
        shapes = InkShapes,
        content = content,
    )
}
