package com.example.scanit.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    secondary = Slate300,
    tertiary = Gray400,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    error = Color(0xFFEF4444)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    secondary = Slate700,
    tertiary = Slate600,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    error = Color(0xFFDC2626)
)

@Composable
fun SCANitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val targetColorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val primary = animateColorAsState(targetValue = targetColorScheme.primary, animationSpec = tween(400), label = "primary").value
    val secondary = animateColorAsState(targetValue = targetColorScheme.secondary, animationSpec = tween(400), label = "secondary").value
    val tertiary = animateColorAsState(targetValue = targetColorScheme.tertiary, animationSpec = tween(400), label = "tertiary").value
    val background = animateColorAsState(targetValue = targetColorScheme.background, animationSpec = tween(400), label = "background").value
    val surface = animateColorAsState(targetValue = targetColorScheme.surface, animationSpec = tween(400), label = "surface").value
    val surfaceVariant = animateColorAsState(targetValue = targetColorScheme.surfaceVariant, animationSpec = tween(400), label = "surfaceVariant").value
    val onPrimary = animateColorAsState(targetValue = targetColorScheme.onPrimary, animationSpec = tween(400), label = "onPrimary").value
    val onSecondary = animateColorAsState(targetValue = targetColorScheme.onSecondary, animationSpec = tween(400), label = "onSecondary").value
    val onBackground = animateColorAsState(targetValue = targetColorScheme.onBackground, animationSpec = tween(400), label = "onBackground").value
    val onSurface = animateColorAsState(targetValue = targetColorScheme.onSurface, animationSpec = tween(400), label = "onSurface").value
    val error = animateColorAsState(targetValue = targetColorScheme.error, animationSpec = tween(400), label = "error").value

    val animatedColorScheme = targetColorScheme.copy(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        error = error
    )

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}