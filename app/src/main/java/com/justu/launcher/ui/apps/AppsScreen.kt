package com.justu.launcher.ui.apps

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.utils.AppLauncherInterceptor
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ALPHABET = ('A'..'Z').map { it.toString() } + listOf("#")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
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
                .padding(start = 32.dp, end = 52.dp),
            contentPadding = PaddingValues(bottom = 64.dp, top = 24.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search apps...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            flatList.forEachIndexed { index, item ->
                if (item.header != null) {
                    stickyHeader(key = "header_${item.header}") {
                        Text(
                            text = item.header,
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 12.dp)
                        )
                    }
                } else if (item.app != null) {
                    item(key = item.app.packageName) {
                        val app = item.app
                        val isFavorite = favoritePackages.contains(app.packageName)
                        val isBlocked = settings.blockedApps.contains(app.packageName)

                        var isHighlighted by remember { mutableStateOf(false) }

                        val graphicsModifier = Modifier.graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (visibleItem != null) {
                                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                val itemCenter = visibleItem.offset + visibleItem.size / 2f
                                val center = viewportHeight / 2f
                                val distanceFromCenter = abs(center - itemCenter)
                                
                                val normalizedDistance = (distanceFromCenter / (viewportHeight / 2f)).coerceIn(0f, 1f)
                                isHighlighted = normalizedDistance < 0.08f // Tightened threshold

                                // Smooth scale effect
                                val scale = 1.08f - (normalizedDistance * 0.08f)
                                scaleX = scale
                                scaleY = scale
                                
                                alpha = (1f - (normalizedDistance * 0.4f)).coerceIn(0.5f, 1f)
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
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 20.sp
                                ),
                                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isFavorite) {
                                    Text("★", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                }
                                if (isBlocked) {
                                    Text("Blocked", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
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
                .width(40.dp)
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
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            presentLetters.contains(letter) -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                        },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        // Active letter bubble overlay
        if (activeLetter != null) {
            Box(
                modifier = Modifier
                    .padding(end = 64.dp)
                    .size(64.dp)
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        scaleX = letterBubbleAlpha
                        scaleY = letterBubbleAlpha
                        alpha = letterBubbleAlpha
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    style = MaterialTheme.typography.displaySmall,
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
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Header
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Grid/List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Favorite Toggle
                        AppMenuButton(
                            icon = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            text = if (isFavorite) "Remove Favorite" else "Pin to Home",
                            isSelected = isFavorite,
                            onClick = {
                                viewModel.toggleFavorite(app.packageName)
                                showAppMenu = null
                            }
                        )

                        // Timer Exemption Toggle
                        AppMenuButton(
                            icon = Icons.Rounded.Bolt,
                            text = if (isExempt) "Disable Instant Launch" else "Enable Instant Launch",
                            isSelected = isExempt,
                            onClick = {
                                viewModel.toggleExempt(app.packageName)
                                showAppMenu = null
                            }
                        )

                        // Hide App
                        AppMenuButton(
                            icon = Icons.Rounded.VisibilityOff,
                            text = "Hide from Drawer",
                            isSelected = false,
                            onClick = {
                                viewModel.hideApp(app.packageName)
                                showAppMenu = null
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        // Uninstall App (System Intent)
                        AppMenuButton(
                            icon = Icons.Rounded.DeleteForever,
                            text = "Uninstall App",
                            isSelected = false,
                            color = MaterialTheme.colorScheme.error,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = android.net.Uri.parse("package:${app.packageName}")
                                }
                                context.startActivity(intent)
                                showAppMenu = null
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = { showAppMenu = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Dismiss",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
