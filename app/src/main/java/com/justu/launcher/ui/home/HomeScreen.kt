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
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "clockAlpha"
    )

    // Dialogs — shown in order: TC → Onboarding → Default Launcher prompt
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

                Spacer(modifier = Modifier.height(32.dp))

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
    var showFullTerms by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { /* Force action to close */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000), // Force Night Dark
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
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Sectioned indicator like onboarding
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2F6BFF).copy(alpha = 0.4f))
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "JustU Launcher is built with a focus on your digital well-being. It is open-source, respects your privacy, and collects zero personal data.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFA1A4A8),
                            lineHeight = 30.sp,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "To help you stay focused, we require accessibility permissions only to block distracting content like YouTube Shorts on your behalf.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFA1A4A8),
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
                                    checkedColor = Color(0xFF2F6BFF),
                                    uncheckedColor = Color.White.copy(alpha = 0.2f),
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            val annotatedString = buildAnnotatedString {
                                append("I agree to the ")
                                pushStringAnnotation(tag = "terms", annotation = "terms")
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF2F6BFF),
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
                                    color = Color.White.copy(alpha = 0.7f),
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
                                containerColor = Color(0xFF2F6BFF),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF2F6BFF).copy(alpha = 0.2f),
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
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

                        Spacer(modifier = Modifier.height(20.dp)) // Extra spacing at bottom
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
                color = Color(0xFF0A0B0D), // Night Dark Surface
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
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
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Set JustU as your default launcher to ensure every time you unlock your phone, it's with intention.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFA1A4A8),
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
                            containerColor = Color(0xFF2F6BFF),
                            contentColor = Color.White
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
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuidedTourDialog(
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
