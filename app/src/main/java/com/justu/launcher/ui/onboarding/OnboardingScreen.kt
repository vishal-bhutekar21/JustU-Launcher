package com.justu.launcher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0A0A0A)
private val AccentViolet = Color(0xFF9B5DE5)
private val AccentTeal = Color(0xFF00BBD4)

data class OnboardingPage(
    val tag: String,
    val headline: String,
    val body: String,
    val emoji: String
)

private val pages = listOf(
    OnboardingPage(
        tag = "WELCOME",
        headline = "Less Phone,\nMore Life.",
        body = "JustU is a mindful launcher built to help you reclaim your time, one intentional tap at a time.",
        emoji = "✦"
    ),
    OnboardingPage(
        tag = "MINDFUL LAUNCHING",
        headline = "Pause Before\nYou Open.",
        body = "Every app launch shows you a moment of reflection — asking 'Is this necessary?' so you decide with intention.",
        emoji = "☽"
    ),
    OnboardingPage(
        tag = "AWARENESS",
        headline = "Goals &\nReality Checks.",
        body = "Swipe left to set daily intentions. Swipe right to see your real screen-time stats and hold yourself accountable.",
        emoji = "◎"
    ),
    OnboardingPage(
        tag = "SETUP",
        headline = "Two Steps\nto Begin.",
        body = "Set JustU as your default launcher and grant usage access so we can show you honest screen time data.",
        emoji = "◈"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(
    onComplete: () -> Unit,
    context: Context
) {
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { /* Force explicit completion */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
            // Gradient top accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentViolet.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(32.dp))

                // Page Indicator at top
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "indicatorWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) AccentViolet else Color.White.copy(alpha = 0.2f),
                            animationSpec = tween(300),
                            label = "indicatorColor"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val p = pages[page]
                    if (page < pages.size - 1) {
                        OnboardingSlide(page = p)
                    } else {
                        OnboardingFinalSlide(context = context, onComplete = onComplete)
                    }
                }

                // Footer
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = onComplete) {
                            Text("Skip", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.height(52.dp).width(120.dp)
                        ) {
                            Text(
                                text = if (pagerState.currentPage == pages.size - 2) "Finish" else "Next",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // On final slide, footer is hidden (final slide has its own CTA)
                        Spacer(modifier = Modifier.height(52.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OnboardingSlide(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        // Large decorative emoji
        Text(
            text = page.emoji,
            fontSize = 64.sp,
            color = AccentViolet,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = page.tag,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 4.sp,
                color = AccentViolet.copy(alpha = 0.8f)
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.headline,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 52.sp
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 28.sp
            )
        )
    }
}

@Composable
fun OnboardingFinalSlide(
    context: Context,
    onComplete: () -> Unit
) {
    var launcherDone by remember { mutableStateOf(false) }
    var usageDone by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SETUP",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 4.sp,
                    color = AccentViolet.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Almost\nThere.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 52.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Complete the two steps below to start your intentional journey.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 28.sp
                )
            )
        }

        Column {
            // Step 1
            SetupActionCard(
                stepNumber = "01",
                title = "Set Default Launcher",
                description = "Make JustU your home screen so every unlock is intentional.",
                isDone = launcherDone,
                onClick = {
                    launcherDone = true
                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2
            SetupActionCard(
                stepNumber = "02",
                title = "Grant Usage Access",
                description = "Allows JustU to show your honest daily screen-time breakdown.",
                isDone = usageDone,
                onClick = {
                    usageDone = true
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Start Using JustU", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SetupActionCard(
    stepNumber: String,
    title: String,
    description: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isDone) AccentViolet.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        animationSpec = tween(400),
        label = "cardBg"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDone) AccentViolet else Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stepNumber, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isDone) AccentViolet else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
