package com.justu.launcher.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ── Premium Night Dark Palette ──────────────────────────────────────────
private val BgColor       = Color(0xFF000000)
private val AccentColor   = Color(0xFF2F6BFF)
private val DimColor      = Color(0xFF15171A)
private val CardColor     = Color(0xFF0A0B0D)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFA1A4A8)

data class OnboardingPage(
    val tag: String,
    val headline: String,
    val body: String,
    val icon: ImageVector
)

private val pages = listOf(
    OnboardingPage(
        tag = "WELCOME",
        headline = "Less Phone,\nMore Life.",
        body = "JustU Launcher is a mindful space built to help you reclaim your time, one intentional tap at a time.",
        icon = Icons.Rounded.Spa
    ),
    OnboardingPage(
        tag = "MINDFUL LAUNCH",
        headline = "Pause Before\nYou Open.",
        body = "Every app launch shows you a moment of reflection — asking 'Is this necessary?' so you decide with intention.",
        icon = Icons.Rounded.Visibility
    ),
    OnboardingPage(
        tag = "INTENTIONS",
        headline = "Goals &\nIntentions.",
        body = "Swipe left to set daily intentions and stay focused on what truly matters most today.",
        icon = Icons.Rounded.Bolt
    ),
    OnboardingPage(
        tag = "SETUP",
        headline = "One Step\nto Begin.",
        body = "Set JustU Launcher as your default launcher so every unlock is an intentional choice.",
        icon = Icons.Rounded.SettingsSuggest
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    onComplete: () -> Unit,
    context: Context
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Dialog(
        onDismissRequest = { /* force explicit complete */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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

            // Navigation Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 68.dp)
            ) {
                // Back Button
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Text("Back", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Indicators
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "w")
                        val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.3f, label = "a")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(AccentColor.copy(alpha = alpha))
                        )
                    }
                }

                // Next Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(56.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Start" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingSlide(page: OnboardingPage) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // Icon Glow Effect
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AccentColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, AccentColor.copy(alpha = 0.2f))
        ) {
            Text(
                text = page.tag,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AccentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.headline,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 52.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 32.sp,
            fontSize = 19.sp
        )
        
        Spacer(modifier = Modifier.height(180.dp))
    }
}

@Composable
fun OnboardingFinalSlide(
    context: Context,
    onComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    var launcherDone by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AccentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AccentColor.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, AccentColor.copy(alpha = 0.2f))
        ) {
            Text(
                text = "GET STARTED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AccentColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Almost\nThere.",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 52.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Complete the final step below to start your intentional journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 28.sp,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

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
        
        Spacer(modifier = Modifier.height(180.dp))
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
        targetValue = if (isDone) AccentColor.copy(alpha = 0.1f) else CardColor,
        animationSpec = tween(400),
        label = "bg"
    )

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isDone) AccentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isDone) AccentColor else DimColor),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(28.dp))
                } else {
                    Text(stepNumber, color = AccentColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isDone) AccentColor else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(description, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
            }
        }
    }
}
