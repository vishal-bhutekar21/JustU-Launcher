package com.justu.launcher.ui.apps

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.graphicsLayer
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
    viewModel: AppsViewModel = hiltViewModel(),
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
    var sidebarHeight by remember { mutableFloatStateOf(0f) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    val letterBubbleAlpha by animateFloatAsState(
        targetValue = if (activeLetter != null) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bubbleAlpha"
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 52.dp), // leave room for sidebar
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp)
                        )
                    }
                } else if (item.app != null) {
                    item(key = item.app.packageName) {
                        val app = item.app
                        val isFavorite = favoritePackages.contains(app.packageName)
                        val isBlocked = settings.blockedApps.contains(app.packageName)
                        val isExempt = exemptPackages.contains(app.packageName)

                        // Modern Smooth Scroll Animation
                        val graphicsModifier = Modifier.graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val visibleItem = layoutInfo.visibleItemsInfo.find { it.key == app.packageName }
                            if (visibleItem != null) {
                                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                val itemCenter = visibleItem.offset + visibleItem.size / 2f
                                val distanceFromCenter = kotlin.math.abs(viewportHeight / 2f - itemCenter)
                                
                                // Scale down items as they move away from the center of the screen
                                val scale = (1f - ((distanceFromCenter / viewportHeight) * 0.15f)).coerceIn(0.85f, 1f)
                                scaleX = scale
                                scaleY = scale
                                alpha = (1f - ((distanceFromCenter / viewportHeight) * 0.5f)).coerceIn(0.5f, 1f)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(graphicsModifier)
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
                                    isBlocked -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.onBackground
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
                                    Text("★", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                                if (isBlocked) {
                                    Text("Blocked", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
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
                            isActive -> MaterialTheme.colorScheme.primary
                            presentLetters.contains(letter) -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.padding(vertical = 0.5.dp)
                    )
                }
            }
        }

        // Active letter bubble overlay (right side, near sidebar)
        if (activeLetter != null) {
            Box(
                modifier = Modifier
                    .padding(end = 56.dp)
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        scaleX = letterBubbleAlpha
                        scaleY = letterBubbleAlpha
                        alpha = letterBubbleAlpha
                        translationX = (1f - letterBubbleAlpha) * 40f
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
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
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Favorite
                    Button(
                        onClick = { viewModel.toggleFavorite(app.packageName); showAppMenu = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
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
                            containerColor = if (isExempt) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
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
                        Text("Hide App", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}
