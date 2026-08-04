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
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // 5-second enforced pause with visual countdown
    val totalSeconds = 5
    var secondsLeft by remember { mutableStateOf(totalSeconds) }
    var canProceed by remember { mutableStateOf(false) }
    val timerProgress by animateFloatAsState(
        targetValue = secondsLeft.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "timerProgress"
    )

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1017),
                        Color(0xFF070709),
                        Color(0xFF040405)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background aura
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(breathingScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7).copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF132232),
                        border = BorderStroke(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Mindful Pause",
                            color = Color(0xFFBDE6FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Is this intentional?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Take a breath before proceeding.",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }

            // Quote Glass Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF121824).copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"${thought.quote}\"",
                        fontSize = 17.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "— ${thought.author}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF81D4FA).copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Actions & Timer Ring
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Secondary Action: Stay Focused
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Stay Focused (Return Home)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Circular Progress Ring & Timer
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(76.dp)) {
                        val stroke = 5.dp.toPx()
                        // Track
                        drawArc(
                            color = Color.White.copy(alpha = 0.08f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // Progress
                        drawArc(
                            color = if (canProceed) Color(0xFF81D4FA) else Color.White.copy(alpha = 0.35f),
                            startAngle = -90f,
                            sweepAngle = -360f * timerProgress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (canProceed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onProceed()
                            }
                        },
                        enabled = canProceed,
                        modifier = Modifier.size(68.dp)
                    ) {
                        if (canProceed) {
                            Text(
                                text = "Continue",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF81D4FA),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "$secondsLeft",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (canProceed) "Tap to open" else "Pause for $secondsLeft s",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0E10),
                        Color(0xFF09090B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A1416))
                    .border(BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Restricted",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141416).copy(alpha = 0.9f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = description,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReturn()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Return to Home", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
