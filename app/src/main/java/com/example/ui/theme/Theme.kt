package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = DrawerActiveBg,
    onPrimaryContainer = DeepBlue,
    secondary = DeepBlue,
    onSecondary = Color.White,
    secondaryContainer = AccentCyanSoft,
    onSecondaryContainer = DeepBlue,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = IceBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CardWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = InputBgLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SoftBorderLight,
    error = DangerRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = DrawerDarkBlue,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentCyan,
    onSecondary = DarkNavy,
    secondaryContainer = SoftBorderDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = IceBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = InputBgDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SoftBorderDark,
    error = DangerRed,
    onError = Color.White
)

val BrohiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BrohiIceTheme(
    darkTheme: Boolean = false,
    isUrduRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val layoutDirection = if (isUrduRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = BrohiShapes,
            content = content
        )
    }
}
