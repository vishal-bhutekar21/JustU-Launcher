package com.justu.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
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
    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
    val componentName = android.content.ComponentName(context, com.justu.launcher.receiver.LauncherDeviceAdminReceiver::class.java)

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

    Box(modifier = modifier.fillMaxSize()) {
        // Background layer to handle empty space gestures without stealing from children
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (devicePolicyManager.isAdminActive(componentName)) {
                                devicePolicyManager.lockNow()
                            } else {
                                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allow JustU Launcher to lock the screen on double tap.")
                                }
                                context.startActivity(intent)
                            }
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.markTooltipSeen()
                            val intent = Intent(context, Class.forName("com.justu.launcher.SettingsActivity"))
                            context.startActivity(intent)
                        }
                    )
                }
        )

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

        // Tooltip Overlay for First-time users
        if (!settings.hasSeenHomescreenTooltip && settings.hasCompletedOnboarding) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                viewModel.markTooltipSeen()
                                val intent = Intent(context, Class.forName("com.justu.launcher.SettingsActivity"))
                                context.startActivity(intent)
                            },
                            onTap = { viewModel.markTooltipSeen() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome to JustU", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Long-press anywhere on the background to open settings.", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.markTooltipSeen() }) {
                            Text("Got it")
                        }
                    }
                }
            }
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
                                favoritePackages = favoritePackages
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
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "By using JustU Launcher, you agree to our privacy policy and terms of service. This app is open-source, respects your digital wellbeing, and collects zero personal data. We require accessibility permissions solely to help block distracting feeds like YouTube Shorts on your behalf.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Start,
                        lineHeight = 28.sp
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onAgree,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
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
                color = Color(0xFF111111),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set as Default",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To get the best experience and prevent the system from closing the app, please set JustU as your default launcher.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Settings")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Not Now",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { showDialog = false }.padding(8.dp)
                    )
                }
            }
        }
    }
}
