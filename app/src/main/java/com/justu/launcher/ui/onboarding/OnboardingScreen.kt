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
        body = "JustU is a mindful space built to help you reclaim your time, one intentional tap at a time.",
        icon = Icons.Rounded.Spa
    ),
    OnboardingPage(
        tag = "MINDFUL LAUNCH",
        headline = "Pause Before\nYou Open.",
        body = "Every app launch shows you a moment of reflection — asking 'Is this necessary?'",
        icon = Icons.Rounded.Visibility
    ),
    OnboardingPage(
        tag = "INTENTIONS",
        headline = "Goals &\nFocus.",
        body = "Set daily intentions and stay focused on what truly matters most today.",
        icon = Icons.Rounded.Bolt
    ),
    OnboardingPage(
        tag = "SETUP",
        headline = "Final Step\nto Begin.",
        body = "Set JustU as your default launcher so every unlock is an intentional choice.",
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
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 64.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicators
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "w")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                    }
                }

                // Next/Start Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.height(64.dp).width(120.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
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

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.tag,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.headline,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 48.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            lineHeight = 32.sp
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
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "SETUP",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Almost\nThere.",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 48.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Complete the final step to start your intentional journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            onClick = {
                launcherDone = true
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            },
            shape = RoundedCornerShape(28.dp),
            color = if (launcherDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (launcherDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (launcherDone) {
                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(32.dp))
                    } else {
                        Text("01", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Default Launcher", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Make JustU your home screen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}
