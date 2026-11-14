package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ScamShieldExtendedColors(
    val navyHeader: Color,
    val navySecondary: Color,
    val cardBorder: Color,
    val safe: Color,
    val safeBg: Color,
    val caution: Color,
    val cautionBg: Color,
    val highRisk: Color,
    val highRiskBg: Color,
    val criticalRisk: Color,
    val criticalRiskBg: Color,
    val neutralStatus: Color,
    val neutralStatusBg: Color,
    val softBlueCard: Color,
    val secondaryText: Color
)

private val LightExtendedColors = ScamShieldExtendedColors(
    navyHeader = NavyPrimary,
    navySecondary = NavySecondary,
    cardBorder = BorderLight,
    safe = StatusSafe,
    safeBg = StatusSafeBgLight,
    caution = StatusCaution,
    cautionBg = StatusCautionBgLight,
    highRisk = StatusHighRisk,
    highRiskBg = StatusHighRiskBgLight,
    criticalRisk = StatusCriticalRisk,
    criticalRiskBg = StatusCriticalRiskBgLight,
    neutralStatus = StatusNeutral,
    neutralStatusBg = StatusNeutralBgLight,
    softBlueCard = BlueLightBg,
    secondaryText = TextSecondaryLight
)

private val DarkExtendedColors = ScamShieldExtendedColors(
    navyHeader = NavyDeep,
    navySecondary = NavySecondary,
    cardBorder = BorderDark,
    safe = StatusSafe,
    safeBg = StatusSafeBgDark,
    caution = StatusCaution,
    cautionBg = StatusCautionBgDark,
    highRisk = StatusHighRisk,
    highRiskBg = StatusHighRiskBgDark,
    criticalRisk = StatusCriticalRisk,
    criticalRiskBg = StatusCriticalRiskBgDark,
    neutralStatus = StatusNeutral,
    neutralStatusBg = StatusNeutralBgDark,
    softBlueCard = NavyElevatedDark,
    secondaryText = TextSecondaryDark
)

val LocalScamShieldColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    primaryContainer = BlueLightBg,
    onPrimaryContainer = NavyPrimary,
    secondary = NavySecondary,
    onSecondary = White,
    secondaryContainer = BlueSoftTint,
    onSecondaryContainer = NavyPrimary,
    tertiary = BlueBright,
    onTertiary = White,
    background = White,
    onBackground = TextMainLight,
    surface = White,
    onSurface = TextMainLight,
    surfaceVariant = BlueLightBg,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = StatusHighRisk,
    onError = White,
    errorContainer = StatusHighRiskBgLight,
    onErrorContainer = StatusCriticalRisk
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueBright,
    onPrimary = NavyDeep,
    primaryContainer = NavyElevatedDark,
    onPrimaryContainer = White,
    secondary = BlueMutedDark,
    onSecondary = NavyDeep,
    secondaryContainer = NavySecondary,
    onSecondaryContainer = White,
    tertiary = BluePrimary,
    onTertiary = White,
    background = NavyDeep,
    onBackground = TextMainDark,
    surface = NavySurfaceDark,
    onSurface = TextMainDark,
    surfaceVariant = NavyElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = StatusHighRisk,
    onError = White,
    errorContainer = StatusHighRiskBgDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

val ScamShieldShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ScamShieldTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalScamShieldColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScamShieldTypography,
            shapes = ScamShieldShapes,
            content = content
        )
    }
}
