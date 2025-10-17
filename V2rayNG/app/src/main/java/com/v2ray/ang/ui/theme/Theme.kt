package com.v2ray.ang.ui.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricAzure,
    onPrimary = SnowyWhite,
    primaryContainer = MidnightShadow,
    onPrimaryContainer = MidnightBlue,
    secondary = AquaAccent,
    onSecondary = SnowyWhite,
    background = MidnightBlue,
    onBackground = SnowyWhite,
    surface = MidnightNavy,
    onSurface = SnowyWhite,
    surfaceVariant = MidnightShadow,
    onSurfaceVariant = MistySlate,
    outline = GraphiteOutline,
    error = CoralAlert,
    onError = SnowyWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CeruleanPulse,
    onPrimary = Color.White,
    primaryContainer = GlacierBlue,
    onPrimaryContainer = MidnightBlue,
    secondary = AquaAccent,
    onSecondary = MidnightBlue,
    background = Color(0xFFF5FBFF),
    onBackground = MidnightBlue,
    surface = Color(0xFFF1F7FF),
    onSurface = MidnightBlue,
    surfaceVariant = Color(0xFFE0ECFF),
    onSurfaceVariant = GraphiteOutline,
    outline = GraphiteOutline,
    error = CoralAlert,
    onError = Color.White
)

@Composable
fun ATGateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ATGateTypography,
        shapes = ATGateShapes,
        content = content
    )
}
