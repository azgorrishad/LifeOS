package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SleekLightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = Color(0xFF0D2004),
    secondary = Color(0xFF54634D),
    background = SleekBackground,
    surface = SleekSurface,
    surfaceVariant = SleekSurfaceVariant,
    onPrimary = Color.White,
    onBackground = SleekOnBackground,
    onSurface = SleekOnBackground,
    onSurfaceVariant = Color(0xFF42493E),
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant,
    error = Color(0xFFBA1A1A)
)

private val SleekDarkColorScheme = darkColorScheme(
    primary = SleekPrimaryDark,
    primaryContainer = SleekPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFD7E8CD),
    secondary = Color(0xFFBCCBB0),
    background = SleekBackgroundDark,
    surface = SleekSurfaceDark,
    surfaceVariant = SleekSurfaceVariantDark,
    onPrimary = Color(0xFF0D2004),
    onBackground = SleekOnBackgroundDark,
    onSurface = SleekOnBackgroundDark,
    onSurfaceVariant = Color(0xFFC4C8BB),
    outline = SleekOutlineDark,
    outlineVariant = SleekOutlineVariantDark,
    error = Color(0xFFFFB4AB)
)

private val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimary,
    primaryContainer = OceanPrimaryContainer,
    background = OceanBackground,
    surface = OceanSurface,
    surfaceVariant = OceanSurfaceVariant,
    onPrimary = Color.White,
    onBackground = OceanOnBackground,
    onSurface = OceanOnBackground,
    error = Color(0xFFBA1A1A)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    primaryContainer = OceanPrimaryContainerDark,
    background = OceanBackgroundDark,
    surface = OceanSurfaceDark,
    surfaceVariant = OceanSurfaceVariantDark,
    onPrimary = Color(0xFF00344F),
    onBackground = OceanOnBackgroundDark,
    onSurface = OceanOnBackgroundDark,
    error = Color(0xFFFFB4AB)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    primaryContainer = SunsetPrimaryContainer,
    background = SunsetBackground,
    surface = SunsetSurface,
    surfaceVariant = SunsetSurfaceVariant,
    onPrimary = Color.White,
    onBackground = SunsetOnBackground,
    onSurface = SunsetOnBackground,
    error = Color(0xFFBA1A1A)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetPrimaryDark,
    primaryContainer = SunsetPrimaryContainerDark,
    background = SunsetBackgroundDark,
    surface = SunsetSurfaceDark,
    surfaceVariant = SunsetSurfaceVariantDark,
    onPrimary = Color(0xFF552000),
    onBackground = SunsetOnBackgroundDark,
    onSurface = SunsetOnBackgroundDark,
    error = Color(0xFFFFB4AB)
)

private val LavenderLightColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    primaryContainer = LavenderPrimaryContainer,
    background = LavenderBackground,
    surface = LavenderSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onPrimary = Color.White,
    onBackground = LavenderOnBackground,
    onSurface = LavenderOnBackground,
    error = Color(0xFFBA1A1A)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = LavenderPrimaryDark,
    primaryContainer = LavenderPrimaryContainerDark,
    background = LavenderBackgroundDark,
    surface = LavenderSurfaceDark,
    surfaceVariant = LavenderSurfaceVariantDark,
    onPrimary = Color(0xFF4A2532),
    onBackground = LavenderOnBackgroundDark,
    onSurface = LavenderOnBackgroundDark,
    error = Color(0xFFFFB4AB)
)

private val CyberpunkLightColorScheme = lightColorScheme(
    primary = CyberpunkPrimary,
    primaryContainer = CyberpunkPrimaryContainer,
    background = CyberpunkBackground,
    surface = CyberpunkSurface,
    surfaceVariant = CyberpunkSurfaceVariant,
    onPrimary = Color.Black,
    onBackground = CyberpunkOnBackground,
    onSurface = CyberpunkOnBackground,
    outline = CyberpunkOutline,
    outlineVariant = CyberpunkOutlineVariant,
    error = Color(0xFFBA1A1A)
)

private val CyberpunkDarkColorScheme = darkColorScheme(
    primary = CyberpunkPrimaryDark,
    primaryContainer = CyberpunkPrimaryContainerDark,
    background = CyberpunkBackgroundDark,
    surface = CyberpunkSurfaceDark,
    surfaceVariant = CyberpunkSurfaceVariantDark,
    onPrimary = Color.Black,
    onBackground = CyberpunkOnBackgroundDark,
    onSurface = CyberpunkOnBackgroundDark,
    outline = CyberpunkOutlineDark,
    outlineVariant = CyberpunkOutlineVariantDark,
    error = Color(0xFFFFB4AB)
)

