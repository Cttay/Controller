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
    val initializationState by vm.initializationState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ConnectionScreenContent(
            innerPadding = innerPadding,
            permissionState = permissionState,
            initializationState = initializationState,
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

@Composable
private fun ConnectionScreenContent(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    permissionState: GamepadViewModel.PermissionStatus,
    initializationState: GamepadViewModel.InitializationState,
    connectionState: BluetoothGamepadService.ConnectionState,
    connectedDeviceName: String?,
    errorMessage: String?,
    pairedDevices: List<BluetoothDevice>,
    onDeviceConnectClick: (BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit,
    onStartGamepad: () -> Unit,
    vm: GamepadViewModel
) {
    // Capture Material3 colors for use in DrawScope
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Brush.verticalGradient(listOf(Bg0, Bg1, Bg1))),
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

            // LOGO MARK + TITLE
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
                color = onSurfaceColor,
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            radius = size.width * 0.6f
                        ),
                        radius = size.width * 0.6f
                    )
                }
            )
            Text(
                text = "Bluetooth Game Controller",
                fontSize = 12.sp,
                color = onSurfaceVariantColor,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )

            Spacer(Modifier.height(24.dp))

            // SINGLE UNIFIED STATE CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                val isInitialized = permissionState is GamepadViewModel.PermissionStatus.Granted &&
                    initializationState == GamepadViewModel.InitializationState.Completed

                if (!isInitialized) {
                    InitializationScreen(
                        state = initializationState,
                        onStart = { vm.startInitialization() },
                        onOpenSettings = { vm.openAppSettings() },
                        onRetry = { vm.startInitialization() },
                        vm = vm
                    )
                } else {
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
            }

            Spacer(Modifier.height(16.dp))

            // Version footer
            Text(
                "v1.0 · MacGamepad",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// PERMISSION FLOW SCREENS

@Composable
private fun InitializationScreen(
    state: GamepadViewModel.InitializationState,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    vm: GamepadViewModel
) {
    when (state) {
        is GamepadViewModel.InitializationState.Idle -> {
            PermissionIntroScreen(onInitialize = onStart)
        }
        is GamepadViewModel.InitializationState.RequestingPermissions -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Requesting Bluetooth permissions…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.RequestingBluetoothEnable -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Enabling Bluetooth…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.ShowingDiscoverabilityScreen -> {
            DiscoverabilityScreen(
                onAllow = { vm.onDiscoverabilityScreenAllow() },
                onSkip = { vm.onDiscoverabilitySkipped() },
                onBack = { vm.resetInitialization() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        is GamepadViewModel.InitializationState.RequestingDiscoverability -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Making phone discoverable…",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        is GamepadViewModel.InitializationState.Error -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    state.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                if (state.message.contains("permission", ignoreCase = true)) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Text("Open Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
                TextButton(onClick = onRetry) {
                    Text("Try Again", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        is GamepadViewModel.InitializationState.Completed -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    "Ready",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Button(
            onClick = onInitialize,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Text("Initialize Controller", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// CONNECTION STATE CONTENT (single unified card)

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
        errorMessage?.let { err ->
            if (!err.contains("Bluetooth is not enabled", ignoreCase = true)) {
                ErrorInline(err)
            }
        }

        when (connectionState) {
            BluetoothGamepadService.ConnectionState.DISCONNECTED -> {
                DisconnectedState(
                    onRequestDiscoverable = { vm.requestDiscoverability() },
                    pairedDevices = pairedDevices
                )
            }
            BluetoothGamepadService.ConnectionState.ERROR -> {
                DisconnectedState(
                    onRequestDiscoverable = { vm.requestDiscoverability() },
                    pairedDevices = pairedDevices
                )
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

// STATE-SPECIFIC CONTENT

@Composable
private fun ErrorInline(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
            Text(message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 15.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DisconnectedState(
    onRequestDiscoverable: () -> Unit,
    pairedDevices: List<BluetoothDevice>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ready to pair", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Make your phone discoverable, then pair from your Mac's Bluetooth settings.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 18.sp
        )

        Button(
            onClick = onRequestDiscoverable,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BluetoothSearching, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Text("Make Discoverable", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Previously Paired Macs - available even when disconnected for quick reconnect
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
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Previously Paired Macs", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                macHosts.forEach { device ->
                    val name = try { device.name ?: "Mac" } catch (e: SecurityException) { "Mac" }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRequestDiscoverable() } // Will trigger discoverability then allow quick connect
                            .padding(12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Laptop, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
        Text("Setting up…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
        PulsingBeacon()

        Text("Discoverable — waiting for Mac", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("How to Pair", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                PairingStepRow("1", "Open Bluetooth settings on your Mac")
                PairingStepRow("2", "Remove any old phone connection")
                PairingStepRow("3", "Find your phone name and click Connect")
            }
        }

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
                Text("Previously Paired Macs", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                macHosts.forEach { device ->
                    val name = try { device.name ?: "Mac" } catch (e: SecurityException) { "Mac" }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceClick(device) }
                            .padding(12.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Laptop, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
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
        // Connected status badge using Surface
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                Text("Connected to ${deviceName ?: "Mac"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        // Launch Controller button
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(22.dp))
                Text("Launch Controller", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onTertiary)
            }
        }

        // Disconnect button
        TextButton(onClick = onDisconnect) {
            Text("Disconnect", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
    }
}

// Pulsing beacon

@Composable
private fun PulsingBeacon() {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOut), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOut), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val beaconColor = MaterialTheme.colorScheme.tertiary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = beaconColor.copy(alpha = pulseAlpha), radius = (size.minDimension / 2f) * pulseScale, style = Stroke(2f))
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(beaconColor.copy(alpha = 0.15f))
                .border(1.5.dp, beaconColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bluetooth, null, tint = beaconColor, modifier = Modifier.size(12.dp))
        }
    }
}

// Numbered pairing step row

@Composable
private fun PairingStepRow(num: String, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

// Bluetooth Enable Screen

@Composable
fun BluetoothEnableScreen(
    onEnable: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backHandlerEnabled = remember { true }
    BackHandler(backHandlerEnabled) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg0, Bg1, Bg1))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth(0.88f).padding(vertical = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(Brush.radialGradient(listOf(AccentBlue.copy(alpha = 0.2f), Color.Transparent), center = Offset(44f, 44f), radius = 60f))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text("Bluetooth is Off", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, letterSpacing = (-0.5).sp)

            Spacer(Modifier.height(8.dp))

            Text("Please enable Bluetooth to use MacGamepad", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 18.sp)

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text("MacGamepad needs Bluetooth to be enabled to work as a gamepad controller for your Mac.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, lineHeight = 16.sp, modifier = Modifier.padding(20.dp))
            }

            Spacer(Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            Text("Enable Bluetooth", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}