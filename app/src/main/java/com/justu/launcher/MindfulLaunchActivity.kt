package com.justu.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justu.launcher.data.repository.MindfulThought
import com.justu.launcher.data.repository.MindfulThoughtsRepository
import com.justu.launcher.ui.theme.JustUTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MindfulLaunchActivity : ComponentActivity() {

    @Inject
    lateinit var thoughtsRepository: MindfulThoughtsRepository

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetIntent = intent.getParcelableExtra<Intent>("EXTRA_TARGET_INTENT")
        val isFocusMode = intent.getBooleanExtra("EXTRA_IS_FOCUS_MODE", false)
        val isBlocked = intent.getBooleanExtra("EXTRA_IS_BLOCKED", false)
        val thought = thoughtsRepository.getRandomThought()

        setContent {
            JustUTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF070709)
                ) {
                    when {
                        isBlocked -> {
                            BlockedScreen(
                                title = "App Blocked",
                                description = "You chose to block this app to protect your digital wellbeing and focus.",
                                onReturn = { finish() }
                            )
                        }
                        isFocusMode -> {
                            BlockedScreen(
                                title = "Focus Mode Active",
                                description = "Focus Mode is currently enabled. Non-essential apps are paused.",
                                onReturn = { finish() }
                            )
                        }
                        else -> {
                            MindfulScreen(
                                thought = thought,
                                onProceed = {
                                    targetIntent?.let { startActivity(it) }
                                    finish()
                                },
                                onCancel = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindfulScreen(
    thought: MindfulThought,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 5-second enforced pause
    val totalSeconds = 5
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var canProceed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
        canProceed = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08080A)),
        contentAlignment = Alignment.Center
    ) {
        var showInfoDialog by remember { mutableStateOf(false) }

        // Info Icon Top Right
        IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.2f)
            )
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Mindful Launch") },
                text = {
                    Text("This 5-second pause helps you stay intentional. To remove this timer for essential apps (like Phone or Maps), long-press the app in the drawer and select 'Enable Instant Launch'.")
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Got it")
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = Color(0xFF161618),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.7f)
            )
        }
        // Ambient background aura
        Box(
            modifier = Modifier
                .size(450.dp)
                .scale(breathingScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7).copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MINDFULNESS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 6.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Do you really need this?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Take a breath. Is this action serving you?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

            // Quote Section - Floating & Elegant
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\"${thought.quote}\"",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        lineHeight = 34.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "— ${thought.author}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF81D4FA).copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Actions Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Action: Stay Focused
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "I'll stay focused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Action: Proceed (after delay)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (canProceed) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onProceed()
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = "Open anyway",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { (totalSeconds - secondsLeft).toFloat() / totalSeconds.toFloat() },
                                modifier = Modifier.size(18.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                strokeWidth = 2.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Waiting $secondsLeft s...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedScreen(
    title: String,
    description: String,
    onReturn: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0506)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF211012)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Restricted",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onReturn()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("Return to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MindfulScreenPreview() {
    JustUTheme {
        MindfulScreen(
            thought = MindfulThought(
                quote = "We suffer more often in imagination than in reality.",
                author = "Seneca"
            ),
            onProceed = {},
            onCancel = {}
        )
    }
}
