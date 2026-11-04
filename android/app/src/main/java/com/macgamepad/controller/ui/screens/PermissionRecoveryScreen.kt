package com.macgamepad.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.macgamepad.controller.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PermissionRecoveryScreen(
    missingPermissions: List<String> = emptyList(),
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backHandlerEnabled = remember { true }
    androidx.activity.compose.BackHandler(backHandlerEnabled) {
        // Consume back press - user must explicitly choose action
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Bg0, Bg1, Bg1))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // Warning icon with glow
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentAmber.copy(alpha = 0.2f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(44f, 44f),
                            radius = 60f
                        )
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = AccentAmber, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Bluetooth Permissions Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "You previously chose \"Don't ask again\" for Bluetooth permissions.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))

            // Explanation card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Bg2)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "MacGamepad cannot function without Bluetooth permissions. Please enable them in Android Settings.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                if (missingPermissions.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Missing permissions:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        missingPermissions.forEach { perm ->
                            Text(
                                perm.substringAfterLast("."),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentAmber,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .background(Bg3)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open Settings button
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Text("Open Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }

                // Retry button (after granting in Settings)
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentBlue)
                ) {
                    Text("I've granted permissions — retry", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AccentBlue)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0E)
@Composable
fun PermissionRecoveryScreenPreview() {
    PermissionRecoveryScreen(
        missingPermissions = listOf("android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"),
        onOpenSettings = {},
        onRetry = {}
    )
}