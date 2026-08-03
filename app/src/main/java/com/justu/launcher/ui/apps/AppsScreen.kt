package com.justu.launcher.ui.apps

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.utils.AppLauncherInterceptor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val settings by viewModel.homeSettings.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var showAppMenu by remember { mutableStateOf<AppInfo?>(null) }
    val favoritePackages = remember(settings.favoriteApps) { settings.favoriteApps.toSet() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    textStyle = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        apps.firstOrNull()?.let { app ->
                            app.launchIntent?.let { intent ->
                                val isBlocked = settings.blockedApps.contains(app.packageName)
                                AppLauncherInterceptor.launchAppMindfully(
                                    context,
                                    intent,
                                    app.packageName,
                                    isFocusMode = settings.isFocusModeEnabled,
                                    isBlocked = isBlocked,
                                    favoritePackages = favoritePackages
                                )
                            }
                        }
                    }),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { viewModel.updateSearchQuery("") }
                        .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No apps found" else "Loading apps...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 64.dp)
            ) {
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    val isFavorite = settings.favoriteApps.contains(app.packageName)
                    val isBlocked = settings.blockedApps.contains(app.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement(animationSpec = tween(durationMillis = 200))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        app.launchIntent?.let {
                                            AppLauncherInterceptor.launchAppMindfully(
                                                context,
                                                it,
                                                app.packageName,
                                                isFocusMode = settings.isFocusModeEnabled,
                                                isBlocked = isBlocked,
                                                favoritePackages = favoritePackages
                                            )
                                        }
                                    },
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showAppMenu = app
                                    }
                                )
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isBlocked) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onBackground
                        )

                        if (isBlocked) {
                            Text(
                                text = "Blocked",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        } else if (isFavorite) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAppMenu != null) {
        val app = showAppMenu!!
        val isFavorite = settings.favoriteApps.contains(app.packageName)
        
        Dialog(onDismissRequest = { showAppMenu = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111111),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.toggleFavorite(app.packageName)
                            showAppMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites (Home)")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.hideApp(app.packageName)
                            showAppMenu = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hide App")
                    }
                }
            }
        }
    }
}