private val MidnightLightColorScheme = lightColorScheme(
    primary = MidnightPrimary,
    primaryContainer = MidnightPrimaryContainer,
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onPrimary = Color.White,
    onBackground = MidnightOnBackground,
    onSurface = MidnightOnBackground,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineVariant,
    error = Color(0xFFBA1A1A)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = MidnightPrimaryDark,
    primaryContainer = MidnightPrimaryContainerDark,
    background = MidnightBackgroundDark,
    surface = MidnightSurfaceDark,
    surfaceVariant = MidnightSurfaceVariantDark,
    onPrimary = Color.White,
    onBackground = MidnightOnBackgroundDark,
    onSurface = MidnightOnBackgroundDark,
    outline = MidnightOutlineDark,
    outlineVariant = MidnightOutlineVariantDark,
    error = Color(0xFFFFB4AB)
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    surfaceVariant = EmeraldSurfaceVariant,
    onPrimary = Color.White,
    onBackground = EmeraldOnBackground,
    onSurface = EmeraldOnBackground,
    outline = EmeraldOutline,
    outlineVariant = EmeraldOutlineVariant,
    error = Color(0xFFBA1A1A)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    background = EmeraldBackgroundDark,
    surface = EmeraldSurfaceDark,
    surfaceVariant = EmeraldSurfaceVariantDark,
    onPrimary = Color.Black,
    onBackground = EmeraldOnBackgroundDark,
    onSurface = EmeraldOnBackgroundDark,
    outline = EmeraldOutlineDark,
    outlineVariant = EmeraldOutlineVariantDark,
    error = Color(0xFFFFB4AB)
)

enum class AppTheme(val displayName: String) {
    SLEEK_LIGHT("Sleek Light"), 
    SLEEK_DARK("Sleek Dark"), 
    MIDNIGHT_BLUE("Midnight Blue"), 
    EMERALD_FOREST("Emerald Forest"),
    CYBERPUNK("Cyberpunk"),
    CALM_NATURE("Calm Nature")
}

@Composable
fun MyApplicationTheme(
    dynamicColor: Boolean = true, // Default to true for dynamic color support
    appTheme: AppTheme = AppTheme.SLEEK_DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.SLEEK_LIGHT -> false
        AppTheme.SLEEK_DARK -> true
        AppTheme.MIDNIGHT_BLUE -> true
        AppTheme.EMERALD_FOREST -> true
        AppTheme.CYBERPUNK -> true
        AppTheme.CALM_NATURE -> false
    }
    
    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (appTheme) {
            AppTheme.SLEEK_LIGHT -> SleekLightColorScheme
            AppTheme.SLEEK_DARK -> SleekDarkColorScheme
            AppTheme.MIDNIGHT_BLUE -> MidnightDarkColorScheme
            AppTheme.EMERALD_FOREST -> EmeraldDarkColorScheme
            AppTheme.CYBERPUNK -> CyberpunkDarkColorScheme
            AppTheme.CALM_NATURE -> OceanLightColorScheme
        }
    }

    val animatedPrimary by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.primary, animationSpec = androidx.compose.animation.core.tween(1000), label = "primary")
    val animatedPrimaryContainer by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.primaryContainer, animationSpec = androidx.compose.animation.core.tween(1000), label = "primaryContainer")
    val animatedSecondary by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.secondary, animationSpec = androidx.compose.animation.core.tween(1000), label = "secondary")
    val animatedBackground by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.background, animationSpec = androidx.compose.animation.core.tween(1000), label = "background")
    val animatedSurface by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.surface, animationSpec = androidx.compose.animation.core.tween(1000), label = "surface")
    val animatedSurfaceVariant by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.surfaceVariant, animationSpec = androidx.compose.animation.core.tween(1000), label = "surfaceVariant")
    val animatedOnPrimary by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.onPrimary, animationSpec = androidx.compose.animation.core.tween(1000), label = "onPrimary")
    val animatedOnBackground by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.onBackground, animationSpec = androidx.compose.animation.core.tween(1000), label = "onBackground")
    val animatedOnSurface by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.onSurface, animationSpec = androidx.compose.animation.core.tween(1000), label = "onSurface")
    val animatedOnSurfaceVariant by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.onSurfaceVariant, animationSpec = androidx.compose.animation.core.tween(1000), label = "onSurfaceVariant")
    val animatedOutline by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.outline, animationSpec = androidx.compose.animation.core.tween(1000), label = "outline")
    val animatedOutlineVariant by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.outlineVariant, animationSpec = androidx.compose.animation.core.tween(1000), label = "outlineVariant")
    val animatedError by androidx.compose.animation.animateColorAsState(targetValue = targetColorScheme.error, animationSpec = androidx.compose.animation.core.tween(1000), label = "error")

    val colorScheme = targetColorScheme.copy(
        primary = animatedPrimary,
        primaryContainer = animatedPrimaryContainer,
        secondary = animatedSecondary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        onPrimary = animatedOnPrimary,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
        onSurfaceVariant = animatedOnSurfaceVariant,
        outline = animatedOutline,
        outlineVariant = animatedOutlineVariant,
        error = animatedError
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
