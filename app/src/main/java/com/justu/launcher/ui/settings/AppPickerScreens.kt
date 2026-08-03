package com.justu.launcher.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justu.launcher.data.model.AppInfo

// ─────────────────────────────────────────────────────────────────────────────
// CHOOSE FAVORITE APPS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FavoriteAppsPickerScreen(
    allApps: List<AppInfo>,
    favoriteApps: List<String>,    // ordered list of package names
    maxFavorites: Int,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    val favSet = remember(favoriteApps) { favoriteApps.toSet() }
    val currentCount = favSet.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Favorite Apps",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose up to $maxFavorites apps to pin on your home screen.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Count badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111111))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(maxFavorites) { index ->
                    val filled = index < currentCount
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (filled) Color.White else Color.White.copy(alpha = 0.2f))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$currentCount / $maxFavorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (currentCount >= maxFavorites) Color.White else Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.35f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Show selected apps first, then the rest
            val selected = filteredApps.filter { favSet.contains(it.packageName) }
            val unselected = filteredApps.filter { !favSet.contains(it.packageName) }
            val sorted = selected + unselected

            items(sorted, key = { it.packageName }) { app ->
                val isFavorite = favSet.contains(app.packageName)
                val canAddMore = currentCount < maxFavorites

                FavoriteAppPickerRow(
                    app = app,
                    isFavorite = isFavorite,
                    enabled = isFavorite || canAddMore,
                    onToggle = { onToggleFavorite(app.packageName) }
                )
            }
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FavoriteAppPickerRow(
    app: AppInfo,
    isFavorite: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isFavorite) 0.12f else 0.04f,
        animationSpec = tween(200),
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                fontWeight = if (isFavorite) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 10.sp
            )
        }

        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = null,
            tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TIMER-EXEMPT APPS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExemptAppsScreen(
    allApps: List<AppInfo>,
    exemptApps: Set<String>,
    onToggleExempt: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("Timer-Exempt Apps", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Exempt apps open instantly — no 5-second mindful pause.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Use this for important apps like Phone, Maps, or Messages.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.35f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps...", color = Color.White.copy(alpha = 0.35f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            val exempted = filteredApps.filter { exemptApps.contains(it.packageName) }
            val rest = filteredApps.filter { !exemptApps.contains(it.packageName) }

            items(exempted + rest, key = { it.packageName }) { app ->
                val isExempt = exemptApps.contains(app.packageName)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isExempt) Color.White.copy(alpha = 0.1f) else Color(0xFF0D0D0D))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = if (isExempt) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isExempt) {
                            Text(
                                text = "⚡ Opens instantly",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Switch(
                        checked = isExempt,
                        onCheckedChange = { onToggleExempt(app.packageName) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
