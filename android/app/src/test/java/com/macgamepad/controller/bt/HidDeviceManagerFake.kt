package com.macgamepad.controller.bt

import android.bluetooth.BluetoothDevice
import com.macgamepad.controller.input.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake implementation of [HidDeviceManager] for testing.
 * Provides realistic state transitions without requiring Android Bluetooth stack.
 */
class HidDeviceManagerFake : HidDeviceManager {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    override val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage

    // Registration complete signal
    private val _registrationComplete = MutableStateFlow(false)
    override val registrationComplete: StateFlow<Boolean> = _registrationComplete

    private var initialized = false
    private var shutdown = false
    private var currentMode = Mode.GAMEPAD
    private var targetDeviceName = "MacGamepad"
    private val bondedDevices = mutableListOf<BluetoothDevice>()
    private val reportCount = AtomicInteger(0)

    override fun initialize() {
        if (shutdown) return
        initialized = true
        _connectionState.value = ConnectionState.REGISTERING
        _registrationComplete.value = false
        // Simulate async registration
        Executors.newSingleThreadExecutor().execute {
            Thread.sleep(10)
            if (!shutdown) {
                _connectionState.value = ConnectionState.REGISTERED_WAITING
                _registrationComplete.value = true
            }
        }
    }

    override fun shutdown() {
        shutdown = true
        initialized = false
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
        _registrationComplete.value = false
    }

    override fun sendGamepadReport(state: InputState): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        reportCount.incrementAndGet()
        return true
    }

    override fun sendReport(reportId: Int, data: ByteArray): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        reportCount.incrementAndGet()
        return true
    }

    override fun connectToDevice(device: BluetoothDevice): Boolean {
        if (_connectionState.value != ConnectionState.REGISTERED_WAITING) return false
        _connectionState.value = ConnectionState.CONNECTING
        Executors.newSingleThreadExecutor().execute {
            Thread.sleep(10)
            if (!shutdown) {
                _connectionState.value = ConnectionState.CONNECTED
                _connectedDeviceName.value = device.name
            }
        }
        return true
    }

    override fun getBondedDevices(): List<BluetoothDevice> {
        return bondedDevices.toList()
    }

    override fun setMode(mode: Mode) {
        currentMode = mode
    }

    override fun setTargetDeviceName(name: String) {
        targetDeviceName = name
    }

    override fun recreateNotificationChannel() {
        // No-op for testing
    }

    override fun removeNotificationChannel() {
        // No-op for testing
    }

    // Test helper methods
    fun simulateHostConnection(deviceName: String = "Test Mac") {
        if (_connectionState.value == ConnectionState.REGISTERED_WAITING) {
            _connectionState.value = ConnectionState.CONNECTING
            _connectedDeviceName.value = deviceName
            Executors.newSingleThreadExecutor().execute {
                Thread.sleep(5)
                if (!shutdown) {
                    _connectionState.value = ConnectionState.CONNECTED
                }
            }
        }
    }

    fun simulateHostDisconnect() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.REGISTERED_WAITING
            _connectedDeviceName.value = null
        }
    }

    fun addBondedDevice(device: BluetoothDevice) {
        bondedDevices.add(device)
    }

    fun getReportCount(): Int = reportCount.get()

    fun isInitialized(): Boolean = initialized
    fun isShutdown(): Boolean = shutdown
    fun getCurrentMode(): Mode = currentMode
    fun getTargetDeviceName(): String = targetDeviceName
}