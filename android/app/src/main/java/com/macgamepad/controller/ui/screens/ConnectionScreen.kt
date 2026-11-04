package com.macgamepad.controller.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macgamepad.controller.GamepadViewModel
import com.macgamepad.controller.bt.BluetoothGamepadService
import com.macgamepad.controller.ui.theme.*

@Composable
fun ConnectionScreen(
    connectionState: BluetoothGamepadService.ConnectionState,
    connectedDeviceName: String?,
    errorMessage: String?,
    pairedDevices: List<BluetoothDevice>,
    onDeviceConnectClick: (BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit,
    onStartGamepad: () -> Unit,
    vm: GamepadViewModel,
    modifier: Modifier = Modifier
) {
    val permissionState by vm.permissionState.collectAsState()
    val isRequestingPermissions by vm.isRequestingPermissions.collectAsState()
    val initializationState by vm.initializationState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Bg0, Bg1, Bg1))
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.06f), Color.Transparent),
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 20.dp)
        ) {

            // ═══════════════════════════════════════════════════════════════
            // LOGO MARK + TITLE
            // ══════════════════════════════════════════════════════════════
            Spacer(Modifier.height(8.dp))

            // Canvas-drawn controller logo
            Canvas(modifier = Modifier.size(88.dp)) {
                val cx = size.width / 2f; val cy = size.height / 2f
                val bw = size.width; val bh = size.height

                // Outer glow halo
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(AccentBlue.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(cx, cy), radius = cx * 1.3f
                    ),
                    radius = cx * 1.3f, center = Offset(cx, cy)
                )

                // Controller body — main wide pill
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Bg4, Bg3)),
                    topLeft = Offset(bw * 0.08f, bh * 0.28f),
                    size = Size(bw * 0.84f, bh * 0.40f),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawRoundRect(
                    color = AccentBlue.copy(alpha = 0.45f),
                    topLeft = Offset(bw * 0.08f, bh * 0.28f),
                    size = Size(bw * 0.84f, bh * 0.40f),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(1.5f)
                )

                // Left grip
                drawRoundRect(
                    color = Bg3,
                    topLeft = Offset(bw * 0.14f, bh * 0.52f),
                    size = Size(bw * 0.22f, bh * 0.30f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = AccentBlue.copy(alpha = 0.35f),
                    topLeft = Offset(bw * 0.14f, bh * 0.52f),
                    size = Size(bw * 0.22f, bh * 0.30f),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(1.2f)
                )

                // Right grip
                drawRoundRect(
                    color = Bg3,
                    topLeft = Offset(bw * 0.64f, bh * 0.52f),
                    size = Size(bw * 0.22f, bh * 0.30f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = AccentBlue.copy(alpha = 0.35f),
                    topLeft = Offset(bw * 0.64f, bh * 0.52f),
                    size = Size(bw * 0.22f, bh * 0.30f),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(1.2f)
                )

                // Left shoulder
                drawRoundRect(
                    color = Bg4,
                    topLeft = Offset(bw * 0.08f, bh * 0.18f),
                    size = Size(bw * 0.24f, bh * 0.14f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Right shoulder
                drawRoundRect(
                    color = Bg4,
                    topLeft = Offset(bw * 0.68f, bh * 0.18f),
                    size = Size(bw * 0.24f, bh * 0.14f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Left stick
                drawCircle(
                    brush = Brush.radialGradient(listOf(AccentCyan.copy(alpha = 0.6f), AccentBlue.copy(alpha = 0.2f))),
                    radius = 7f, center = Offset(bw * 0.29f, bh * 0.46f)
                )
                // Right stick
                drawCircle(
                    brush = Brush.radialGradient(listOf(AccentCyan.copy(alpha = 0.6f), AccentBlue.copy(alpha = 0.2f))),
                    radius = 7f, center = Offset(bw * 0.58f, bh * 0.52f)
                )

                // ABXY dots
                val btnColors = listOf(
                    Color(0xFF22C55E), Color(0xFFEF4444), Color(0xFF4D8EFF), Color(0xFFF59E0B)
                )
                val btnPos = listOf(
                    Offset(bw*0.74f, bh*0.40f),
                    Offset(bw*0.78f, bh*0.46f),
                    Offset(bw*0.70f, bh*0.46f),
                    Offset(bw*0.74f, bh*0.52f)
                )
                btnPos.zip(btnColors).forEach { (pos, col) ->
                    drawCircle(color = col.copy(alpha = 0.7f), radius = 4f, center = pos)
                }

                // Centre connection dot
                drawCircle(
                    brush = Brush.radialGradient(listOf(AccentBlue.copy(alpha = 0.9f), AccentCyan.copy(alpha = 0.0f))),
                    radius = 5f,
                    center = Offset(cx, cy)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Title
            Text(
                text = "MacGamepad",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = TextPrimary,
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f
                    )
                }
            )
            Text(
                text = "Bluetooth Game Controller",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(24.dp))

            // ══════════════════════════════════════════════════════════════
            // SINGLE UNIFIED STATE CARD
            // ══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Bg2)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Check if we're in the initialization flow
                val isInitializing = initializationState != GamepadViewModel.InitializationState.Idle
                val isInitialized = permissionState is GamepadViewModel.PermissionStatus.Granted && initializationState == GamepadViewModel.InitializationState.Idle

                if (!isInitialized) {
                    // Show initialization flow
                    InitializationScreen(
                        state = initializationState,
                        onStart = { vm.startInitialization() },
                        onOpenSettings = { vm.openAppSettings() },
                        onRetry = { vm.startInitialization() }
                    )
                } else {
                    // Permissions granted and initialization complete - show connection state UI
                    ConnectionStateContent(
                        connectionState = connectionState,
                        connectedDeviceName = connectedDeviceName,
                        errorMessage = errorMessage,
                        pairedDevices = pairedDevices,
                        onDeviceConnectClick = onDeviceConnectClick,
                        onDisconnectClick = onDisconnectClick,
                        onStartGamepad = onStartGamepad,
                        vm = vm
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Version footer
            Text(
                "v1.0 · MacGamepad",
                fontSize = 10.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// PERMISSION FLOW SCREENS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun InitializationScreen(
    state: GamepadViewModel.InitializationState,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is GamepadViewModel.InitializationState.Idle -> {
            // Initial state - show start button
            PermissionIntroScreen(onInitialize = onStart)
        }
        is GamepadViewModel.InitializationState.RequestingPermissions -> {
            // Show progress while requesting permissions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Requesting Bluetooth permissions…",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.RequestingBluetoothEnable -> {
            // Show progress while enabling Bluetooth
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Enabling Bluetooth…",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.RequestingDiscoverability -> {
            // Show progress while making discoverable
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Making phone discoverable…",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.Error -> {
            // Show error with retry option
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    state.message,
                    fontSize = 13.sp,
                    color = AccentRed,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                if (state.message.contains("permission", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(AccentBlue, Color(0xFF2563EB))))
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Open Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
                TextButton(onClick = onRetry) {
                    Text("Try Again", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = AccentBlue)
                }
            }
        }
    }
}

@Composable
private fun PermissionIntroScreen(onInitialize: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Turn your phone into a gamepad for your Mac.\nWe'll set up Bluetooth so your Mac can find it.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(AccentBlue, Color(0xFF2563EB)))
                )
                .clickable { onInitialize() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Initialize Controller", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONNECTION STATE CONTENT (single unified card)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ConnectionStateContent(
    connectionState: BluetoothGamepadService.ConnectionState,
    connectedDeviceName: String?,
    errorMessage: String?,
    pairedDevices: List<BluetoothDevice>,
    onDeviceConnectClick: (BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit,
    onStartGamepad: () -> Unit,
    vm: GamepadViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error message (inline, dismissible feel)
        errorMessage?.let { err ->
            // Don't show "Bluetooth is not enabled" error since we handle that in init flow
            if (!err.contains("Bluetooth is not enabled", ignoreCase = true)) {
                ErrorInline(err)
            }
        }

        when (connectionState) {
            BluetoothGamepadService.ConnectionState.DISCONNECTED -> {
                DisconnectedState(onRequestDiscoverable = { vm.requestDiscoverability() })
            }

            BluetoothGamepadService.ConnectionState.ERROR -> {
                // All Bluetooth errors should be handled in init flow now
                DisconnectedState(onRequestDiscoverable = { vm.requestDiscoverability() })
            }

            BluetoothGamepadService.ConnectionState.REGISTERING,
            BluetoothGamepadService.ConnectionState.CONNECTING -> {
                ConnectingState()
            }

            BluetoothGamepadService.ConnectionState.REGISTERED_WAITING -> {
                DiscoverableState(
                    pairedDevices = pairedDevices,
                    onDeviceClick = onDeviceConnectClick
                )
            }

            BluetoothGamepadService.ConnectionState.CONNECTED -> {
                ConnectedState(
                    deviceName = connectedDeviceName,
                    onStart = onStartGamepad,
                    onDisconnect = onDisconnectClick
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STATE-SPECIFIC CONTENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ErrorInline(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.08f))
            .border(1.dp, AccentRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = AccentRed, modifier = Modifier.size(14.dp))
        Text(message, fontSize = 11.sp, color = AccentRed, lineHeight = 15.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DisconnectedState(onRequestDiscoverable: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Friendly status text instead of "Not Connected" badge
        Text(
            "Ready to pair",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            "Make your phone discoverable, then pair from your Mac's Bluetooth settings.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        // Primary action - prominent button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(AccentBlue, Color(0xFF2563EB)))
                )
                .clickable { onRequestDiscoverable() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BluetoothSearching, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Make Discoverable", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun ConnectingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            color = AccentBlue,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
        Text(
            "Setting up…",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DiscoverableState(
    pairedDevices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulsing beacon
        PulsingBeacon()

        Text(
            "Discoverable — waiting for Mac",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // Pairing guide card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Bg3)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("How to Pair", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 0.5.sp)
            PairingStepRow("1", "Open Bluetooth settings on your Mac")
            PairingStepRow("2", "Remove any old phone connection")
            PairingStepRow("3", "Find your phone name and click Connect")
        }

        // Paired Mac hosts - progressive disclosure
        val macHosts = remember(pairedDevices) {
            pairedDevices.filter { device ->
                try {
                    val name = device.name ?: ""
                    name.contains("mac", ignoreCase = true) ||
                    name.contains("book", ignoreCase = true) ||
                    name.contains("imac", ignoreCase = true) ||
                    name.contains("studio", ignoreCase = true) ||
                    name.contains("mini", ignoreCase = true) ||
                    name.isEmpty()
                } catch (e: SecurityException) { true }
            }
        }

        if (macHosts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Previously Paired Macs",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextTertiary
                )
                macHosts.forEach { device ->
                    val name = try { device.name ?: "Mac" } catch (e: SecurityException) { "Mac" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Bg4)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { onDeviceClick(device) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Laptop, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Text(name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedState(
    deviceName: String?,
    onStart: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connected status with subtle indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(StatusConnected.copy(alpha = 0.08f))
                .border(1.dp, StatusConnected.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusConnected))
            Text("Connected to ${deviceName ?: "Mac"}", color = StatusConnected, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Primary action - Launch Controller
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(StatusConnected, Color(0xFF16A34A))))
                .clickable { onStart() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text("Launch Controller", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
            }
        }

        // Disconnect - subtle text button
        Text(
            "Disconnect",
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier
                .clickable { onDisconnect() }
                .padding(12.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Pulsing beacon — shown during REGISTERED_WAITING state
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PulsingBeacon() {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        // Outer pulse ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = AccentAmber.copy(alpha = pulseAlpha),
                radius = (size.minDimension / 2f) * pulseScale,
                style = Stroke(2f)
            )
        }
        // Inner static dot
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentAmber.copy(alpha = 0.15f))
                .border(1.5.dp, AccentAmber.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bluetooth, null, tint = AccentAmber, modifier = Modifier.size(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Numbered pairing step row
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PairingStepRow(num: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.15f))
                .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontSize = 9.sp, color = AccentBlue, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Bluetooth Enable Screen - shown when Bluetooth is disabled (kept for fallback)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun BluetoothEnableScreen(
    onEnable: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backHandlerEnabled = remember { true }
    BackHandler(backHandlerEnabled) {
        // Consume back press - user must explicitly choose Enable or Cancel
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

            // Bluetooth icon with glow
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentBlue.copy(alpha = 0.2f), Color.Transparent),
                            center = Offset(44f, 44f),
                            radius = 60f
                        )
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = AccentBlue, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Bluetooth is Off",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Please enable Bluetooth to use MacGamepad",
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
                    "MacGamepad needs Bluetooth to be enabled to work as a gamepad controller for your Mac.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enable Bluetooth button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(AccentBlue, Color(0xFF2563EB)))
                        )
                        .clickable { onEnable() },
                    contentAlignment = Alignment.Center
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
                            Icon(Icons.Default.Bluetooth, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("Enable Bluetooth", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Cancel button
                Text(
                    "Cancel",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onCancel() }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}