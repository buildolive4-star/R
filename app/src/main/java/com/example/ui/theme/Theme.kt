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

private val JustUsDarkColorScheme = darkColorScheme(
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
    surfaceContainerHighest = Color(0xFF3B2A42),
    outline = SubtleOutlineDark,
    outlineVariant = Color(0xFF2C2030),
    error = ErrorCrimson,
    onError = Color(0xFF3E0614),
)

private val JustUsLightColorScheme = lightColorScheme(
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

private val StaffDarkColorScheme = darkColorScheme(
    primary = StaffEmeraldPrimary,
    onPrimary = StaffOnEmeraldPrimary,
    primaryContainer = StaffEmeraldContainer,
    onPrimaryContainer = StaffIvoryText,
    secondary = StaffRoyalGoldSecondary,
    onSecondary = Color(0xFF332404),
    secondaryContainer = Color(0xFF4B370C),
    onSecondaryContainer = StaffIvoryText,
    tertiary = StaffTealTertiary,
    onTertiary = Color(0xFF042B24),
    background = StaffMidnightBackground,
    onBackground = StaffIvoryText,
    surface = StaffMidnightSurface,
    onSurface = StaffIvoryText,
    surfaceVariant = StaffMidnightSurfaceElevated,
    onSurfaceVariant = StaffMutedSageText,
    surfaceContainerLowest = StaffMidnightBackground,
    surfaceContainerLow = StaffMidnightSurface,
    surfaceContainer = StaffMidnightSurfaceElevated,
    surfaceContainerHigh = StaffMidnightSurfaceHighest,
    surfaceContainerHighest = Color(0xFF24473B),
    outline = StaffOutlineDark,
    outlineVariant = Color(0xFF19352C),
    error = ErrorCrimson,
    onError = Color(0xFF3E0614),
)

private val StaffLightColorScheme = lightColorScheme(
    primary = StaffDeepEmerald,
    onPrimary = Color.White,
    primaryContainer = StaffDayContainer,
    onPrimaryContainer = StaffDeepInk,
    secondary = Color(0xFFA67016),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7BE),
    onSecondaryContainer = Color(0xFF301F02),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    background = StaffDayBackground,
    onBackground = StaffDeepInk,
    surface = StaffDaySurface,
    onSurface = StaffDeepInk,
    surfaceVariant = StaffDaySurfaceVariant,
    onSurfaceVariant = StaffWarmSlateText,
    surfaceContainerLowest = StaffDaySurface,
    surfaceContainerLow = StaffDayBackground,
    surfaceContainer = StaffDaySurfaceVariant,
    surfaceContainerHigh = StaffDaySurfaceHighest,
    surfaceContainerHighest = Color(0xFFC3E0D4),
    outline = StaffOutlineLight,
    outlineVariant = Color(0xFFD9EDE5),
    error = ErrorCrimsonLight,
    onError = Color.White,
)

val JustUsShapes = Shapes(
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
fun JustUsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    isStaffTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        isStaffTheme && isDark -> StaffDarkColorScheme
        isStaffTheme && !isDark -> StaffLightColorScheme
        isDark -> JustUsDarkColorScheme
        else -> JustUsLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = JustUsTypography,
        shapes = JustUsShapes,
        content = content,
    )
}
