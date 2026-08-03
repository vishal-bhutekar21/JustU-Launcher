package com.justu.launcher.ui.apps

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.utils.AppLauncherInterceptor
import kotlinx.coroutines.launch

private val ALPHABET = ('A'..'Z').map { it.toString() } + listOf("#")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val settings by viewModel.homeSettings.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showAppMenu by remember { mutableStateOf<AppInfo?>(null) }
    val favoritePackages = remember(settings.favoriteApps) { settings.favoriteApps.toSet() }
    val exemptPackages = remember(settings.exemptApps) { settings.exemptApps }

    // Group apps by first letter
    val groupedApps = remember(apps) {
        apps.groupBy { app ->
            val first = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            if (first.isLetter()) first.toString() else "#"
        }
    }

    // Build flat list with header items interspersed
    data class AppListItem(val header: String? = null, val app: AppInfo? = null)
    val flatList = remember(groupedApps) {
        val result = mutableListOf<AppListItem>()
        ALPHABET.forEach { letter ->
            val group = groupedApps[letter]
            if (!group.isNullOrEmpty()) {
                result.add(AppListItem(header = letter))
                group.forEach { result.add(AppListItem(app = it)) }
            }
        }
        result
    }

    // Map each letter to its index in flatList
    val letterIndexMap = remember(flatList) {
        val map = mutableMapOf<String, Int>()
        flatList.forEachIndexed { idx, item ->
            if (item.header != null && !map.containsKey(item.header)) map[item.header] = idx
        }
        map
    }

    // Sidebar drag state
    var sidebarHeight by remember { mutableStateOf(0f) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    val letterBubbleAlpha by animateFloatAsState(
        targetValue = if (activeLetter != null) 1f else 0f,
        animationSpec = tween(150),
        label = "bubbleAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 48.dp), // leave room for sidebar
            contentPadding = PaddingValues(bottom = 64.dp, top = 24.dp)
        ) {
            flatList.forEach { item ->
                if (item.header != null) {
                    stickyHeader(key = "header_${item.header}") {
                        Text(
                            text = item.header,
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(vertical = 8.dp)
                        )
                    }
                } else if (item.app != null) {
                    item(key = item.app.packageName) {
                        val app = item.app
                        val isFavorite = favoritePackages.contains(app.packageName)
                        val isBlocked = settings.blockedApps.contains(app.packageName)
                        val isExempt = exemptPackages.contains(app.packageName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            app.launchIntent?.let {
                                                AppLauncherInterceptor.launchAppMindfully(
                                                    context, it, app.packageName,
                                                    isFocusMode = settings.isFocusModeEnabled,
                                                    isBlocked = isBlocked,
                                                    favoritePackages = favoritePackages,
                                                    exemptPackages = exemptPackages
                                                )
                                            }
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showAppMenu = app
                                        }
                                    )
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleLarge,
                                color = when {
                                    isBlocked -> Color.White.copy(alpha = 0.25f)
                                    else -> Color.White
                                }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isExempt) {
                                    Text("⚡", fontSize = 13.sp)
                                }
                                if (isFavorite) {
                                    Text("★", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.5f))
                                }
                                if (isBlocked) {
                                    Text("Blocked", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── A–Z Fast-scroll Sidebar ───────────────────────────────────
        val presentLetters = remember(letterIndexMap) { letterIndexMap.keys.toSet() }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(32.dp)
                .onGloballyPositioned { coords -> sidebarHeight = coords.size.height.toFloat() }
                .pointerInput(letterIndexMap, sidebarHeight) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.y / sidebarHeight).coerceIn(0f, 1f)
                            val idx = (fraction * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
                            val letter = ALPHABET[idx]
                            if (presentLetters.contains(letter)) {
                                activeLetter = letter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                letterIndexMap[letter]?.let { listIdx ->
                                    scope.launch { listState.scrollToItem(listIdx) }
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            val fraction = (offset.y / sidebarHeight).coerceIn(0f, 1f)
                            val idx = (fraction * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
                            val letter = ALPHABET[idx]
                            if (letter != activeLetter && presentLetters.contains(letter)) {
                                activeLetter = letter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                letterIndexMap[letter]?.let { listIdx ->
                                    scope.launch { listState.scrollToItem(listIdx) }
                                }
                            }
                        },
                        onDragEnd = { activeLetter = null },
                        onDragCancel = { activeLetter = null }
                    )
                }
                .pointerInput(letterIndexMap, sidebarHeight) {
                    detectTapGestures { offset ->
                        val fraction = (offset.y / sidebarHeight).coerceIn(0f, 1f)
                        val idx = (fraction * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
                        val letter = ALPHABET[idx]
                        if (presentLetters.contains(letter)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            letterIndexMap[letter]?.let { listIdx ->
                                scope.launch { listState.scrollToItem(listIdx) }
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ALPHABET.forEach { letter ->
                    val isActive = activeLetter == letter
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.4f else if (presentLetters.contains(letter)) 1f else 0.7f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "letterScale"
                    )
                    Text(
                        text = letter,
                        fontSize = (9 * scale).sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> Color.White
                            presentLetters.contains(letter) -> Color.White.copy(alpha = 0.45f)
                            else -> Color.White.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.padding(vertical = 0.5.dp)
                    )
                }
            }
        }

        // Active letter bubble overlay (center of screen)
        if (activeLetter != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .alpha(letterBubbleAlpha)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    // ── App Long-Press Menu ───────────────────────────────────────────
    if (showAppMenu != null) {
        val app = showAppMenu!!
        val isFavorite = settings.favoriteApps.contains(app.packageName)
        val isExempt = settings.exemptApps.contains(app.packageName)

        Dialog(onDismissRequest = { showAppMenu = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111111),
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Favorite
                    Button(
                        onClick = { viewModel.toggleFavorite(app.packageName); showAppMenu = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isFavorite) "★ Remove from Favorites" else "☆ Add to Favorites")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Exempt from timer
                    Button(
                        onClick = { viewModel.toggleExempt(app.packageName); showAppMenu = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExempt) Color(0xFF1A1A1A) else Color(0xFF222222),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isExempt) "⚡ Remove Timer Exemption" else "⚡ Exempt from 5s Timer")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hide
                    TextButton(
                        onClick = { viewModel.hideApp(app.packageName); showAppMenu = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hide App", color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}
