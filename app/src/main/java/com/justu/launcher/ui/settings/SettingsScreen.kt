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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.data.model.ThemeMode

enum class SettingsSubScreen {
    MAIN, TERMS, ABOUT, BLOCKING, FAVORITES, EXEMPT
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
        SettingsSubScreen.MAIN -> {
            val scrollState = rememberScrollState()

            var showAlignmentMenu by remember { mutableStateOf(false) }
            var showThemeMenu by remember { mutableStateOf(false) }
            var showFontMenu by remember { mutableStateOf(false) }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Customize your distraction-free launcher.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // HOME SCREEN DISPLAY SETTINGS
                SettingsCard(title = "Home Display") {
                    SettingsSwitchRow("Show Clock", homeSettings.showClock) {
                        viewModel.updateHomeElementVisibility(showClock = it)
                    }
                    SettingsSwitchRow("Show Date", homeSettings.showDate) {
                        viewModel.updateHomeElementVisibility(showDate = it)
                    }
                    SettingsSwitchRow("Show Battery", homeSettings.showBattery) {
                        viewModel.updateHomeElementVisibility(showBattery = it)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FAVORITE APPS CUSTOMIZATION
                SettingsCard(title = "Favorite Apps Customization") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Max Favorites Count", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Display 1 to 10 apps on home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Text("${homeSettings.maxFavoriteApps}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Slider(
                        value = homeSettings.maxFavoriteApps.toFloat(),
                        onValueChange = { viewModel.updateMaxFavoriteApps(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                    // Favorites Placement Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Favorites Placement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Where to position apps on home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Box {
                            Text(
                                text = when (homeSettings.favoritesAlignment) {
                                    "TOP" -> "Top"
                                    "BOTTOM" -> "Bottom"
                                    else -> "Center"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.clickable { showAlignmentMenu = true }
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
                                        },
                                        trailingIcon = {
                                            if (homeSettings.favoritesAlignment == value) {
                                                Text("✓", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FAVORITE & EXEMPT APP PICKERS
                SettingsCard(title = "App Shortcuts") {
                    SettingsNavigationRow(
                        title = "Choose Favorite Apps",
                        subtitle = "${homeSettings.favoriteApps.size} of ${homeSettings.maxFavoriteApps} pinned to home",
                        onClick = { currentSubScreen = SettingsSubScreen.FAVORITES }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsNavigationRow(
                        title = "Timer-Exempt Apps",
                        subtitle = "${homeSettings.exemptApps.size} apps skip the 5s mindful timer",
                        onClick = { currentSubScreen = SettingsSubScreen.EXEMPT }
                    )
                    Text(
                        text = "⚡ Exempt apps always open instantly. Long-press any app in the drawer to toggle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // THEME & FONTS
                SettingsCard(title = "Theme & Typography") {
                    // Theme Mode Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("App color scheme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Box {
                            Text(
                                text = when (themeSettings.themeMode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.PURE_BLACK -> "Pure Black"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.clickable { showThemeMenu = true }
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
                                        },
                                        trailingIcon = {
                                            if (themeSettings.themeMode == mode) {
                                                Text("✓", color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Font Size Slider
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Font Size", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Scale text across the launcher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Text(
                                text = "${(themeSettings.fontScale * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = themeSettings.fontScale,
                            onValueChange = { viewModel.updateFontScale(it) },
                            valueRange = 0.7f..1.5f,
                            steps = 7
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DIGITAL WELLBEING & BLOCKING
                SettingsCard(title = "Digital Wellbeing & Focus") {
                    SettingsNavigationRow(
                        title = "Manage Blocked Apps",
                        subtitle = "${homeSettings.blockedApps.size} apps blocked",
                        onClick = { currentSubScreen = SettingsSubScreen.BLOCKING }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    // YouTube Shorts Blocker Toggle
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        SettingsSwitchRow(
                            title = "Block YouTube Shorts",
                            checked = homeSettings.blockYoutubeShorts,
                            onCheckedChange = { enabled ->
                                viewModel.toggleBlockYoutubeShorts(enabled)
                                if (enabled) {
                                    // Prompt user to enable Accessibility Service if not active
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        )
                        Text(
                            text = "Uses Accessibility Service to redirect you out of the YouTube Shorts feed automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SYSTEM & DEFAULTS
                SettingsCard(title = "System Defaults") {
                    SettingsNavigationRow(
                        title = "Default Home Launcher",
                        subtitle = "Select JustU Launcher as system home app",
                        onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LEGAL & INFORMATION
                SettingsCard(title = "Legal & Info") {
                    SettingsNavigationRow(
                        title = "Terms & Privacy Policy",
                        subtitle = "Read data protection statement",
                        onClick = { currentSubScreen = SettingsSubScreen.TERMS }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingsNavigationRow(
                        title = "About & Open Source",
                        subtitle = "GitHub repo, share app, developer bio",
                        onClick = { currentSubScreen = SettingsSubScreen.ABOUT }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Back to Home")
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text(text = "→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}
