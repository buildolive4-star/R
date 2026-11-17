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
    primary = AmberGold,
    onPrimary = AmberGoldDark,
    primaryContainer = AmberGlowContainer,
    onPrimaryContainer = IvoryText,
    secondary = TerracottaAccent,
    onSecondary = Color(0xFF2B120C),
    secondaryContainer = Color(0xFF47231A),
    onSecondaryContainer = IvoryText,
    tertiary = SoftSage,
    onTertiary = Color(0xFF0E291F),
    background = ObsidianBackground,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    surfaceContainerHighest = Color(0xFF343442),
    outline = SubtleOutlineDark,
    outlineVariant = Color(0xFF252530),
    error = ErrorCrimson,
    onError = Color(0xFF3A0912),
)

private val PixoraLightColorScheme = lightColorScheme(
    primary = BurnishedAmber,
    onPrimary = Color.White,
    primaryContainer = BurnishedAmberContainer,
    onPrimaryContainer = DeepEspresso,
    secondary = TerracottaAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD1),
    onSecondaryContainer = Color(0xFF3B130A),
    tertiary = Color(0xFF3D6B58),
    onTertiary = Color.White,
    background = AlabasterBackground,
    onBackground = CharcoalInk,
    surface = AlabasterSurface,
    onSurface = CharcoalInk,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = WarmStoneText,
    surfaceContainerLowest = AlabasterSurface,
    surfaceContainerLow = AlabasterBackground,
    surfaceContainer = AlabasterSurfaceVariant,
    surfaceContainerHigh = AlabasterSurfaceHighest,
    surfaceContainerHighest = Color(0xFFDAD3C8),
    outline = SubtleOutlineLight,
    outlineVariant = Color(0xFFEAE4DA),
    error = ErrorCrimsonLight,
    onError = Color.White,
)

val PixoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
