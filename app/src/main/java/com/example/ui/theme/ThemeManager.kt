package com.example.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow

object ThemeManager {
    enum class ThemeOption(val label: String) {
        MIDNIGHT("Midnight Slate"),
        FOREST("Forest Canopy"),
        OCEAN("Ocean Depths"),
        SUNSET("Sunset Glow"),
        CANYON("Deep Canyon"),
        RIVER("River Pebbles")
    }
    
    val currentTheme = MutableStateFlow(ThemeOption.MIDNIGHT)
}
