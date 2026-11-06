package com.macgamepad.controller

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import android.bluetooth.BluetoothDevice
import androidx.core.content.ContextCompat
import android.Manifest
import com.macgamepad.controller.bt.BluetoothGamepadService
import com.macgamepad.controller.bt.GamepadService
import com.macgamepad.controller.input.InputState
import com.macgamepad.controller.input.InputThrottler
import com.macgamepad.controller.input.ThrottleResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset

/**
 * ViewModel managing the gamepad and UI state.
 *
 * Interacts with the background GamepadService to send input reports and get connection updates.
 */
class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "GamepadVM"
        private const val PREFS_NAME = "macgamepad_prefs"
        private const val KEY_PROFILE = "active_profile"
        private const val KEY_MODE = "active_mode"
        private val LAYOUT_COMPONENTS = listOf("leftStick", "rightStick", "dpad", "abxy", "shoulderL", "shoulderR", "sysButtons")
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Store Activity context for permission rationale checks (set from lifecycle observer)
    private var activityContext: androidx.activity.ComponentActivity? = null

    // ================================================================
    // Layout pref key helpers — NAMESPACED BY PROFILE so each profile
    // has its own independent layout. This was the root cause of the
    // "one layout affects all controllers" bug.
    // ================================================================
    private fun offsetXKey(profileId: String, componentId: String) = "ly_${profileId}_ox_$componentId"
    private fun offsetYKey(profileId: String, componentId: String) = "ly_${profileId}_oy_$componentId"
    private fun scaleKey(profileId: String, componentId: String)   = "ly_${profileId}_sc_$componentId"

    // ================================================================
    // Navigation state
    // ================================================================
    enum class Screen { CONNECTION, DASHBOARD, GAMEPAD, PROFILE, SETTINGS }

    private val _currentScreen = MutableStateFlow(Screen.CONNECTION)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // ================================================================
    // Active controller profile skin
    // ================================================================
    private val _activeProfileId = MutableStateFlow(prefs.getString(KEY_PROFILE, "ps5") ?: "ps5")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    // ================================================================
    // Active service reference (bound from MainActivity)
    // ================================================================
    private val _gamepadService = MutableStateFlow<GamepadService?>(null)
    val gamepadService: StateFlow<GamepadService?> = _gamepadService.asStateFlow()

    // ================================================================
    // Flows mapping connection states directly from GamepadService
    // ================================================================
    private val _connectionState = MutableStateFlow(BluetoothGamepadService.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothGamepadService.ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _mode = MutableStateFlow(GamepadService.Companion.Mode.GAMEPAD)
    val mode: StateFlow<GamepadService.Companion.Mode> = _mode.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    // Persistent configuration flows
    private val _sensitivity = MutableStateFlow(prefs.getFloat("pref_sensitivity", 1.0f))
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _deadZone = MutableStateFlow(prefs.getFloat("pref_deadzone", 0.15f))
    val deadZone: StateFlow<Float> = _deadZone.asStateFlow()

    private val _hapticStrength = MutableStateFlow(prefs.getFloat("pref_haptic", 0.8f))
    val hapticStrength: StateFlow<Float> = _hapticStrength.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean("pref_haptic_enabled", true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _gyroEnabled = MutableStateFlow(prefs.getBoolean("pref_gyro", false))
    val gyroEnabled: StateFlow<Boolean> = _gyroEnabled.asStateFlow()

    private val _autoReconnect = MutableStateFlow(prefs.getBoolean("pref_auto_reconnect", false))
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean("pref_vibration", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // ================================================================
    // Permission State Machine
    // ================================================================
    sealed interface PermissionStatus {
        object Granted : PermissionStatus
        data class Denied(val shouldShowRationale: Boolean, val missingPermissions: List<String>) : PermissionStatus
        data class PermanentlyDenied(val missingPermissions: List<String>) : PermissionStatus
        object Unknown : PermissionStatus
    }

    private val _permissionState = MutableStateFlow<PermissionStatus>(PermissionStatus.Unknown)
    val permissionState: StateFlow<PermissionStatus> = _permissionState.asStateFlow()

    private val _isRequestingPermissions = MutableStateFlow(false)
    val isRequestingPermissions: StateFlow<Boolean> = _isRequestingPermissions.asStateFlow()

    private var hasDoneInitialPermissionCheck = false

    // Persisted flag to distinguish first-time denial from "Don't ask again"
    private var hasEverRequestedBluetoothPermissions: Boolean
        get() = prefs.getBoolean("pref_bt_perm_requested", false)
        set(value) = prefs.edit().putBoolean("pref_bt_perm_requested", value).apply()

    // Notification opt-in preference
    private val _connectionAlertsEnabled = MutableStateFlow(prefs.getBoolean("pref_connection_alerts", false))
    val connectionAlertsEnabled: StateFlow<Boolean> = _connectionAlertsEnabled.asStateFlow()

    // Bluetooth permissions required for HID Device profile (API 31+)
    private val bluetoothPermissions = listOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
    )

    // Permission callback interface for MainActivity
    interface PermissionCallback {
        fun requestBluetoothPermissions(permissions: Array<String>)
        fun requestDiscoverability()
        fun requestNotificationPermission()
        fun requestBluetoothEnable()
        fun bindGamepadService()
        fun onDiscoverabilityScreenAllow()
    }

    // Tracks initialization sequence state
    private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.Idle)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    sealed interface InitializationState {
        object Idle : InitializationState
        data class RequestingPermissions(val missingPermissions: List<String>) : InitializationState
        object RequestingBluetoothEnable : InitializationState
        object ShowingDiscoverabilityScreen : InitializationState
        object RequestingDiscoverability : InitializationState
        object Completed : InitializationState
        data class Error(val message: String) : InitializationState
    }

    private var permissionCallback: PermissionCallback? = null

    fun setPermissionCallback(callback: PermissionCallback) {
        permissionCallback = callback
    }

    init {
        // Register lifecycle observer for permission re-check on resume
        (getApplication() as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (!hasDoneInitialPermissionCheck) {
                    hasDoneInitialPermissionCheck = true
                }
                // owner is the Activity, store it for proper rationale checks
                if (owner is androidx.activity.ComponentActivity) {
                    activityContext = owner
                }
                checkAndUpdatePermissionState(owner as Context)
            }
        })
    }

    fun setService(service: GamepadService?) {
        _gamepadService.value = service
        if (service != null) {
            Log.i(TAG, "Service reference acquired by ViewModel")
            _connectionState.value = service.btService.connectionState.value
            _connectedDeviceName.value = service.btService.connectedDeviceName.value
            _errorMessage.value = service.btService.errorMessage.value
            _pairedDevices.value = service.btService.getBondedDevices()

            val savedMode = prefs.getString(KEY_MODE, GamepadService.Companion.Mode.GAMEPAD.name)
                ?.let { GamepadService.Companion.Mode.valueOf(it) }
                ?: GamepadService.Companion.Mode.GAMEPAD
            _mode.value = savedMode
            service.setMode(savedMode)

            service.updatePreferences(_sensitivity.value, _deadZone.value, _gyroEnabled.value)
            updateTargetDeviceName()
        }
    }

    private fun updateTargetDeviceName() {
        val name = when (_mode.value) {
            GamepadService.Companion.Mode.GAMEPAD -> "MacGamepad"
            GamepadService.Companion.Mode.KEYBOARD -> "MacGamepad Keyboard"
        }
        Log.i(TAG, "Setting target Bluetooth device name to: $name")
        _gamepadService.value?.btService?.setTargetDeviceName(name)
    }

    /**
     * Update connection state flows (called by MainActivity ServiceConnection).
     */
    fun updateServiceStates(
        connState: BluetoothGamepadService.ConnectionState,
        devName: String?,
        error: String?
    ) {
        _connectionState.value = connState
        _connectedDeviceName.value = devName
        _errorMessage.value = error

        _gamepadService.value?.let { service ->
            _pairedDevices.value = service.btService.getBondedDevices()
        }

        // On connect: jump from Connection screen to Dashboard
        if (connState == BluetoothGamepadService.ConnectionState.CONNECTED && _currentScreen.value == Screen.CONNECTION) {
            _currentScreen.value = Screen.DASHBOARD
        }
        // On disconnect: return to Connection from Gamepad only (Dashboard/Profile/Settings stay visible)
        if (connState != BluetoothGamepadService.ConnectionState.CONNECTED && _currentScreen.value == Screen.GAMEPAD) {
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    // ================================================================
    // Public API
    // ================================================================

    private var isInitializingBluetooth = false

    fun initializeBluetooth() {
        // Guard against double initialization
        if (isInitializingBluetooth) {
            Log.w(TAG, "initializeBluetooth called while already initializing, skipping")
            return
        }
        val service = _gamepadService.value
        if (service != null) {
            isInitializingBluetooth = true
            updateTargetDeviceName()
            service.startSession()
            // Reset flag after session starts (HID registration is async)
            // We'll reset when connection state changes or after timeout
            viewModelScope.launch {
                delay(3000)
                isInitializingBluetooth = false
            }
        } else {
            Log.w(TAG, "Cannot initialize BT, service is null")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        _gamepadService.value?.btService?.connectToDevice(device)
    }

    fun disconnect() {
        _gamepadService.value?.stopSession()
        _currentScreen.value = Screen.CONNECTION
    }

    fun setMode(newMode: GamepadService.Companion.Mode) {
        _mode.value = newMode
        prefs.edit().putString(KEY_MODE, newMode.name).apply()
        _gamepadService.value?.let { service ->
            updateTargetDeviceName()
            service.setMode(newMode)
        }
    }

    fun setProfile(profileId: String) {
        val changed = _activeProfileId.value != profileId
        _activeProfileId.value = profileId
        prefs.edit().putString(KEY_PROFILE, profileId).apply()
        // Reload layouts for the new profile — critical for per-profile independence
        if (changed) {
            _componentOffsets.value = loadComponentOffsets(profileId)
            _componentScales.value = loadComponentScales(profileId)
        }
        _gamepadService.value?.let { service ->
            updateTargetDeviceName()
            val currState = service.btService.connectionState.value
            if (changed && currState == BluetoothGamepadService.ConnectionState.REGISTERED_WAITING) {
                Log.i(TAG, "Restarting discoverable session for profile: $profileId")
                service.stopSession()
                service.startSession()
            }
        }
    }

    fun navigateToGamepad() {
        if (_connectionState.value == BluetoothGamepadService.ConnectionState.CONNECTED) {
            _currentScreen.value = Screen.GAMEPAD
        }
    }

    fun navigateToDashboard() { _currentScreen.value = Screen.DASHBOARD }
    fun navigateToProfile()   { _currentScreen.value = Screen.PROFILE   }
    fun navigateToSettings()  { _currentScreen.value = Screen.SETTINGS  }

    fun setSensitivity(value: Float) {
        _sensitivity.value = value
        prefs.edit().putFloat("pref_sensitivity", value).apply()
        _gamepadService.value?.updatePreferences(value, _deadZone.value, _gyroEnabled.value)
    }

    fun setDeadZone(value: Float) {
        _deadZone.value = value
        prefs.edit().putFloat("pref_deadzone", value).apply()
        _gamepadService.value?.updatePreferences(_sensitivity.value, value, _gyroEnabled.value)
    }

    fun setHapticsIntensity(value: Float) {
        _hapticStrength.value = value
        prefs.edit().putFloat("pref_haptic", value).apply()
    }

    fun setHapticEnabled(value: Boolean) {
        _hapticEnabled.value = value
        prefs.edit().putBoolean("pref_haptic_enabled", value).apply()
    }

    fun setGyroEnabled(value: Boolean) {
        _gyroEnabled.value = value
        prefs.edit().putBoolean("pref_gyro", value).apply()
        _gamepadService.value?.updatePreferences(_sensitivity.value, _deadZone.value, value)
    }

    fun setAutoReconnect(value: Boolean) {
        _autoReconnect.value = value
        prefs.edit().putBoolean("pref_auto_reconnect", value).apply()
    }

    fun setVibrationEnabled(value: Boolean) {
        _vibrationEnabled.value = value
        prefs.edit().putBoolean("pref_vibration", value).apply()
    }

    // Layout Edit Mode state
    private val _isLayoutEditMode = MutableStateFlow(false)
    val isLayoutEditMode: StateFlow<Boolean> = _isLayoutEditMode.asStateFlow()

    // Per-profile component offsets and scales — loaded fresh each time profile changes
    private val _componentOffsets = MutableStateFlow<Map<String, Offset>>(loadComponentOffsets(_activeProfileId.value))
    val componentOffsets: StateFlow<Map<String, Offset>> = _componentOffsets.asStateFlow()

    private val _componentScales = MutableStateFlow<Map<String, Float>>(loadComponentScales(_activeProfileId.value))
    val componentScales: StateFlow<Map<String, Float>> = _componentScales.asStateFlow()

    /** Called from Dashboard 'Edit Layout' — navigates to Gamepad in edit mode */
    fun setLayoutEditMode(enabled: Boolean) {
        _isLayoutEditMode.value = enabled
        if (enabled) {
            _currentScreen.value = Screen.GAMEPAD
        } else {
            _currentScreen.value = Screen.DASHBOARD
        }
    }

    /** Called from inside the controller's ✓ button — exits edit mode, stays on Gamepad */
    fun exitEditModeToGamepad() {
        _isLayoutEditMode.value = false
        // Intentionally do NOT change _currentScreen
    }

    fun updateComponentOffset(componentId: String, offset: Offset) {
        val updated = _componentOffsets.value.toMutableMap()
        updated[componentId] = offset
        _componentOffsets.value = updated
        // Store with profile-namespaced key
        prefs.edit()
            .putFloat(offsetXKey(_activeProfileId.value, componentId), offset.x)
            .putFloat(offsetYKey(_activeProfileId.value, componentId), offset.y)
            .apply()
    }

    fun updateComponentScale(componentId: String, scale: Float) {
        val updated = _componentScales.value.toMutableMap()
        updated[componentId] = scale
        _componentScales.value = updated
        prefs.edit().putFloat(scaleKey(_activeProfileId.value, componentId), scale).apply()
    }

    fun resetLayout() {
        _componentOffsets.value = emptyMap()
        _componentScales.value = emptyMap()
        val pid = _activeProfileId.value
        val editor = prefs.edit()
        LAYOUT_COMPONENTS.forEach { id ->
            editor.remove(offsetXKey(pid, id))
            editor.remove(offsetYKey(pid, id))
            editor.remove(scaleKey(pid, id))
        }
        editor.apply()
    }

    /** Clears ALL layout data for ALL profiles — used in Settings > Clear Cache */
    fun clearAllLayouts() {
        _componentOffsets.value = emptyMap()
        _componentScales.value = emptyMap()
        val editor = prefs.edit()
        listOf("ps5", "xbox", "switch").forEach { pid ->
            LAYOUT_COMPONENTS.forEach { id ->
                editor.remove(offsetXKey(pid, id))
                editor.remove(offsetYKey(pid, id))
                editor.remove(scaleKey(pid, id))
            }
        }
        editor.apply()
    }

    /** Returns approximate storage used by this app's shared preferences (bytes) */
    fun estimateStorageBytes(): Int {
        return prefs.all.entries.sumOf { (key, value) ->
            key.length * 2 + when (value) {
                is Float   -> 4
                is Boolean -> 1
                is String  -> (value.length * 2)
                else       -> 8
            }
        }
    }

    private fun loadComponentOffsets(profileId: String): Map<String, Offset> {
        val map = mutableMapOf<String, Offset>()
        LAYOUT_COMPONENTS.forEach { id ->
            val x = prefs.getFloat(offsetXKey(profileId, id), 0f)
            val y = prefs.getFloat(offsetYKey(profileId, id), 0f)
            if (x != 0f || y != 0f) map[id] = Offset(x, y)
        }
        return map
    }

    private fun loadComponentScales(profileId: String): Map<String, Float> {
        val map = mutableMapOf<String, Float>()
        LAYOUT_COMPONENTS.forEach { id ->
            val scale = prefs.getFloat(scaleKey(profileId, id), 1.0f)
            if (scale != 1.0f) map[id] = scale
        }
        return map
    }

    // Throttle input reports to prevent BT queue buildup (default 4ms ≈ 250 Hz)
    private val inputThrottler = InputThrottler()

    fun onInputChanged(state: InputState) {
        val result = inputThrottler.process(state)
        when (result) {
            is ThrottleResult.Transmit -> {
                // Transmit pending state first (trailing-edge delivery)
                result.pendingState?.let {
                    _gamepadService.value?.updateInputState(it)
                }
                // Then transmit current state
                _gamepadService.value?.updateInputState(result.state)
            }
            is ThrottleResult.Pending -> {
                // State held as pending, do nothing
            }
        }
    }

    fun onMouseMoved(dx: Float, dy: Float) {
        _gamepadService.value?.sendMouseMovement(dx, dy)
    }

    fun onTouchpadClick() {
        _gamepadService.value?.triggerMouseClick()
    }

    // ================================================================
    // Permission Flow API
    // ================================================================

    private fun checkAndUpdatePermissionState(context: Context) {
        _permissionState.value = checkBluetoothPermissions(context)
    }

    fun checkBluetoothPermissions(context: Context): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // API < 31: legacy permissions are install-time, consider granted if app runs
            return PermissionStatus.Granted
        }

        val missingPermissions = mutableListOf<String>()
        var anyRationale = false
        var anyPermanentDenial = false

        for (perm in bluetoothPermissions) {
            val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                missingPermissions.add(perm)
                // Only check rationale if context is an Activity
                val rationale = if (context is androidx.activity.ComponentActivity) {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context, perm)
                } else {
                    false // Can't check rationale without Activity
                }
                if (rationale) {
                    anyRationale = true
                } else if (hasEverRequestedBluetoothPermissions) {
                    anyPermanentDenial = true
                }
            }
        }

        return when {
            missingPermissions.isEmpty() -> PermissionStatus.Granted
            anyPermanentDenial -> PermissionStatus.PermanentlyDenied(missingPermissions)
            else -> PermissionStatus.Denied(anyRationale, missingPermissions)
        }
    }

    fun requestBluetoothPermissions(missingPermissions: List<String> = bluetoothPermissions) {
        if (_isRequestingPermissions.value) return
        _isRequestingPermissions.value = true
        hasEverRequestedBluetoothPermissions = true
        permissionCallback?.requestBluetoothPermissions(missingPermissions.toTypedArray())
    }

    

    fun requestDiscoverability() {
        permissionCallback?.requestDiscoverability()
    }

    fun onDiscoverabilityResult(resultCode: Int) {
        // On some devices, discoverability returns the duration (e.g., 300) as success code
        // RESULT_OK is -1, but positive values also indicate success (duration in seconds)
        val isSuccess = resultCode == android.app.Activity.RESULT_OK || resultCode > 0
        if (isSuccess) {
            Log.i(TAG, "Discoverability granted (resultCode=$resultCode), proceeding to HID initialization")
            _initializationState.value = InitializationState.Completed
            initializeBluetooth()
        } else {
            // User cancelled discoverability - show error, don't proceed
            Log.w(TAG, "Discoverability cancelled/denied: $resultCode")
            _initializationState.value = InitializationState.Error("Discoverability is required to connect. Please retry.")
        }
    }

    fun onDiscoverabilitySkipped() {
        Log.i(TAG, "Discoverability skipped, proceeding to HID initialization")
        _initializationState.value = InitializationState.Completed
        // Only initialize if not already initializing
        if (!isInitializingBluetooth) {
            initializeBluetooth()
        }
    }

    fun openAppSettings() {
        val context = getApplication() as Context
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // Public method for UI to trigger permission re-check
    fun recheckPermissions() {
        // Need an Activity context for rationale checks - use lifecycle owner
        val context = getApplication() as Context
        // This will use the Activity when called from onResume, otherwise just check grants
        checkAndUpdatePermissionState(context)
    }

    // Public method to reset initialization state (e.g., on back press from discoverability screen)
    fun resetInitialization() {
        _initializationState.value = InitializationState.Idle
    }

    // Bluetooth enable prompt (used by initialization flow)
    fun requestBluetoothEnable() {
        permissionCallback?.requestBluetoothEnable()
    }

    // Integrated initialization flow: permissions → BT enable → discoverable → HID
    fun startInitialization() {
        // Guard against rapid re-taps
        if (_initializationState.value != InitializationState.Idle && 
            _initializationState.value !is InitializationState.Error) {
            return
        }
        
        // Use stored Activity context for permission rationale checks, fallback to Application
        val context = activityContext ?: getApplication<Application>() as Context
        // First update the permissionState flow that UI observes
        checkAndUpdatePermissionState(context)
        val currentPermissionState = _permissionState.value
        
        when (currentPermissionState) {
            is PermissionStatus.Unknown, is PermissionStatus.Denied -> {
                // Need to request permissions first
                _initializationState.value = InitializationState.RequestingPermissions(
                    missingPermissions = bluetoothPermissions
                )
                requestBluetoothPermissions(bluetoothPermissions)
            }
            is PermissionStatus.PermanentlyDenied -> {
                _initializationState.value = InitializationState.Error(
                    "Bluetooth permissions are required. Please enable them in Settings."
                )
            }
            is PermissionStatus.Granted -> {
                // Permissions granted, check Bluetooth state
                proceedAfterPermissionsGranted()
            }
        }
    }

    private fun proceedAfterPermissionsGranted() {
        // Check if Bluetooth is enabled
        val app = getApplication() as android.app.Application
        val bluetoothManager = app.getSystemService(android.bluetooth.BluetoothManager::class.java) as android.bluetooth.BluetoothManager
        val adapter = bluetoothManager.adapter
        
        if (adapter?.isEnabled == true) {
            // Bluetooth is on, show custom discoverability screen first
            _initializationState.value = InitializationState.ShowingDiscoverabilityScreen
            permissionCallback?.bindGamepadService()
        } else {
            // Need to enable Bluetooth first
            _initializationState.value = InitializationState.RequestingBluetoothEnable
            requestBluetoothEnable()
        }
    }

    // Called from MainActivity after permission result
    fun onBluetoothPermissionsResult(results: Map<String, Boolean>) {
        _isRequestingPermissions.value = false
        val context = getApplication<Application>() as Context
        val missingAfterRequest = mutableListOf<String>()
        var anyRationale = false
        var anyPermanentDenial = false

        for ((perm, granted) in results) {
            if (!granted && perm in bluetoothPermissions) {
                missingAfterRequest.add(perm)
                // Only check rationale if context is an Activity
                val rationale = if (context is androidx.activity.ComponentActivity) {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(context, perm)
                } else {
                    false
                }
                if (rationale) {
                    anyRationale = true
                } else if (hasEverRequestedBluetoothPermissions) {
                    anyPermanentDenial = true
                }
            }
        }

        _permissionState.value = when {
            missingAfterRequest.isEmpty() -> PermissionStatus.Granted
            anyPermanentDenial -> PermissionStatus.PermanentlyDenied(missingAfterRequest)
            else -> PermissionStatus.Denied(anyRationale, missingAfterRequest)
        }

        // If permissions granted, continue initialization flow
        if (_permissionState.value is PermissionStatus.Granted) {
            proceedAfterPermissionsGranted()
        } else {
            // Reset initialization state on failure
            _initializationState.value = InitializationState.Idle
        }
    }

    // Called from MainActivity after Bluetooth enable result
    fun onBluetoothEnableResult(resultCode: Int) {
        if (resultCode == android.app.Activity.RESULT_OK) {
            // Bluetooth enabled, show custom discoverability screen
            _initializationState.value = InitializationState.ShowingDiscoverabilityScreen
            permissionCallback?.bindGamepadService()
        } else {
            // User denied Bluetooth enable
            _initializationState.value = InitializationState.Error("Bluetooth must be enabled to continue.")
        }
    }

    // Called from MainActivity when user clicks "Allow Discoverability" on custom screen
    fun onDiscoverabilityScreenAllow() {
        _initializationState.value = InitializationState.RequestingDiscoverability
        requestDiscoverability()
    }

    // ================================================================
    // Notification Opt-In API
    // ================================================================

    fun requestNotificationPermission() {
        permissionCallback?.requestNotificationPermission()
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        if (granted) {
            _connectionAlertsEnabled.value = true
            prefs.edit().putBoolean("pref_connection_alerts", true).apply()
            // Recreate notification channel if service is running
            _gamepadService.value?.btService?.recreateNotificationChannel()
        } else {
            _connectionAlertsEnabled.value = false
            prefs.edit().putBoolean("pref_connection_alerts", false).apply()
        }
    }

    fun setConnectionAlertsEnabled(enabled: Boolean) {
        if (enabled) {
            // Check current permission state first
            val context = getApplication() as Context
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                _connectionAlertsEnabled.value = true
                prefs.edit().putBoolean("pref_connection_alerts", true).apply()
                _gamepadService.value?.btService?.recreateNotificationChannel()
            } else {
                requestNotificationPermission()
            }
        } else {
            _connectionAlertsEnabled.value = false
            prefs.edit().putBoolean("pref_connection_alerts", false).apply()
            _gamepadService.value?.btService?.removeNotificationChannel()
        }
    }

    fun syncNotificationPermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val context = getApplication() as Context
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted != _connectionAlertsEnabled.value) {
                _connectionAlertsEnabled.value = granted
                prefs.edit().putBoolean("pref_connection_alerts", granted).apply()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }
}
