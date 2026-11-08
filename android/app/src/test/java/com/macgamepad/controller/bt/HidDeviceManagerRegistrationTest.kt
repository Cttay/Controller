package com.macgamepad.controller.bt

import android.bluetooth.BluetoothDevice
import com.macgamepad.controller.input.InputState
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HidDeviceManagerRegistrationTest {

    private lateinit var fake: HidDeviceManagerFake

    @Before
    fun setUp() {
        fake = HidDeviceManagerFake()
    }

    @Test
    fun initialize_emitsRegisteringThenRegisteredWaiting() {
        // Initial state
        Assert.assertEquals(ConnectionState.DISCONNECTED, fake.connectionState.value)

        // Initialize
        fake.initialize()

        // Should immediately emit REGISTERING
        Assert.assertEquals(ConnectionState.REGISTERING, fake.connectionState.value)

        // Wait for async transition to REGISTERED_WAITING
        Thread.sleep(50)

        Assert.assertEquals(ConnectionState.REGISTERED_WAITING, fake.connectionState.value)
        Assert.assertTrue(fake.isInitialized())
    }

    @Test
    fun shutdown_emitsDisconnected() {
        fake.initialize()
        Thread.sleep(50) // Wait for REGISTERED_WAITING
        Assert.assertEquals(ConnectionState.REGISTERED_WAITING, fake.connectionState.value)

        fake.shutdown()

        Assert.assertEquals(ConnectionState.DISCONNECTED, fake.connectionState.value)
        Assert.assertNull(fake.connectedDeviceName.value)
        Assert.assertTrue(fake.isShutdown())
    }

    @Test
    fun sendGamepadReport_returnsFalse_whenNotConnected() {
        fake.initialize()
        Thread.sleep(50) // Wait for REGISTERED_WAITING

        val state = InputState()
        val result = fake.sendGamepadReport(state)

        Assert.assertFalse(result)
    }

    @Test
    fun sendGamepadReport_returnsTrue_whenConnected() {
        fake.initialize()
        Thread.sleep(50)

        // Simulate host connection
        fake.simulateHostConnection("Test Mac")
        Thread.sleep(20)

        Assert.assertEquals(ConnectionState.CONNECTED, fake.connectionState.value)

        val state = InputState()
        val result = fake.sendGamepadReport(state)

        Assert.assertTrue(result)
        Assert.assertEquals(1, fake.getReportCount())
    }

    @Test
    fun sendReport_returnsFalse_whenNotConnected() {
        fake.initialize()
        Thread.sleep(50)

        val result = fake.sendReport(1, ByteArray(13))

        Assert.assertFalse(result)
    }

    @Test
    fun connectToDevice_transitionsToConnectingThenConnected() {
        fake.initialize()
        Thread.sleep(50)

        // Use a real BluetoothDevice - create via reflection
        val mockDevice = createMockBluetoothDevice()

        val result = fake.connectToDevice(mockDevice)

        Assert.assertTrue(result)
        Assert.assertEquals(ConnectionState.CONNECTING, fake.connectionState.value)

        Thread.sleep(20)

        Assert.assertEquals(ConnectionState.CONNECTED, fake.connectionState.value)
        // Device name from reflection mock may be null, just verify connected
    }

    @Test
    fun getBondedDevices_returnsEmptyList_initially() {
        fake.initialize()
        Thread.sleep(50)

        val devices = fake.getBondedDevices()

        Assert.assertTrue(devices.isEmpty())
    }

    @Test
    fun setMode_updatesCurrentMode() {
        fake.setMode(Mode.KEYBOARD)
        Assert.assertEquals(Mode.KEYBOARD, fake.getCurrentMode())

        fake.setMode(Mode.MOUSE)
        Assert.assertEquals(Mode.MOUSE, fake.getCurrentMode())
    }

    @Test
    fun setTargetDeviceName_updatesName() {
        fake.setTargetDeviceName("Custom Name")
        Assert.assertEquals("Custom Name", fake.getTargetDeviceName())
    }

    private fun createMockBluetoothDevice(): BluetoothDevice {
        // Use reflection to create a mock BluetoothDevice
        val clazz = Class.forName("android.bluetooth.BluetoothDevice")
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        return constructor.newInstance() as BluetoothDevice
    }
}