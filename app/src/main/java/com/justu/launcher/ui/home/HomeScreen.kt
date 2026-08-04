package com.justu.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.SettingsActivity
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.ui.onboarding.OnboardingDialog
import com.justu.launcher.utils.AppLauncherInterceptor
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val settings by viewModel.homeSettings.collectAsState()
    val time by viewModel.currentTime.collectAsState()
    val date by viewModel.currentDate.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    val usage by viewModel.todayUsage.collectAsState()
    val favApps by viewModel.favoriteApps.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "clockBreathing")
    val clockAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clockAlpha"
    )

    val isDefaultLauncher = remember {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    }

    // Dialogs — shown in order: TC → Onboarding → Default Launcher prompt
    if (!settings.hasAgreedToTC) {
        TermsAndConditionsDialog(onAgree = { viewModel.agreeToTerms() })
    } else if (!settings.hasCompletedOnboarding) {
        OnboardingDialog(
            onComplete = { viewModel.completeOnboarding() },
            context = context
        )
    } else if (!isDefaultLauncher) {
        DefaultLauncherDialog(context = context)
    }

    val favoritePackages = remember(settings.favoriteApps) { settings.favoriteApps.toSet() }
    val viewConfiguration = LocalViewConfiguration.current
    val longPressMs = viewConfiguration.longPressTimeoutMillis

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
                // Initial pass: we see the pointer event before children consume it.
                // We only act on long-press; short taps propagate to children normally.
                .pointerInput(longPressMs) {
                    awaitEachGesture {
                        awaitFirstDown(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        var longPressed = false
                        do {
                            val event = withTimeoutOrNull(longPressMs) {
                                awaitPointerEvent(pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            }
                            if (event == null) {
                                longPressed = true
                                break
                            }
                            if (event.changes.all { !it.pressed }) break
                        } while (true)

                        if (longPressed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.markTooltipSeen()
                            val intent = Intent(context, SettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    }
                }
    ) {
            // Top-right Settings Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(96.dp))

                if (settings.showClock) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.alpha(clockAlpha)
                    )
                }

                if (settings.showDate) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (settings.showBattery && battery.isNotEmpty()) {
                    Text(
                        text = "Battery: $battery",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val verticalArrangement = when (settings.favoritesAlignment) {
                    "TOP" -> Arrangement.Top
                    "BOTTOM" -> Arrangement.Bottom
                    else -> Arrangement.Center
                }

                if (settings.showGreeting) {
                    Text(
                        text = getGreeting(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Screen Time: $usage",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = verticalArrangement
                ) {
                    items(favApps) { app ->
                        FavoriteAppItem(
                            app = app,
                            settings = settings,
                            favoritePackages = favoritePackages,
                            haptic = haptic,
                            context = context,
                            viewModel = viewModel
                        )
                    }
                }

                // Focus Mode toggle + Phone/Camera row at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Phone shortcut (bottom-left)
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(dialIntent)
                            }
                    )

                    // Focus Mode label (center)
                    Text(
                        text = if (settings.isFocusModeEnabled) "• Focus Mode On" else "Focus Mode Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.isFocusModeEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFocusMode(!settings.isFocusModeEnabled)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    // Camera shortcut (bottom-right)
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Camera",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(camIntent)
                            }
                    )
                }
            }

            if (!settings.hasSeenHomescreenTooltip && settings.hasCompletedOnboarding && isDefaultLauncher) {
                GuidedTourDialog(
                    onOpenSettings = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    onDismiss = { viewModel.markTooltipSeen() }
                )
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteAppItem(
    app: AppInfo,
    settings: com.justu.launcher.data.model.HomeSettings,
    favoritePackages: Set<String>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context,
    viewModel: HomeViewModel
) {
    val exemptPackages = settings.exemptApps
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        app.launchIntent?.let { intent ->
                            val isBlocked = settings.blockedApps.contains(app.packageName)
                            AppLauncherInterceptor.launchAppMindfully(
                                context,
                                intent,
                                app.packageName,
                                isFocusMode = settings.isFocusModeEnabled,
                                isBlocked = isBlocked,
                                favoritePackages = favoritePackages,
                                exemptPackages = exemptPackages
                            )
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .padding(vertical = 12.dp)
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp)
        ) {
            DropdownMenuItem(
                text = { Text("Remove from Favorites") },
                onClick = {
                    showMenu = false
                    viewModel.removeFavoriteApp(app.packageName)
                }
            )
        }
    }
}



private fun getGreeting(): String {
    val c = Calendar.getInstance()
    return when (c.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        in 16..20 -> "Good Evening"
        else -> "Good Night"
    }
}

@Composable
fun TermsAndConditionsDialog(onAgree: () -> Unit) {
    Dialog(
        onDismissRequest = { /* Force action to close */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = "Terms & Conditions",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "By using JustU Launcher – Digital Detox & Focus, you agree to our privacy policy and terms of service. This app is open-source, respects your digital wellbeing, and collects zero personal data. We require accessibility permissions solely to help block distracting feeds like YouTube Shorts on your behalf.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start,
                        lineHeight = 28.sp
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAgree,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Agree & Continue", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun DefaultLauncherDialog(context: Context) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set as Default",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To get the best experience and prevent the system from closing the app, please set JustU Launcher as your default launcher.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Not Now",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { showDialog = false }.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GuidedTourDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Quick Tour", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "A few gestures and shortcuts will help you use JustU faster.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GuidedTourItem(
                        icon = Icons.Rounded.Home,
                        title = "Home lane",
                        body = "This is your clean launcher home with the clock, favorites, and focus controls."
                    )
                    GuidedTourItem(
                        icon = Icons.Rounded.Analytics,
                        title = "Reality Check",
                        body = "Swipe right to review today vs yesterday usage, top apps, and your screen-time patterns."
                    )
                    GuidedTourItem(
                        icon = Icons.Rounded.Visibility,
                        title = "Mindful launch",
                        body = "Open apps through the launcher so you get a pause before distracting launches."
                    )
                    GuidedTourItem(
                        icon = Icons.Rounded.Settings,
                        title = "Settings & setup",
                        body = "Use Settings to enable usage access, pick favorites, and tune the launcher layout."
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Open Settings")
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Got it")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            onOpenSettings()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Open launcher settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedTourItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            }
        }
    }
}
