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
fun DiscoverabilityScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backHandlerEnabled = remember { true }
    androidx.activity.compose.BackHandler(backHandlerEnabled) {
        // Consume back press - user must explicitly choose
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

            // Discoverability icon with glow
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentGreen.copy(alpha = 0.2f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(44f, 44f),
                            radius = 60f
                        )
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Visibility, null, tint = AccentGreen, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Make Your Phone Discoverable",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Help your Mac find this phone for pairing",
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
                DiscoverabilityInfoRow(
                    icon = Icons.Default.Timer,
                    title = "Visible for 5 minutes",
                    description = "Your phone will appear in Mac's Bluetooth scan for 300 seconds",
                    color = AccentGreen
                )
                DiscoverabilityInfoRow(
                    icon = Icons.Default.Link,
                    title = "One-time setup",
                    description = "After first pairing, your Mac remembers this phone",
                    color = AccentBlue
                )
                DiscoverabilityInfoRow(
                    icon = Icons.Default.Security,
                    title = "Secure connection",
                    description = "Only your Mac can connect — no other devices see this phone",
                    color = AccentPurple
                )
            }

            Spacer(Modifier.height(24.dp))

            // Alternative option
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Bg3)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Text(
                        "Prefer manual pairing?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    "You can skip discoverability and pair from your Mac's Bluetooth settings instead. Your phone will still be visible as \"MacGamepad\".",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Allow Discoverability button
                Button(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.White
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
                            Icon(Icons.Default.BluetoothConnected, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Allow Discoverability", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Skip for now button
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skip for now — pair manually", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextSecondary)
                        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiscoverabilityInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(description, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0E)
@Composable
fun DiscoverabilityScreenPreview() {
    DiscoverabilityScreen(
        onAllow = {},
        onSkip = {}
    )
}