package com.macgamepad.controller.bt

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow
import com.macgamepad.controller.input.InputState

/**
 * Interface for Bluetooth HID Device profile management.
 *
 * Provides a platform-agnostic abstraction over Android's [BluetoothHidDevice] API,
 * enabling testable connection lifecycle, report transmission, bonded device access,
 * and mode/profile configuration without coupling to Android framework implementation.
 */
interface HidDeviceManager {

    /**
     * Current connection state of the HID Device profile.
     * Emits: DISCONNECTED -> REGISTERING -> REGISTERED_WAITING -> CONNECTING -> CONNECTED
     * On disconnect: CONNECTED -> REGISTERED_WAITING
     * On error: any state -> ERROR
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Name of the currently connected host device.
     * Emits null when not connected.
     */
    val connectedDeviceName: StateFlow<String?>

    /**
     * Human-readable error message for UI display.
     * Emits null when no error condition exists.
     */
    val errorMessage: StateFlow<String?>

    /**
     * Emits true once the HID app registration is complete and ready for connections.
     * Use this to gate transmission loop startup and UI state collection.
     */
    val registrationComplete: StateFlow<Boolean>

    /**
     * Initialize the Bluetooth HID Device service.
     * Acquires the HID Device profile proxy and begins registration.
     * Must be called after Bluetooth permissions are granted.
     */
    fun initialize()

    /**
     * Shutdown the HID Device service.
     * Disconnects from host, unregisters HID app, releases profile proxy,
     * restores original Bluetooth adapter name, and emits DISCONNECTED.
     */
    fun shutdown()

    /**
     * Send a packed gamepad report (GAMEPAD mode).
     * Formats [InputState] per HID descriptor and transmits via BluetoothHidDevice.
     *
     * @param state The current controller input state
     * @return true if the report was sent successfully, false if not connected
     */
    fun sendGamepadReport(state: InputState): Boolean

    /**
     * Send a raw HID report (KEYBOARD/MOUSE modes).
     * Transmits [data] with the specified [reportId] via BluetoothHidDevice.
     *
     * @param reportId The HID report ID (e.g., 1 for keyboard, 2 for mouse)
     * @param data The report payload
     * @return true if the report was sent successfully, false if not connected
     */
    fun sendReport(reportId: Int, data: ByteArray): Boolean

    /**
     * Initiate connection to a specific bonded device.
     *
     * @param device The bonded Bluetooth device to connect to
     * @return true if the connection command was sent, false if HID Device proxy unavailable
     */
    fun connectToDevice(device: BluetoothDevice): Boolean

    /**
     * Get a list of currently bonded (paired) Bluetooth devices.
     *
     * @return List of paired devices suitable for manual connection.
     * NOTE: This is a synchronous call; UI observes bonded devices via
     * GamepadViewModel._pairedDevices StateFlow which snapshots the list on service binding.
     */
    fun getBondedDevices(): List<BluetoothDevice>

    /**
     * Set the HID operating mode (GAMEPAD, KEYBOARD, MOUSE).
     * Mode change takes effect on next re-registration (initialize()).
     *
     * @param mode The desired HID mode
     */
    fun setMode(mode: Mode)

    /**
     * Set the Bluetooth adapter display name for host recognition.
     * Called before initialize() to rename the adapter.
     *
     * @param name The target device name (e.g., "MacGamepad")
     */
    fun setTargetDeviceName(name: String)

    /**
     * Recreate the foreground service notification channel.
     * Called on mode/profile change to ensure channel exists with correct importance.
     */
    fun recreateNotificationChannel()

    /**
     * Remove the foreground service notification channel.
     * Called on shutdown to clean up.
     */
    fun removeNotificationChannel()
}