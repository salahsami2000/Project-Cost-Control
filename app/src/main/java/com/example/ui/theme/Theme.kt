package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = OnBrandPrimaryContainer,
    secondary = EvColor,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F5F5),
    onSecondaryContainer = Color(0xFF0A4D4D),
    tertiary = EtcColor,
    onTertiary = Color.White,
    background = PanelBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE9EEF4),
    onSurfaceVariant = TextSecondary,
    outline = CardBorderSubtle,
    error = StatusBad,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBrandPrimary,
    onPrimary = Color(0xFF3E0507),
    primaryContainer = Color(0xFF5D0E10),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = EvColor,
    onSecondary = Color.White,
    tertiary = EtcColor,
    background = DarkBackground,
    onBackground = Color(0xFFF3F4F6),
    surface = DarkSurface,
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF374151),
    error = StatusBad
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun ProjectCostTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
