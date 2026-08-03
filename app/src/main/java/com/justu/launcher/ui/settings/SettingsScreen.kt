package com.justu.launcher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    SettingsSwitchRow("Show Greeting", homeSettings.showGreeting) {
                        viewModel.updateHomeElementVisibility(showGreeting = it)
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
                            Text("Max Favorites Count", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Display 1 to 10 apps on home", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        }
                        Text("${homeSettings.maxFavoriteApps}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Slider(
                        value = homeSettings.maxFavoriteApps.toFloat(),
                        onValueChange = { viewModel.updateMaxFavoriteApps(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Favorites Placement Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Favorites Placement", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Where to position apps on home", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        }
                        Box {
                            Text(
                                text = when (homeSettings.favoritesAlignment) {
                                    "TOP" -> "Top"
                                    "BOTTOM" -> "Bottom"
                                    else -> "Center"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.6f),
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

                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingsNavigationRow(
                        title = "Timer-Exempt Apps",
                        subtitle = "${homeSettings.exemptApps.size} apps skip the 5s mindful timer",
                        onClick = { currentSubScreen = SettingsSubScreen.EXEMPT }
                    )
                    Text(
                        text = "⚡ Exempt apps always open instantly. Long-press any app in the drawer to toggle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f),
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
                            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("App color scheme", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
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
                                color = Color.White.copy(alpha = 0.6f),
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

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Font Family Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Font Family", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Text style across the app", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        }
                        Box {
                            Text(
                                text = when (themeSettings.fontFamily.uppercase()) {
                                    "MONOSPACE" -> "Monospace"
                                    "SERIF" -> "Serif"
                                    "SANS_SERIF" -> "Sans-Serif"
                                    else -> "Default"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { showFontMenu = true }
                            )
                            DropdownMenu(
                                expanded = showFontMenu,
                                onDismissRequest = { showFontMenu = false }
                            ) {
                                listOf(
                                    "DEFAULT" to "Default",
                                    "SERIF" to "Serif",
                                    "MONOSPACE" to "Monospace",
                                    "SANS_SERIF" to "Sans-Serif"
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.updateFontFamily(value)
                                            showFontMenu = false
                                        },
                                        trailingIcon = {
                                            if (themeSettings.fontFamily.uppercase() == value) {
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

                // DIGITAL WELLBEING & BLOCKING
                SettingsCard(title = "Digital Wellbeing & Focus") {
                    SettingsNavigationRow(
                        title = "Manage Blocked Apps",
                        subtitle = "${homeSettings.blockedApps.size} apps blocked",
                        onClick = { currentSubScreen = SettingsSubScreen.BLOCKING }
                    )

                    Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

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
                            color = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SYSTEM & DEFAULTS
                SettingsCard(title = "System Defaults") {
                    SettingsNavigationRow(
                        title = "Default Home Launcher",
                        subtitle = "Select JustU as system home app",
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

                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    SettingsNavigationRow(
                        title = "About & Open Source",
                        subtitle = "GitHub repo, share app, developer bio",
                        onClick = { currentSubScreen = SettingsSubScreen.ABOUT }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
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
            color = Color(0xFF111111),
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
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
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
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }
        Text(text = "→", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.5f))
    }
}
