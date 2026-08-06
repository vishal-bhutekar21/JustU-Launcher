package com.justu.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.data.model.ThemeMode

enum class SettingsSubScreen {
    MAIN, TERMS, ABOUT, BLOCKING, FAVORITES, EXEMPT, HIDDEN
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    val homeSettings by viewModel.homeSettings.collectAsState()
    val themeSettings by viewModel.themeSettings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val context = LocalContext.current

    BackHandler {
        if (currentSubScreen != SettingsSubScreen.MAIN) {
            currentSubScreen = SettingsSubScreen.MAIN
        } else {
            onBack()
        }
    }

    when (currentSubScreen) {
        SettingsSubScreen.TERMS -> {
            TermsAndConditionsScreen(onBack = { currentSubScreen = SettingsSubScreen.MAIN })
        }
        SettingsSubScreen.ABOUT -> {
            AboutScreen(onBack = { currentSubScreen = SettingsSubScreen.MAIN })
        }
        SettingsSubScreen.BLOCKING -> {
            AppBlockingScreen(
                allApps = installedApps,
                blockedApps = homeSettings.blockedApps,
                onToggleBlock = { viewModel.toggleBlockApp(it) },
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.FAVORITES -> {
            FavoriteAppsPickerScreen(
                allApps = installedApps,
                favoriteApps = homeSettings.favoriteApps,
                maxFavorites = homeSettings.maxFavoriteApps,
                onToggleFavorite = { viewModel.toggleFavoriteApp(it) },
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.EXEMPT -> {
            ExemptAppsScreen(
                allApps = installedApps,
                exemptApps = homeSettings.exemptApps,
                onToggleExempt = { viewModel.toggleExemptApp(it) },
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.HIDDEN -> {
            HiddenAppsScreen(
                allApps = installedApps,
                hiddenApps = homeSettings.hiddenApps,
                onToggleHidden = { viewModel.toggleHiddenApp(it) },
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.MAIN -> {
            val scrollState = rememberScrollState()

            var showAlignmentMenu by remember { mutableStateOf(false) }
            var showThemeMenu by remember { mutableStateOf(false) }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Customize your JustU launcher.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // HOME SCREEN DISPLAY SETTINGS
                ZenSettingsCard(title = "Display") {
                    ZenSettingsSwitchRow("Show Clock", homeSettings.showClock) {
                        viewModel.updateHomeElementVisibility(showClock = it)
                    }
                    ZenSettingsSwitchRow("Show Date", homeSettings.showDate) {
                        viewModel.updateHomeElementVisibility(showDate = it)
                    }
                    ZenSettingsSwitchRow("Show Battery", homeSettings.showBattery) {
                        viewModel.updateHomeElementVisibility(showBattery = it)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // FAVORITE APPS CUSTOMIZATION
                ZenSettingsCard(title = "Favorites") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Max Apps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("1 to 10 apps on home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        Text("${homeSettings.maxFavoriteApps}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = homeSettings.maxFavoriteApps.toFloat(),
                        onValueChange = { viewModel.updateMaxFavoriteApps(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Favorites Placement Dropdown
                    ZenSettingsNavigationRow(
                        title = "Placement",
                        subtitle = when (homeSettings.favoritesAlignment) {
                            "TOP" -> "Top"
                            "BOTTOM" -> "Bottom"
                            else -> "Center"
                        },
                        onClick = { showAlignmentMenu = true }
                    )
                    DropdownMenu(
                        expanded = showAlignmentMenu,
                        onDismissRequest = { showAlignmentMenu = false }
                    ) {
                        listOf("TOP" to "Top", "CENTER" to "Center", "BOTTOM" to "Bottom").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateFavoritesAlignment(value)
                                    showAlignmentMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // APP MANAGEMENT
                ZenSettingsCard(title = "Apps") {
                    ZenSettingsNavigationRow(
                        title = "Favorite Apps",
                        subtitle = "${homeSettings.favoriteApps.size} of ${homeSettings.maxFavoriteApps} pinned",
                        onClick = { currentSubScreen = SettingsSubScreen.FAVORITES }
                    )
                    ZenSettingsNavigationRow(
                        title = "Timer-Exempt Apps",
                        subtitle = "${homeSettings.exemptApps.size} apps skip the 5s timer",
                        onClick = { currentSubScreen = SettingsSubScreen.EXEMPT }
                    )
                    ZenSettingsNavigationRow(
                        title = "Hidden Apps",
                        subtitle = "${homeSettings.hiddenApps.size} apps hidden from drawer",
                        onClick = { currentSubScreen = SettingsSubScreen.HIDDEN }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // THEME
                ZenSettingsCard(title = "Appearance") {
                    ZenSettingsNavigationRow(
                        title = "Theme",
                        subtitle = when (themeSettings.themeMode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.PURE_BLACK -> "Pure Black"
                        },
                        onClick = { showThemeMenu = true }
                    )
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "System Default",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.PURE_BLACK to "Pure Black"
                        ).forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateThemeMode(mode)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Scale", style = MaterialTheme.typography.titleMedium)
                            Text("${(themeSettings.fontScale * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = themeSettings.fontScale,
                            onValueChange = { viewModel.updateFontScale(it) },
                            valueRange = 0.7f..1.5f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // WELLBEING
                ZenSettingsCard(title = "Wellbeing") {
                    ZenSettingsNavigationRow(
                        title = "Blocked Apps",
                        subtitle = "${homeSettings.blockedApps.size} apps restricted",
                        onClick = { currentSubScreen = SettingsSubScreen.BLOCKING }
                    )
                    ZenSettingsSwitchRow(
                        title = "Block YouTube Shorts",
                        checked = homeSettings.blockYoutubeShorts,
                        onCheckedChange = { enabled ->
                            viewModel.toggleBlockYoutubeShorts(enabled)
                            if (enabled) {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SYSTEM
                ZenSettingsCard(title = "System") {
                    ZenSettingsNavigationRow(
                        title = "Default Launcher",
                        subtitle = "Select JustU as home app",
                        onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    ZenSettingsNavigationRow(
                        title = "Terms & Privacy",
                        subtitle = "Read data statement",
                        onClick = { currentSubScreen = SettingsSubScreen.TERMS }
                    )
                    ZenSettingsNavigationRow(
                        title = "About JustU Launcher",
                        subtitle = "Open source & dev info",
                        onClick = { currentSubScreen = SettingsSubScreen.ABOUT },
                        isHighlighted = true
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("Back to Home", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun ZenSettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            letterSpacing = 2.sp
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), content = content)
        }
    }
}

@Composable
fun ZenSettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun ZenSettingsNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = if (isHighlighted) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
        Text(
            text = "→", 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }
}
