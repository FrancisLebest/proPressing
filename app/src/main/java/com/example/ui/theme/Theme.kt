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

private val LightPressingScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = CrispWhite,
    primaryContainer = SoftBlueContainer,
    onPrimaryContainer = MidnightNavy,
    secondary = MidnightNavy,
    onSecondary = CrispWhite,
    secondaryContainer = Color(0xFFDDE6FA),
    onSecondaryContainer = MidnightNavy,
    tertiary = AfricaGold,
    onTertiary = MidnightNavy,
    tertiaryContainer = WarmGoldContainer,
    onTertiaryContainer = Color(0xFF5C4200),
    background = IceBackground,
    onBackground = SlateText,
    surface = CrispWhite,
    onSurface = SlateText,
    surfaceVariant = Color(0xFFEAF0FB),
    onSurfaceVariant = MutedSlate,
    outline = SoftBorder,
    outlineVariant = Color(0xFFE8EEF8),
    error = CrimsonDanger,
    onError = CrispWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkPressingScheme = darkColorScheme(
    primary = BrightAzure,
    onPrimary = CrispWhite,
    primaryContainer = DeepNavySurface,
    onPrimaryContainer = DarkTextPrimary,
    secondary = AfricaGold,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFF3A2D00),
    onSecondaryContainer = WarmGoldContainer,
    tertiary = EmeraldSuccess,
    onTertiary = MidnightNavy,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1D305B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5C1515),
    onErrorContainer = CrimsonContainer
)

val PressingShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PressingProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPressingScheme else LightPressingScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PressingShapes,
        content = content
    )
}
