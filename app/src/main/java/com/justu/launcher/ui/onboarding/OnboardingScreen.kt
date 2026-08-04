package com.justu.launcher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.justu.launcher.ui.onboarding.SetupActionCard
import kotlinx.coroutines.launch
import com.justu.launcher.utils.rememberUsageAccessGranted

// ── Light blue onboarding palette ─────────────────────────────────────────
private val BgColor       = Color(0xFFF4F8FD)
private val AccentColor   = Color(0xFF2F6BFF)
private val DimColor      = Color(0xFFD7E4FB)
private val CardColor     = Color(0xFFF8FBFF)
private val TextPrimary   = Color(0xFF152033)
private val TextSecondary = Color(0xFF6F829E)

data class OnboardingPage(
    val tag: String,
    val headline: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        tag = "WELCOME",
        headline = "Less Phone,\nMore Life.",
        body = "JustU Launcher is a mindful launcher built to help you reclaim your time, one intentional tap at a time."
    ),
    OnboardingPage(
        tag = "MINDFUL LAUNCHING",
        headline = "Pause Before\nYou Open.",
        body = "Every app launch shows you a moment of reflection — asking 'Is this necessary?' so you decide with intention."
    ),
    OnboardingPage(
        tag = "AWARENESS",
        headline = "Goals &\nReality Checks.",
        body = "Swipe left to set daily intentions. Swipe right to see your real screen-time stats and hold yourself accountable."
    ),
    OnboardingPage(
        tag = "SETUP",
        headline = "Two Steps\nto Begin.",
        body = "Set JustU Launcher as your default launcher and grant usage access so we can show you honest screen time data."
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
        onDismissRequest = { /* must complete explicitly */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page < pages.size - 1) {
                    OnboardingSlide(page = pages[page])
                } else {
                    OnboardingFinalSlide(context = context, onComplete = onComplete)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 26.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "indicatorWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) AccentColor else DimColor,
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(120.dp)
                    ) {
                        Text(
                            text = "Previous",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentColor,
                            contentColor = BgColor
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(120.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == pages.size - 1) "Start" else "Next",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingSlide(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = CardColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Text(
                text = page.tag,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 3.sp,
                    color = TextSecondary
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Text(
            text = page.headline,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 52.sp
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = TextSecondary,
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
    val scrollState = rememberScrollState()
    var launcherDone by remember { mutableStateOf(false) }
    val usageDone by rememberUsageAccessGranted(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = CardColor,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Text(
                text = "SETUP",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 3.sp,
                    color = TextSecondary
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column {
            Text(
                text = "Almost\nThere.",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 52.sp
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Complete the two steps below to start your intentional journey.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextSecondary,
                    lineHeight = 28.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column {
            SetupActionCard(
                stepNumber = "01",
                title = "Set Default Launcher",
                description = "Make JustU Launcher your home screen so every unlock is intentional.",
                isDone = launcherDone,
                onClick = {
                    launcherDone = true
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            SetupActionCard(
                stepNumber = "02",
                title = "Grant Usage Access",
                description = "Allows JustU Launcher to show your honest daily screen-time breakdown.",
                isDone = usageDone,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor,
                    contentColor = BgColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Start Using JustU Launcher", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
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
        targetValue = if (isDone) Color(0xFF1A1A1A) else Color(0xFF111111),
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDone) AccentColor else DimColor),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = BgColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(stepNumber, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isDone) AccentColor else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
