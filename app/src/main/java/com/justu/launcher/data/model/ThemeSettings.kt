package com.justu.launcher.data.model

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.PURE_BLACK,
    val fontScale: Float = 1.0f,
    val isBoldTextEnabled: Boolean = false,
    val fontFamily: String = "DEFAULT"
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM, PURE_BLACK
}
