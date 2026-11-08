package com.macgamepad.controller.bt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.macgamepad.controller.input.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * Manages the Bluetooth HID Device profile connection.
 *
 * This service:
 * 1. Registers the app as a Bluetooth HID Device (gamepad) with our HID descriptor
 * 2. Manages pairing and connection with the host (macOS)
 * 3. Sends HID input reports at high frequency via [sendReport]
 *
 * Uses Android's [BluetoothHidDevice] API (available since API 28).
 */
class BluetoothGamepadService(private val context: Context) : HidDeviceManager {

    companion object {
        private const val TAG = "BTGamepad"

        // Generic gamepad identifiers — no brand spoofing
        private const val DEVICE_NAME = "MacGamepad"
        private const val DEVICE_DESCRIPTION = "Virtual Gamepad Controller"
        private const val DEVICE_PROVIDER = "MacGamepad"

        // HID subclass for gamepad
        private const val HID_SUBCLASS_GAMEPAD: Byte = 0x02

        // Notification channel
        private const val CHANNEL_ID = "macgamepad_service_channel"
    }

    // ================================================================
    // Connection state
    // ================================================================
    // ConnectionState enum moved to bt/ConnectionState.kt
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    // Emits true once HID app registration is complete and ready for connections
    private val _registrationComplete = MutableStateFlow(false)
    override val registrationComplete: StateFlow<Boolean> = _registrationComplete.asStateFlow()

    // ================================================================
    // Bluetooth objects
    // ================================================================
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var hidDevice: BluetoothHidDevice? = null
    private var hostDevice: BluetoothDevice? = null
    private var originalBluetoothName: String? = null  // Save to restore on shutdown

    private val reportPacker = HidReportPacker()
    private val executor = Executors.newSingleThreadExecutor()

