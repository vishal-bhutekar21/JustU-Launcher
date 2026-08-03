package com.justu.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
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
                    color = Color(0xFF000000)
                ) {
                    when {
                        isBlocked -> {
                            BlockedScreen(
                                title = "App Blocked",
                                description = "You have blocked this app to reclaim your time.",
                                onReturn = { finish() }
                            )
                        }
                        isFocusMode -> {
                            BlockedScreen(
                                title = "Focus Mode Active",
                                description = "Focus Mode is currently enabled. Non-essential apps are restricted.",
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
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Is this necessary?",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(alpha),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Take a moment to breathe.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            // Quote Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"${thought.quote}\"",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "— ${thought.author}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Stay Focused (Return)", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Countdown Timer Ring + Proceed Button
                Box(contentAlignment = Alignment.Center) {
                    // Circular track
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(80.dp)) {
                        val stroke = 6.dp.toPx()
                        // Background track
                        drawArc(
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        // Animated progress arc
                        drawArc(
                            color = if (canProceed)
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                            else
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                            startAngle = -90f,
                            sweepAngle = -360f * timerProgress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    // Center text / button
                    TextButton(
                        onClick = onProceed,
                        enabled = canProceed,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (canProceed) {
                                Text(
                                    text = "Open",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            } else {
                                Text(
                                    text = "$secondsLeft",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onReturn,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Return to Home", style = MaterialTheme.typography.titleMedium)
        }
    }
}
