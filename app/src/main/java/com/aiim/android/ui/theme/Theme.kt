package com.aiim.android.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = ChatPrimary,
    onPrimary = ChatOnPrimary,
    primaryContainer = ChatPrimaryContainer,
    onPrimaryContainer = ChatOnPrimaryContainer,
    secondary = ChatSecondary,
    onSecondary = ChatOnSecondary,
    secondaryContainer = ChatSecondaryContainer,
    onSecondaryContainer = ChatOnSecondaryContainer,
    background = ChatBackground,
    onBackground = ChatOnBackground,
    surface = ChatSurface,
    onSurface = ChatOnSurface,
    surfaceVariant = ChatSurfaceVariant,
    onSurfaceVariant = ChatOnSurfaceVariant,
    outline = ChatOutline,
    outlineVariant = ChatOutlineVariant,
    error = ChatError,
    onError = ChatOnError,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = ChatPrimaryDark,
    onPrimary = ChatOnPrimaryDark,
    primaryContainer = ChatPrimaryContainerDark,
    onPrimaryContainer = ChatOnPrimaryContainerDark,
    secondary = ChatOnSurfaceVariantDark,
    onSecondary = ChatBackgroundDark,
    secondaryContainer = ChatSurfaceVariantDark,
    onSecondaryContainer = ChatOnSurfaceDark,
    background = ChatBackgroundDark,
    onBackground = ChatOnSurfaceDark,
    surface = ChatSurfaceDark,
    onSurface = ChatOnSurfaceDark,
    surfaceVariant = ChatSurfaceVariantDark,
    onSurfaceVariant = ChatOnSurfaceVariantDark,
    outline = ChatOutlineDark,
    outlineVariant = ChatSurfaceVariantDark,
    error = Color(0xFFF87171),
    onError = ChatBackgroundDark,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFFE4E6)
)

@Composable
fun AIIMChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
