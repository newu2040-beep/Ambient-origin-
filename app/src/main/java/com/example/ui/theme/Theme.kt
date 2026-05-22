package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val MidnightScheme = darkColorScheme(
    primary = TealBright, secondary = PurpleAccent, tertiary = BlueAccent,
    background = SlateDark, surface = SlateAccent,
    onPrimary = SlateDarker, onSecondary = SlateDarker, onTertiary = SlateDarker,
    onBackground = Teal80, onSurface = Teal80
)

private val ForestScheme = darkColorScheme(
    primary = Color(0xFF81C784), secondary = Color(0xFFAED581), tertiary = Color(0xFFFFD54F),
    background = Color(0xFF1B2E24), surface = Color(0xFF263D31),
    onPrimary = Color(0xFF003823), onBackground = Color(0xFFE8F5E9), onSurface = Color(0xFFE8F5E9)
)

private val OceanScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7), secondary = Color(0xFF81D4FA), tertiary = Color(0xFF80DEEA),
    background = Color(0xFF082234), surface = Color(0xFF10334A),
    onPrimary = Color(0xFF01364B), onBackground = Color(0xFFE1F5FE), onSurface = Color(0xFFE1F5FE)
)

private val SunsetScheme = darkColorScheme(
    primary = Color(0xFFFF8A65), secondary = Color(0xFFFFB74D), tertiary = Color(0xFFE57373),
    background = Color(0xFF3E1D19), surface = Color(0xFF4E2A25),
    onPrimary = Color(0xFF4A1001), onBackground = Color(0xFFFBE9E7), onSurface = Color(0xFFFBE9E7)
)

private val CanyonScheme = darkColorScheme(
    primary = Color(0xFFD4A373), secondary = Color(0xFFFAEDCD), tertiary = Color(0xFFE9EDC9),
    background = Color(0xFF33231B), surface = Color(0xFF433229),
    onPrimary = Color(0xFF3D2314), onBackground = Color(0xFFFEFAE0), onSurface = Color(0xFFFEFAE0)
)

private val RiverScheme = darkColorScheme(
    primary = Color(0xFF80CBC4), secondary = Color(0xFFB2DFDB), tertiary = Color(0xFF4DB6AC),
    background = Color(0xFF183133), surface = Color(0xFF224345),
    onPrimary = Color(0xFF00363A), onBackground = Color(0xFFE0F2F1), onSurface = Color(0xFFE0F2F1)
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val currentTheme by ThemeManager.currentTheme.collectAsStateWithLifecycle()
    val isLightMode by ThemeManager.isLightMode.collectAsStateWithLifecycle()
    
    val baseScheme = when(currentTheme) {
        ThemeManager.ThemeOption.MIDNIGHT -> MidnightScheme
        ThemeManager.ThemeOption.FOREST -> ForestScheme
        ThemeManager.ThemeOption.OCEAN -> OceanScheme
        ThemeManager.ThemeOption.SUNSET -> SunsetScheme
        ThemeManager.ThemeOption.CANYON -> CanyonScheme
        ThemeManager.ThemeOption.RIVER -> RiverScheme
        ThemeManager.ThemeOption.DESERT -> darkColorScheme(primary = Color(0xFFE0A96D), background = Color(0xFF2C1E16), surface = Color(0xFF3E2C22))
        ThemeManager.ThemeOption.MOUNTAIN -> darkColorScheme(primary = Color(0xFF90A4AE), background = Color(0xFF1C2321), surface = Color(0xFF263238))
        ThemeManager.ThemeOption.COSMOS -> darkColorScheme(primary = Color(0xFFB39DDB), background = Color(0xFF0F0C29), surface = Color(0xFF1E1945))
        ThemeManager.ThemeOption.AURORA -> darkColorScheme(primary = Color(0xFF69F0AE), background = Color(0xFF0D1B2A), surface = Color(0xFF1B263B))
        ThemeManager.ThemeOption.VOLCANO -> darkColorScheme(primary = Color(0xFFFF5252), background = Color(0xFF210000), surface = Color(0xFF3E0000))
    }
    
    val colorScheme = if (isLightMode) {
        // Automatically invert baseline colors for light mode
        baseScheme.copy(
            background = Color.White,
            surface = Color(0xFFF5F5F5),
            onPrimary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    } else {
        baseScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightMode
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

