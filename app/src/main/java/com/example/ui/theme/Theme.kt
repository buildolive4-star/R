package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PixoraDarkColorScheme = darkColorScheme(
    primary = RoseQuartzPrimary,
    onPrimary = RoseQuartzOnPrimary,
    primaryContainer = RoseGlowContainer,
    onPrimaryContainer = SilkIvoryText,
    secondary = ChampagneGoldSecondary,
    onSecondary = Color(0xFF332004),
    secondaryContainer = Color(0xFF4D3410),
    onSecondaryContainer = SilkIvoryText,
    tertiary = SoftLavenderTertiary,
    onTertiary = Color(0xFF211142),
    background = VelvetMidnightBackground,
    onBackground = SilkIvoryText,
    surface = VelvetMidnightSurface,
    onSurface = SilkIvoryText,
    surfaceVariant = VelvetMidnightSurfaceElevated,
    onSurfaceVariant = MutedPlumText,
    surfaceContainerLowest = VelvetMidnightBackground,
    surfaceContainerLow = VelvetMidnightSurface,
    surfaceContainer = VelvetMidnightSurfaceElevated,
    surfaceContainerHigh = VelvetMidnightSurfaceHighest,
    surfaceContainerHighest = Color(0xFF3A2A40),
    outline = SubtleOutlineDark,
    outlineVariant = Color(0xFF2C2030),
    error = ErrorCrimson,
    onError = Color(0xFF3E0614),
)

private val PixoraLightColorScheme = lightColorScheme(
    primary = DeepVelvetRose,
    onPrimary = Color.White,
    primaryContainer = DeepRoseContainer,
    onPrimaryContainer = DeepWineText,
    secondary = Color(0xFFA86D24),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5C2),
    onSecondaryContainer = Color(0xFF331E02),
    tertiary = Color(0xFF6D4C9E),
    onTertiary = Color.White,
    background = SilkBlushBackground,
    onBackground = PlumCharcoalInk,
    surface = SilkBlushSurface,
    onSurface = PlumCharcoalInk,
    surfaceVariant = SilkBlushSurfaceVariant,
    onSurfaceVariant = WarmPlumStoneText,
    surfaceContainerLowest = SilkBlushSurface,
    surfaceContainerLow = SilkBlushBackground,
    surfaceContainer = SilkBlushSurfaceVariant,
    surfaceContainerHigh = SilkBlushSurfaceHighest,
    surfaceContainerHighest = Color(0xFFE5D2DB),
    outline = SubtleOutlineLight,
    outlineVariant = Color(0xFFF0E0E7),
    error = ErrorCrimsonLight,
    onError = Color.White,
)

val PixoraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

@Composable
fun PixoraTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (isDark) PixoraDarkColorScheme else PixoraLightColorScheme,
        typography = PixoraTypography,
        shapes = PixoraShapes,
        content = content,
    )
}
