package com.justu.launcher.data.model

data class HomeSettings(
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val hasAgreedToTC: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val onboardingPage: Int = 0,
    val hasSeenHomescreenTooltip: Boolean = false,
    val isFocusModeEnabled: Boolean = false,
    val hiddenApps: Set<String> = emptySet(),
    val favoriteApps: List<String> = emptyList(), // List of package names
    val maxFavoriteApps: Int = 5,
    val blockedApps: Set<String> = emptySet(),
    val favoritesAlignment: String = "CENTER",
    val blockYoutubeShorts: Boolean = false,
    val exemptApps: Set<String> = emptySet()  // Apps that skip the 5s mindful timer
)