    // ================================================================
    // HID Device callback
    // ================================================================
    @SuppressLint("MissingPermission")
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "App status changed: registered=$registered, device=${pluggedDevice?.name}")
            if (registered) {
                _connectionState.value = ConnectionState.REGISTERED_WAITING
                _registrationComplete.value = true  // Signal registration complete
                Log.i(TAG, "✅ HID Device app registered. Waiting for host connection...")
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                _registrationComplete.value = false  // Reset on unregister
                Log.w(TAG, "⚠️ HID Device app unregistered")
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            val deviceName = try { device?.name ?: device?.address ?: "unknown" } catch (e: SecurityException) { "unknown" }
            Log.d(TAG, "Connection state changed: device=$deviceName, state=$state")

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    hostDevice = device
                    _connectedDeviceName.value = deviceName
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.i(TAG, "✅ Connected to host: $deviceName")
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                    Log.i(TAG, "🔄 Connecting to: $deviceName")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    hostDevice = null
                    _connectedDeviceName.value = null
                    _connectionState.value = ConnectionState.REGISTERED_WAITING
                    Log.i(TAG, "🔌 Disconnected from: $deviceName")
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.i(TAG, "🔌 Disconnecting from: $deviceName")
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "GET_REPORT: type=$type, id=$id, bufferSize=$bufferSize")
            // Respond with a zeroed-out report (all sticks centered, no buttons pressed)
            hidDevice?.replyReport(device, type, id, ByteArray(13))
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            Log.d(TAG, "SET_REPORT: type=$type, id=$id, data=${data?.size} bytes")
            // Future: handle force-feedback/rumble data from host
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            Log.d(TAG, "INTERRUPT_DATA: reportId=$reportId, data=${data?.size} bytes")
            // Future: handle output reports (LED control, etc.)
        }
    }

    // ================================================================
    // Profile service listener
    // ================================================================
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                Log.i(TAG, "HID Device profile proxy obtained")
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "HID Device profile disconnected")
            }
        }
    }

    // ================================================================
    // Public API
    // ================================================================

    /**
     * Initialize the Bluetooth HID Device service.
     * Call this once from your Activity/ViewModel after permissions are granted.
     */
    @SuppressLint("MissingPermission")
    override fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            _errorMessage.value = "Bluetooth not available on this device"
            _connectionState.value = ConnectionState.ERROR
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            _errorMessage.value = "Bluetooth is not enabled"
            _connectionState.value = ConnectionState.ERROR
            return
        }

        _connectionState.value = ConnectionState.REGISTERING
        Log.i(TAG, "Requesting HID Device profile proxy...")

        // Rename Bluetooth adapter to the target device profile name so it's recognized on the Mac
        try {
            originalBluetoothName = bluetoothAdapter?.name
            bluetoothAdapter?.name = targetDeviceName
            Log.i(TAG, "Renamed BT adapter from '$originalBluetoothName' to '$targetDeviceName'")
        } catch (e: Exception) {
            Log.w(TAG, "Could not rename BT adapter", e)
        }

        bluetoothAdapter?.getProfileProxy(
            context,
            serviceListener,
            BluetoothProfile.HID_DEVICE
        )
    }

    /**
     * Get a list of currently bonded (paired) devices that we can try to connect to.
     */
    @SuppressLint("MissingPermission")
    override fun getBondedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Attempt to programmatically connect to a specific paired device.
     * Useful when the Mac has already been paired previously.
     */
    @SuppressLint("MissingPermission")
    override fun connectToDevice(device: BluetoothDevice): Boolean {
        val hid = hidDevice ?: return false
        Log.i(TAG, "Attempting to connect to: ${device.name ?: device.address}")
        return hid.connect(device)
    }

    // Active mode configuration
    @Volatile
    private var activeMode: Mode = Mode.GAMEPAD

    /**
     * Set the mode of operation before calling initialize().
     */
    override fun setMode(mode: Mode) {
        activeMode = mode
    }

    /**
     * Send a raw HID report with a specific report ID (KEYBOARD/MOUSE modes).
     * Transmits [data] with the specified [reportId] via BluetoothHidDevice.
     *
     * @param reportId The HID report ID (e.g., 1 for keyboard, 2 for mouse)
     * @param data The report payload
     * @return true if the report was sent successfully, false if not connected
     */
    @SuppressLint("MissingPermission")
    override fun sendReport(reportId: Int, data: ByteArray): Boolean {
        val device = hostDevice ?: return false
        val hid = hidDevice ?: return false
        return hid.sendReport(device, reportId, data)
    }

    /**
     * Send the current input state as an HID report to the connected host (GAMEPAD mode).
     * This should be called at 125Hz (every 8ms) from a high-priority thread.
     *
     * @param state The current controller input state
     * @return true if the report was sent successfully, false if not connected
     */
    @SuppressLint("MissingPermission")
    override fun sendGamepadReport(state: InputState): Boolean {
        val device = hostDevice ?: return false
        val hid = hidDevice ?: return false

        val reportData = reportPacker.pack(state)
        // Use Report ID 1 — the gamepad collection in the gamepad-only descriptor
        return hid.sendReport(device, GamepadHidDescriptor.GAMEPAD_REPORT_ID.toInt(), reportData)
    }

    /**
     * Disconnect from the host and unregister the HID app.
     */
    @SuppressLint("MissingPermission")
    override fun shutdown() {
        try {
            hostDevice?.let { device ->
                hidDevice?.disconnect(device)
            }
            hidDevice?.unregisterApp()
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)

            // Restore original Bluetooth name
            originalBluetoothName?.let { origName ->
                bluetoothAdapter?.name = origName
                Log.i(TAG, "Restored BT adapter name to '$origName'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
        hidDevice = null
        hostDevice = null
        originalBluetoothName = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // Customizable Bluetooth display name to match target controllers
    @Volatile
    private var targetDeviceName = "MacGamepad"

    override fun setTargetDeviceName(name: String) {
        targetDeviceName = name
    }

    // ================================================================
    // Internal
    // ================================================================

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val hid = hidDevice ?: run {
            _errorMessage.value = "HID Device proxy not available"
            _connectionState.value = ConnectionState.ERROR
            return
        }

        val subclass: Byte = BluetoothHidDevice.SUBCLASS2_GAMEPAD
        val descriptor = GamepadHidDescriptor.descriptor

        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            targetDeviceName,
            DEVICE_DESCRIPTION,
            DEVICE_PROVIDER,
            subclass,
            descriptor
        )

        val result = hid.registerApp(
            sdpSettings,
            null,    // Let Bluetooth stack negotiate default stable QoS (fixes drops)
            null,    // Output QoS
            executor,
            hidCallback
        )

        if (result) {
            Log.i(TAG, "✅ registerApp() succeeded — waiting for callback...")
        } else {
            _errorMessage.value = "Failed to register HID app. Is Bluetooth HID Device profile supported?"
            _connectionState.value = ConnectionState.ERROR
            Log.e(TAG, "❌ registerApp() returned false")
        }
    }

    // Notification channel methods for connection alerts
    override fun recreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MacGamepad Connection Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Keeps Bluetooth controller connection alive in background"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun removeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.deleteNotificationChannel(CHANNEL_ID)
        }
    }
}
