package com.macgamepad.controller

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macgamepad.controller.bt.BluetoothGamepadService
import com.macgamepad.controller.bt.GamepadService
import com.macgamepad.controller.ui.screens.ConnectionScreen
import com.macgamepad.controller.ui.screens.DashboardScreen
import com.macgamepad.controller.ui.screens.GamepadScreen
import com.macgamepad.controller.ui.screens.ProfileScreen
import com.macgamepad.controller.ui.screens.SettingsScreen
import com.macgamepad.controller.ui.theme.MacGamepadTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), GamepadViewModel.PermissionCallback {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val bluetoothPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // API < 31: legacy permissions are install-time
            arrayOf()
        }

    private var viewModelRef: GamepadViewModel? = null
    private var gamepadService: GamepadService? = null
    private var isBound = false
    private var serviceCollectorJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Connected to GamepadService")
            val binder = service as GamepadService.LocalBinder
            val srv = binder.getService()
            gamepadService = srv
            isBound = true
            
            viewModelRef?.let { vm ->
                vm.setService(srv)
                startStateCollection(srv, vm)
                // Initialize Bluetooth after service is bound (permission flow already completed)
                vm.initializeBluetooth()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Disconnected from GamepadService")
            stopStateCollection()
            gamepadService = null
            isBound = false
            viewModelRef?.setService(null)
        }
    }

    private fun startStateCollection(srv: GamepadService, vm: GamepadViewModel) {
        stopStateCollection()
        serviceCollectorJob = lifecycleScope.launch {
            launch {
                srv.btService.connectionState.collectLatest { state ->
                    vm.updateServiceStates(state, srv.btService.connectedDeviceName.value, srv.btService.errorMessage.value)
                }
            }
            launch {
                srv.btService.connectedDeviceName.collectLatest { name ->
                    vm.updateServiceStates(srv.btService.connectionState.value, name, srv.btService.errorMessage.value)
                }
            }
            launch {
                srv.btService.errorMessage.collectLatest { err ->
                    vm.updateServiceStates(srv.btService.connectionState.value, srv.btService.connectedDeviceName.value, err)
                }
            }
        }
    }

    private fun stopStateCollection() {
        serviceCollectorJob?.cancel()
        serviceCollectorJob = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive mode configuration
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // No auto permission check or service binding on launch
        setContent {
            MacGamepadTheme {
                val vm: GamepadViewModel = viewModel()
                viewModelRef = vm
                vm.setPermissionCallback(this)

                // Sync service if bound late
                LaunchedEffect(gamepadService) {
                    gamepadService?.let { vm.setService(it) }
                }

                val currentScreen by vm.currentScreen.collectAsState()
                val connectionState by vm.connectionState.collectAsState()
                val connectedDevice by vm.connectedDeviceName.collectAsState()
                val errorMessage by vm.errorMessage.collectAsState()
                val activeProfileId by vm.activeProfileId.collectAsState()
                val mode by vm.mode.collectAsState()

                // Bind to service when permissions are granted
                val permissionState by vm.permissionState.collectAsState()
                LaunchedEffect(permissionState) {
                    if (permissionState is GamepadViewModel.PermissionStatus.Granted && !isBound && gamepadService == null) {
                        bindToService()
                    }
                }

                // Immersive landscape vs Portrait control matching
                LaunchedEffect(currentScreen) {
                    val targetOrientation = when (currentScreen) {
                        GamepadViewModel.Screen.GAMEPAD -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    if (requestedOrientation != targetOrientation) {
                        requestedOrientation = targetOrientation
                    }

                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    if (currentScreen == GamepadViewModel.Screen.GAMEPAD) {
                        insetsController.hide(WindowInsetsCompat.Type.systemBars())
                        insetsController.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                    },
                    label = "screenTransition"
                ) { screen ->
                    when (screen) {
                        GamepadViewModel.Screen.CONNECTION -> {
                            val pairedDevices by vm.pairedDevices.collectAsState()
                            ConnectionScreen(
                                connectionState = connectionState,
                                connectedDeviceName = connectedDevice,
                                errorMessage = errorMessage,
                                pairedDevices = pairedDevices,
                                onDeviceConnectClick = { vm.connectToDevice(it) },
                                onDisconnectClick = { vm.disconnect() },
                                onStartGamepad = { vm.navigateToGamepad() },
                                vm = vm,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        GamepadViewModel.Screen.DASHBOARD -> {
                            val sensitivity by vm.sensitivity.collectAsState()
                            val deadZone by vm.deadZone.collectAsState()
                            val gyroEnabled by vm.gyroEnabled.collectAsState()
                            DashboardScreen(
                                connectionState = connectionState,
                                connectedDeviceName = connectedDevice,
                                mode = mode,
                                onModeChange = { vm.setMode(it) },
                                activeProfileId = activeProfileId,
                                onProfileChange = { vm.setProfile(it) },
                                sensitivity = sensitivity,
                                onSensitivityChange = { vm.setSensitivity(it) },
                                deadZone = deadZone,
                                onDeadZoneChange = { vm.setDeadZone(it) },
                                gyroEnabled = gyroEnabled,
                                onGyroChange = { vm.setGyroEnabled(it) },
                                onEditLayoutClick = { vm.setLayoutEditMode(true) },
                                onStartGamepad = { vm.navigateToGamepad() },
                                onDisconnect = { vm.disconnect() },
                                onNavigateToProfile = { vm.navigateToProfile() },
                                onNavigateToSettings = { vm.navigateToSettings() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        GamepadViewModel.Screen.GAMEPAD -> {
                            val deadZone by vm.deadZone.collectAsState()
                            val isEditMode by vm.isLayoutEditMode.collectAsState()
                            val offsets by vm.componentOffsets.collectAsState()
                            val scales by vm.componentScales.collectAsState()

                            GamepadScreen(
                                profileId = activeProfileId,
                                onBackToDashboard = {
                                    if (isEditMode) vm.setLayoutEditMode(false)
                                    else vm.navigateToDashboard()
                                },
                                deadZone = deadZone,
                                isEditMode = isEditMode,
                                componentOffsets = offsets,
                                componentScales = scales,
                                onOffsetChanged = { id, offset -> vm.updateComponentOffset(id, offset) },
                                onScaleChanged = { id, scale -> vm.updateComponentScale(id, scale) },
                                onResetLayout = { vm.resetLayout() },
                                onSaveLayout = { vm.exitEditModeToGamepad() },
                                onEnterEditMode = { vm.setLayoutEditMode(true) },
                                modifier = Modifier.fillMaxSize(),
                                onInputChanged = { vm.onInputChanged(it) },
                                onMouseMoved = { dx, dy -> vm.onMouseMoved(dx, dy) },
                                onTouchpadClick = { vm.onTouchpadClick() }
                            )
                        }

                        GamepadViewModel.Screen.PROFILE -> {
                            ProfileScreen(
                                activeProfileId = activeProfileId,
                                onProfileChange = { vm.setProfile(it) },
                                connectionState = connectionState,
                                connectedDeviceName = connectedDevice,
                                onResetProfileLayout = { vm.resetLayout() },
                                onBack = { vm.navigateToDashboard() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        GamepadViewModel.Screen.SETTINGS -> {
                            val sensitivity by vm.sensitivity.collectAsState()
                            val deadZone by vm.deadZone.collectAsState()
                            val hapticEnabled by vm.hapticEnabled.collectAsState()
                            val hapticStrength by vm.hapticStrength.collectAsState()
                            val vibrationEnabled by vm.vibrationEnabled.collectAsState()
                            val autoReconnect by vm.autoReconnect.collectAsState()
                            val storageBytes = remember(sensitivity, deadZone) { vm.estimateStorageBytes() }
                            SettingsScreen(
                                sensitivity = sensitivity,
                                onSensitivityChange = { vm.setSensitivity(it) },
                                deadZone = deadZone,
                                onDeadZoneChange = { vm.setDeadZone(it) },
                                hapticEnabled = hapticEnabled,
                                onHapticEnabledChange = { vm.setHapticEnabled(it) },
                                hapticStrength = hapticStrength,
                                onHapticStrengthChange = { vm.setHapticsIntensity(it) },
                                vibrationEnabled = vibrationEnabled,
                                onVibrationChange = { vm.setVibrationEnabled(it) },
                                autoReconnect = autoReconnect,
                                onAutoReconnectChange = { vm.setAutoReconnect(it) },
                                mode = mode,
                                onModeChange = { vm.setMode(it) },
                                estimatedStorageBytes = storageBytes,
                                onClearAllLayouts = { vm.clearAllLayouts() },
                                onBack = { vm.navigateToDashboard() },
                                vm = vm,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to service only if permission flow is complete (granted + discoverability done)
        // ViewModel will call initializeBluetooth() which starts the session
        val vm = viewModelRef
        if (vm != null && vm.permissionState.value is GamepadViewModel.PermissionStatus.Granted) {
            // Check if discoverability was completed (service not bound yet)
            if (!isBound && gamepadService == null) {
                bindToService()
            }
        }
    }

    private fun bindToService() {
        Intent(this, GamepadService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "Permission request results: $results")
        viewModelRef?.onBluetoothPermissionsResult(results)
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Discoverability result code: ${result.resultCode}")
        viewModelRef?.onDiscoverabilityResult(result.resultCode)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "Notification permission result: $granted")
        viewModelRef?.onNotificationPermissionResult(granted)
    }

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Bluetooth enable result: ${result.resultCode}")
    }

    // PermissionCallback implementation
    override fun requestBluetoothPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }

    override fun requestDiscoverability() {
        val discoverableIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        discoverableLauncher.launch(discoverableIntent)
    }

    override fun requestNotificationPermission() {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun requestBluetoothEnable() {
        val enableIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher.launch(enableIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStateCollection()
    }
}
