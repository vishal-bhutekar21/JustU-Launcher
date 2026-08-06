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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.justu.launcher.SettingsActivity
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.ui.onboarding.OnboardingDialog
import com.justu.launcher.ui.settings.TermsAndConditionsScreen
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
    val favApps by viewModel.favoriteApps.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDefaultLauncher by remember {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        mutableStateOf(resolveInfo?.activityInfo?.packageName == context.packageName)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                isDefaultLauncher = resolveInfo?.activityInfo?.packageName == context.packageName
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "clockBreathing")
    val clockAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clockAlpha"
    )

    // Dialogs
    if (!settings.hasAgreedToTC) {
        TermsAndConditionsDialog(onAgree = { viewModel.agreeToTerms() })
    } else if (!settings.hasCompletedOnboarding) {
        OnboardingDialog(
            initialPage = settings.onboardingPage,
            onPageChange = { viewModel.updateOnboardingPage(it) },
            onComplete = { viewModel.completeOnboarding() },
            context = context
        )
    } else if (!isDefaultLauncher) {
        DefaultLauncherDialog(context = context)
    } else if (settings.timerReminderCount < 5) {
        DisturbingAppsReminderDialog(
            onRemindMeLater = { viewModel.dismissTimerReminder(false) },
            onNeverShowAgain = { viewModel.dismissTimerReminder(true) }
        )
    }

    val viewConfiguration = LocalViewConfiguration.current
    val longPressMs = viewConfiguration.longPressTimeoutMillis

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        // Settings Button
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(100.dp))

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
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (settings.showBattery && battery.isNotEmpty()) {
                Text(
                    text = "Battery $battery",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    letterSpacing = 2.sp
                )
            }

            val verticalArrangement = when (settings.favoritesAlignment) {
                "TOP" -> Arrangement.Top
                "BOTTOM" -> Arrangement.Bottom
                else -> Arrangement.Center
            }

            Spacer(modifier = Modifier.height(48.dp))

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
                        haptic = haptic,
                        context = context,
                        viewModel = viewModel
                    )
                }
            }

            // Bottom Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(dialIntent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFocusMode(!settings.isFocusModeEnabled)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (settings.isFocusModeEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (settings.isFocusModeEnabled) "FOCUS ACTIVE" else "MINDFUL",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (settings.isFocusModeEnabled) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        letterSpacing = 3.sp
                    )
                }

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        context.startActivity(camIntent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Camera",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
            }
        }

        if (!settings.hasSeenHomescreenTooltip && settings.hasCompletedOnboarding && isDefaultLauncher) {
            GuidedTourDialog(onDismiss = { viewModel.markTooltipSeen() })
        }
    }
}

@Composable
fun DisturbingAppsReminderDialog(
    onRemindMeLater: () -> Unit,
    onNeverShowAgain: () -> Unit
) {
    Dialog(
        onDismissRequest = { onRemindMeLater() }
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium Icon Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterTiltShift,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Intentional Growth",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "To help you stay focused, we've disabled the mindful timer for all apps initially.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "We recommend adding 'disturbing' apps (like social media) back to the timer to reclaim your attention.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onRemindMeLater,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("Remind me later", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onNeverShowAgain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "I've got it, don't show again",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
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
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: Context,
    viewModel: HomeViewModel
) {
    val favoritePackages = remember(settings.favoriteApps) { settings.favoriteApps.toSet() }
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
                .padding(vertical = 16.dp)
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

@Composable
fun TermsAndConditionsDialog(onAgree: () -> Unit) {
    var showFullTerms by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (showFullTerms) {
                TermsAndConditionsScreen(onBack = { showFullTerms = false })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 64.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Privacy &\nTransparency",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Sectioned indicator like onboarding
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "JustU Launcher is built with a focus on your digital well-being. It is open-source, respects your privacy, and collects zero personal data.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            lineHeight = 30.sp,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "To help you stay focused, we require accessibility permissions only to block distracting content like YouTube Shorts on your behalf.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            lineHeight = 30.sp,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isChecked = !isChecked }
                                .padding(vertical = 12.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            val annotatedString = buildAnnotatedString {
                                append("I agree to the ")
                                pushStringAnnotation(tag = "terms", annotation = "terms")
                                withStyle(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ) {
                                    append("Terms & Privacy Policy")
                                }
                                pop()
                            }
                            androidx.compose.foundation.text.ClickableText(
                                text = annotatedString,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                ),
                                onClick = { offset ->
                                    annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset)
                                        .firstOrNull()?.let {
                                            showFullTerms = true
                                        }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onAgree,
                            enabled = isChecked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Text(
                                "Agree & Continue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultLauncherDialog(context: Context) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Intentional Home",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Set JustU as your default launcher to ensure every time you unlock your phone, it's with intention.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    ) {
                        Text("Set as Default", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showDialog = false }) {
                        Text(
                            text = "Maybe Later",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuidedTourDialog(onDismiss: () -> Unit) {
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
                        icon = Icons.Rounded.Visibility,
                        title = "Mindful launch",
                        body = "Open apps through the launcher so you get a pause before distracting launches."
                    )
                    GuidedTourItem(
                        icon = Icons.Rounded.Settings,
                        title = "Settings & setup",
                        body = "Use Settings to pick your favorite apps and tune the launcher layout to your liking."
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Got it", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Pro Tip: Swipe up from home to search Google. Long-press anywhere to open settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
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
