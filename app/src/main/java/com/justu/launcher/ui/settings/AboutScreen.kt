package com.justu.launcher.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About & Open Source",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "JustU Launcher – Digital Detox & Focus v1.0.0 • Designed for Digital Wellbeing",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Developer Info Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Created by Vishal Bhutekar",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Crafted with passion for minimalist design, intentional habits, and open-source software.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Links
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AboutActionRow(
                    title = "Star on GitHub",
                    subtitle = "View repository & source code",
                    onClick = {
                        openUrl(context, "https://github.com/vishal-bhutekar21/JustU-Launcher")
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                AboutActionRow(
                    title = "Share JustU Launcher",
                    subtitle = "Help friends reclaim their screen time",
                    onClick = {
                        shareApp(context)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                AboutActionRow(
                    title = "Developer Portfolio",
                    subtitle = "vishalbhutekar.netlify.app",
                    onClick = {
                        openUrl(context, "https://vishalbhutekar.netlify.app/")
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                AboutActionRow(
                    title = "More Apps on Play Store",
                    subtitle = "Check out other tools by Unexplored Vishal",
                    onClick = {
                        openUrl(context, "https://play.google.com/store/apps/developer?id=Unexplored+Vishal")
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                AboutActionRow(
                    title = "Follow on Instagram",
                    subtitle = "@unexplored_vish_2.0",
                    onClick = {
                        openUrl(context, "https://www.instagram.com/unexplored_vish_2.0/")
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                AboutActionRow(
                    title = "Contact via Email",
                    subtitle = "vishal.bhutekar1@gmail.com",
                    onClick = {
                        sendEmail(context)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Back to Settings")
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun AboutActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text(text = "→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
    }
}

private fun shareApp(context: Context) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out JustU Launcher – Digital Detox & Focus - a minimalist, distraction-free launcher designed to give you your time back! Download now.")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share JustU Launcher")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
    }
}

private fun sendEmail(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:vishal.bhutekar1@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "JustU Launcher – Digital Detox & Focus Feedback")
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } catch (e: Exception) {
    }
}
