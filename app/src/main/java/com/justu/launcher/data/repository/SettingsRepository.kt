package com.justu.launcher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.justu.launcher.data.model.HomeSettings
import com.justu.launcher.data.model.ThemeMode
import com.justu.launcher.data.model.ThemeSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "justu_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val BOLD_TEXT = booleanPreferencesKey("bold_text")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_BATTERY = booleanPreferencesKey("show_battery")
        
        val HAS_AGREED_TO_TC = booleanPreferencesKey("has_agreed_to_tc")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val ONBOARDING_PAGE = intPreferencesKey("onboarding_page")
        val HAS_SEEN_TOOLTIP = booleanPreferencesKey("has_seen_tooltip")
        val IS_FOCUS_MODE = booleanPreferencesKey("is_focus_mode")
        
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val FAVORITE_APPS = stringPreferencesKey("favorite_apps") // CSV for ordering
        val MAX_FAVORITE_APPS = intPreferencesKey("max_favorite_apps")
        val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
        val FAVORITES_ALIGNMENT = stringPreferencesKey("favorites_alignment")
        val BLOCK_YOUTUBE_SHORTS = booleanPreferencesKey("block_youtube_shorts")
        val EXEMPT_APPS = stringSetPreferencesKey("exempt_apps")
        val HAS_INITIALIZED_EXEMPTIONS = booleanPreferencesKey("has_initialized_exemptions")
        val TIMER_REMINDER_COUNT = intPreferencesKey("timer_reminder_count")
    }

    val themeSettings: Flow<ThemeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val modeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeSettings(
                themeMode = try { ThemeMode.valueOf(modeName) } catch (e: Exception) { ThemeMode.SYSTEM },
                fontScale = preferences[PreferencesKeys.FONT_SCALE] ?: 1.0f,
                isBoldTextEnabled = preferences[PreferencesKeys.BOLD_TEXT] ?: false,
                fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "DEFAULT"
            )
        }

    val homeSettings: Flow<HomeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val favAppsCsv = preferences[PreferencesKeys.FAVORITE_APPS] ?: ""
            HomeSettings(
                showClock = preferences[PreferencesKeys.SHOW_CLOCK] ?: true,
                showDate = preferences[PreferencesKeys.SHOW_DATE] ?: true,
                showBattery = preferences[PreferencesKeys.SHOW_BATTERY] ?: true,
                hasAgreedToTC = preferences[PreferencesKeys.HAS_AGREED_TO_TC] ?: false,
                hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
                onboardingPage = preferences[PreferencesKeys.ONBOARDING_PAGE] ?: 0,
                hasSeenHomescreenTooltip = preferences[PreferencesKeys.HAS_SEEN_TOOLTIP] ?: false,
                isFocusModeEnabled = preferences[PreferencesKeys.IS_FOCUS_MODE] ?: false,
                hiddenApps = preferences[PreferencesKeys.HIDDEN_APPS] ?: emptySet(),
                favoriteApps = if (favAppsCsv.isEmpty()) emptyList() else favAppsCsv.split(","),
                maxFavoriteApps = preferences[PreferencesKeys.MAX_FAVORITE_APPS] ?: 5,
                blockedApps = preferences[PreferencesKeys.BLOCKED_APPS] ?: emptySet(),
                favoritesAlignment = preferences[PreferencesKeys.FAVORITES_ALIGNMENT] ?: "CENTER",
                blockYoutubeShorts = preferences[PreferencesKeys.BLOCK_YOUTUBE_SHORTS] ?: false,
                exemptApps = preferences[PreferencesKeys.EXEMPT_APPS] ?: emptySet(),
                hasInitializedExemptions = preferences[PreferencesKeys.HAS_INITIALIZED_EXEMPTIONS] ?: false,
                timerReminderCount = preferences[PreferencesKeys.TIMER_REMINDER_COUNT] ?: 0
            )
        }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updateFontScale(scale: Float) {
        dataStore.edit { it[PreferencesKeys.FONT_SCALE] = scale }
    }

    suspend fun updateBoldText(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BOLD_TEXT] = enabled }
    }

    suspend fun updateFontFamily(fontFamily: String) {
        dataStore.edit { it[PreferencesKeys.FONT_FAMILY] = fontFamily }
    }

    suspend fun updateHomeElementVisibility(
        showClock: Boolean? = null,
        showDate: Boolean? = null,
        showBattery: Boolean? = null
    ) {
        dataStore.edit { prefs ->
            showClock?.let { prefs[PreferencesKeys.SHOW_CLOCK] = it }
            showDate?.let { prefs[PreferencesKeys.SHOW_DATE] = it }
            showBattery?.let { prefs[PreferencesKeys.SHOW_BATTERY] = it }
        }
    }

    suspend fun agreeToTerms() {
        dataStore.edit { it[PreferencesKeys.HAS_AGREED_TO_TC] = true }
    }

    suspend fun completeOnboarding() {
        dataStore.edit { it[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true }
    }

    suspend fun updateOnboardingPage(page: Int) {
        dataStore.edit { it[PreferencesKeys.ONBOARDING_PAGE] = page }
    }

    suspend fun markTooltipSeen() {
        dataStore.edit { it[PreferencesKeys.HAS_SEEN_TOOLTIP] = true }
    }

    suspend fun toggleFocusMode(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_FOCUS_MODE] = enabled }
    }

    suspend fun updateHiddenApps(hiddenApps: Set<String>) {
        dataStore.edit { it[PreferencesKeys.HIDDEN_APPS] = hiddenApps }
    }

    suspend fun updateFavoriteApps(favoriteApps: List<String>) {
        dataStore.edit { it[PreferencesKeys.FAVORITE_APPS] = favoriteApps.joinToString(",") }
    }

    suspend fun updateMaxFavoriteApps(max: Int) {
        dataStore.edit { it[PreferencesKeys.MAX_FAVORITE_APPS] = max }
    }

    suspend fun updateBlockedApps(blockedApps: Set<String>) {
        dataStore.edit { it[PreferencesKeys.BLOCKED_APPS] = blockedApps }
    }

    suspend fun updateFavoritesAlignment(alignment: String) {
        dataStore.edit { it[PreferencesKeys.FAVORITES_ALIGNMENT] = alignment }
    }

    suspend fun removeFavoriteApp(packageName: String) {
        dataStore.edit { prefs ->
            val csv = prefs[PreferencesKeys.FAVORITE_APPS] ?: ""
            val list = if (csv.isEmpty()) emptyList() else csv.split(",")
            prefs[PreferencesKeys.FAVORITE_APPS] = list.filter { it != packageName }.joinToString(",")
        }
    }

    suspend fun replaceFavoriteApp(oldPackage: String, newPackage: String) {
        dataStore.edit { prefs ->
            val csv = prefs[PreferencesKeys.FAVORITE_APPS] ?: ""
            val list = if (csv.isEmpty()) mutableListOf() else csv.split(",").toMutableList()
            val idx = list.indexOf(oldPackage)
            if (idx >= 0) list[idx] = newPackage else list.add(newPackage)
            prefs[PreferencesKeys.FAVORITE_APPS] = list.joinToString(",")
        }
    }

    suspend fun addFavoriteApp(packageName: String) {
        dataStore.edit { prefs ->
            val csv = prefs[PreferencesKeys.FAVORITE_APPS] ?: ""
            val list = if (csv.isEmpty()) mutableListOf() else csv.split(",").toMutableList()
            if (!list.contains(packageName)) list.add(packageName)
            prefs[PreferencesKeys.FAVORITE_APPS] = list.joinToString(",")
        }
    }

    suspend fun toggleBlockYoutubeShorts(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BLOCK_YOUTUBE_SHORTS] = enabled }
    }

    suspend fun updateExemptApps(exemptApps: Set<String>) {
        dataStore.edit { it[PreferencesKeys.EXEMPT_APPS] = exemptApps }
    }

    suspend fun markExemptionsInitialized() {
        dataStore.edit { it[PreferencesKeys.HAS_INITIALIZED_EXEMPTIONS] = true }
    }

    suspend fun updateTimerReminderCount(count: Int) {
        dataStore.edit { it[PreferencesKeys.TIMER_REMINDER_COUNT] = count }
    }
}
