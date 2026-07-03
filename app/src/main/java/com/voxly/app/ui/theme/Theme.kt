package com.voxly.app.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp

// Palette from ActiveCallScreen
val CallScreenBackground = Color(0xFF1E293B)
val CallScreenSurface = Color(0xFF334155) // Slightly lighter slate
val CallScreenPrimary = Color(0xFF3B82F6) // Blue accent (standard) or keep Purple if they like that? 
// User asked to implement "audio and video call screen UI I mean theme and colors"
// The ActiveCallScreen uses 0xFF1E293B background and White text.
// Controls have transparency.
// Let's map this to the material scheme.

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary, 
    onPrimary = Color.White,
    primaryContainer = PremiumSurface,
    onPrimaryContainer = PremiumSilver,
    
    secondary = PremiumGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2723), // Dark Brown/Gold mix for containers
    onSecondaryContainer = PremiumGold,
    
    tertiary = Pink80,
    
    background = PremiumBlack,
    onBackground = PremiumSilver,
    
    surface = PremiumBlack, 
    onSurface = PremiumSilver,
    
    surfaceVariant = PremiumSurface,
    onSurfaceVariant = PremiumSilver,
    
    outline = TextGray,
    outlineVariant = PremiumSurface
)

private val LightColorScheme = darkColorScheme( // Force Dark colors even for "Light" scheme mapping
    primary = BrandPrimary, 
    onPrimary = Color.White,
    primaryContainer = PremiumSurface,
    onPrimaryContainer = PremiumSilver,
    
    secondary = PremiumGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2723),
    onSecondaryContainer = PremiumGold,
    
    tertiary = Pink80,
    
    background = PremiumBlack,
    onBackground = PremiumSilver,
    
    surface = PremiumBlack,
    onSurface = PremiumSilver,
    
    surfaceVariant = PremiumSurface,
    onSurfaceVariant = PremiumSilver,
    
    outline = TextGray,
    outlineVariant = PremiumSurface
)

@Composable
fun VoxlyTheme(
    darkTheme: Boolean = true, // Force Dark Theme by default
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled default to force our branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Even if dynamic, we might want to force dark if we want that look? 
            // But let's respect dynamic if they asked for it (they didn't). 
            // For now, let's stick to our custom palette as "Material 3 dynamic" often overrides custom branding.
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Use background color for status bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // Always light text/icons on dark background
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Will error until Type.kt exists, I will create it next
        content = content
    )
}

object VoxlyDimens {
    val CardCornerRadius = 24.dp
    val HeaderHeight = 56.dp
    val NavHeight = 64.dp
    val SpacingBetweenCards = 10.dp
    val SpacingBelowHeader = 8.dp
    val TagCornerRadius = 50.dp
    val ChipHeight = 28.dp
    val FabSize = 48.dp
    val HeaderHorizontalPadding = 16.dp
    val CardHorizontalPadding = 12.dp
    val SmallIconSize = 14.dp
    val MediumIconSize = 20.dp
    
    // Expanded tokens
    val BorderWidthThin = 0.5.dp
    val BorderWidthThick = 1.dp
    val DefaultContentPadding = 16.dp
    val WalletPillPaddingStart = 12.dp
    val WalletPillPaddingEnd = 6.dp
    val WalletPillPaddingVertical = 4.dp
}

object VoxlySpecs {
    val ProfileImageAspectRatio = 0.88f // Aspect ratio of the profile card (width / height)
    val GradientOverlayStart = 180f      // Y offset for gradient start
    val CardElevation = 0.dp
}

object VoxlyAnim {
    const val NormalDurationMs = 300
    const val FastDurationMs = 150
}
