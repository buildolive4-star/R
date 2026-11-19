package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppLookTheme(
    val displayName: String,
    val subtitle: String,
    val swatchPrimary: Color,
    val swatchAccent: Color,
    val swatchSurface: Color,
) {
    DEFAULT(
        displayName = "Default",
        subtitle = "Rose Pink & Pearl White",
        swatchPrimary = DateMatePrimaryPink,
        swatchAccent = DateMateSecondaryPink,
        swatchSurface = DateMateSoftBlush,
    ),
    GOLD(
        displayName = "Gold",
        subtitle = "Royal Champagne Gold & Ivory",
        swatchPrimary = DateMateGoldPrimary,
        swatchAccent = DateMateGoldSecondary,
        swatchSurface = DateMateGoldSurfaceContainer,
    ),
    SILVER(
        displayName = "Silver",
        subtitle = "Platinum Silver & Cool Mist",
        swatchPrimary = DateMateSilverPrimary,
        swatchAccent = DateMateSilverSecondary,
        swatchSurface = DateMateSilverSurfaceContainer,
    ),
}

val LocalAppLookTheme = staticCompositionLocalOf { AppLookTheme.DEFAULT }

private val DateMateDefaultColorScheme = lightColorScheme(
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

private val DateMateGoldColorScheme = lightColorScheme(
    primary = DateMateGoldPrimary,
    onPrimary = DateMateWhite,
    primaryContainer = DateMateGoldSurfaceContainer,
    onPrimaryContainer = DateMateGoldDarkText,
    secondary = DateMateGoldSecondary,
    onSecondary = DateMateWhite,
    secondaryContainer = DateMateGoldSurfaceHigh,
    onSecondaryContainer = DateMateGoldDarkText,
    tertiary = DateMateGoldTertiary,
    onTertiary = DateMateGoldDarkText,
    tertiaryContainer = Color(0xFFFFF5D1),
    onTertiaryContainer = DateMateGoldDarkText,
    background = DateMateGoldBackground,
    onBackground = DateMateGoldDarkText,
    surface = DateMateWhite,
    onSurface = DateMateGoldDarkText,
    surfaceVariant = DateMateGoldSurfaceContainer,
    onSurfaceVariant = DateMateGoldMutedText,
    surfaceContainerLowest = DateMateWhite,
    surfaceContainerLow = DateMateGoldBackground,
    surfaceContainer = DateMateGoldSurfaceContainer,
    surfaceContainerHigh = DateMateGoldSurfaceHigh,
    surfaceContainerHighest = Color(0xFFFDE6A8),
    outline = DateMateGoldBorder,
    outlineVariant = Color(0xFFF5E3B3),
    error = ErrorCrimsonLight,
    onError = DateMateWhite,
)

private val DateMateSilverColorScheme = lightColorScheme(
    primary = DateMateSilverPrimary,
    onPrimary = DateMateWhite,
    primaryContainer = DateMateSilverSurfaceContainer,
    onPrimaryContainer = DateMateSilverDarkText,
    secondary = DateMateSilverSecondary,
    onSecondary = DateMateWhite,
    secondaryContainer = DateMateSilverSurfaceHigh,
    onSecondaryContainer = DateMateSilverDarkText,
    tertiary = DateMateSilverTertiary,
    onTertiary = DateMateWhite,
    tertiaryContainer = Color(0xFFE8EEF5),
    onTertiaryContainer = DateMateSilverDarkText,
    background = DateMateSilverBackground,
    onBackground = DateMateSilverDarkText,
    surface = DateMateWhite,
    onSurface = DateMateSilverDarkText,
    surfaceVariant = DateMateSilverSurfaceContainer,
    onSurfaceVariant = DateMateSilverMutedText,
    surfaceContainerLowest = DateMateWhite,
    surfaceContainerLow = DateMateSilverBackground,
    surfaceContainer = DateMateSilverSurfaceContainer,
    surfaceContainerHigh = DateMateSilverSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD5DFEB),
    outline = DateMateSilverBorder,
    outlineVariant = Color(0xFFDEE6F0),
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
    appLookTheme: AppLookTheme = AppLookTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val scheme = when (appLookTheme) {
        AppLookTheme.DEFAULT -> DateMateDefaultColorScheme
        AppLookTheme.GOLD -> DateMateGoldColorScheme
        AppLookTheme.SILVER -> DateMateSilverColorScheme
    }
    CompositionLocalProvider(LocalAppLookTheme provides appLookTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DateMateTypography,
            shapes = DateMateShapes,
            content = content,
        )
    }
}

@Composable
fun JustUsTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    isStaffTheme: Boolean = false,
    appLookTheme: AppLookTheme = AppLookTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    DateMateTheme(appLookTheme = appLookTheme, content = content)
}
