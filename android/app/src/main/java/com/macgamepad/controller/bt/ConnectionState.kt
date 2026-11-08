package com.macgamepad.controller.bt

/**
 * Represents the connection state of the Bluetooth HID Device profile.
 */
enum class ConnectionState {
    /** No connection established, HID app not registered */
    DISCONNECTED,
    /** Registering the HID Device app with the system */
    REGISTERING,
    /** HID app registered, waiting for host to connect */
    REGISTERED_WAITING,
    /** Host is connecting to the HID Device profile */
    CONNECTING,
    /** Host is connected and reports can be sent */
    CONNECTED,
    /** Unrecoverable error (Bluetooth unavailable, permissions denied) */
    ERROR
}