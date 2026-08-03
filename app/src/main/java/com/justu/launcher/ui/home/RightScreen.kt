package com.justu.launcher.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RightScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val topApps by viewModel.topApps.collectAsState()
    val context = LocalContext.current

    val totalMillis = topApps.sumOf { it.second }
    val maxMillis = topApps.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
    val totalHours = totalMillis / (1000 * 60 * 60)
    val totalMins = (totalMillis / (1000 * 60)) % 60
    val totalTimeString = if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m"

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Reality Check",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your digital footprint today.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (topApps.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL SCREEN TIME",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = totalTimeString,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (topApps.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111111),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Usage Data Available",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To view your top used apps and reality check statistics, please grant Usage Access permission.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Grant Usage Access")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(topApps) { (appName, millis) ->
                    val hours = millis / (1000 * 60 * 60)
                    val mins = (millis / (1000 * 60)) % 60
                    val timeString = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    val progress = (millis.toFloat() / maxMillis.toFloat()).coerceIn(0.05f, 1.0f)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111111),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = progress)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
