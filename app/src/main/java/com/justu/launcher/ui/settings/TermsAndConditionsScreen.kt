package com.justu.launcher.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsAndConditionsScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Unified Night Dark
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Terms & Privacy Policy",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your privacy and data protection commitment.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFA1A4A8)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A0B0D),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "1. Purely On-Device Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "JustU Launcher – Digital Detox & Focus operates 100% locally on your device. We do not collect, transmit, sell, or store any personal data, app usage statistics, or analytics on external servers.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFA1A4A8),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "2. Permission Disclosures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Device Admin (Optional): Used solely for double-tap to lock screen.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFA1A4A8),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "3. Open Source Commitment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "JustU Launcher is an open-source digital wellbeing project built to help users reduce smartphone addiction and cultivate intentional habits.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFA1A4A8),
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2F6BFF),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Go Back", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
