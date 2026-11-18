package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================================
// DateMate Light-Only Theme — IMPORTANT: Never create a dark theme
// 75% White / Light (#FFFFFF / #FFFAFC)
// 15% Soft Blush Pink (#FFF0F5)
// 10% Rose Pink Accent (#E91E63 / #FF6F9C)
// ============================================================================
private val DateMateLightColorScheme = lightColorScheme(
    primary = DateMatePrimaryPink,
    onPrimary = DateMateWhite,
    primaryContainer = DateMateSoftBlush,
    onPrimaryContainer = DateMateDarkText,
    secondary = DateMateSecondaryPink,
    onSecondary = DateMateWhite,
    secondaryContainer = Color(0xFFFFE4EE),
    onSecondaryContainer = DateMateDarkText,
    tertiary = DateMateRoseGold,
    onTertiary = DateMateWhite,
    tertiaryContainer = Color(0xFFFDF2F2),
    onTertiaryContainer = DateMateDarkText,
    background = DateMateBackground,
    onBackground = DateMateDarkText,
    surface = DateMateWhite,
    onSurface = DateMateDarkText,
    surfaceVariant = DateMateSoftBlush,
    onSurfaceVariant = DateMateSecondaryText,
    surfaceContainerLowest = DateMateWhite,
    surfaceContainerLow = DateMateBackground,
    surfaceContainer = DateMateWarmBlushSurface,
    surfaceContainerHigh = DateMateSoftBlush,
    surfaceContainerHighest = Color(0xFFFCE4EC),
    outline = DateMateSoftBorder,
    outlineVariant = Color(0xFFF9E5ED),
    error = ErrorCrimsonLight,
    onError = DateMateWhite,
)

val DateMateShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

val JustUsShapes = DateMateShapes

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

@Composable
fun DateMateTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DateMateLightColorScheme,
        typography = DateMateTypography,
        shapes = DateMateShapes,
        content = content,
    )
}

@Composable
fun JustUsTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    isStaffTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Strictly enforce light-only theme per DateMate specification
    DateMateTheme(content = content)
}
