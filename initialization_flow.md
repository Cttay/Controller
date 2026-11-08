# Initialization Flow: All Possible Scenarios Before Dashboard

**Entry Point:** `startInitialization()` called from "Initialize Controller" button

## Flow Stages

| Stage | Condition | State Shown | User Action Required | Next Step |
| :--- | :--- | :--- | :--- | :--- |
| **1. Permission Check** | Never asked before (API 31+) | `RequestingPermissions` | System dialog: "Allow nearby devices?" | &rarr; `onBluetoothPermissionsResult()` |
| | Previously denied (rationale) | `RequestingPermissions` | System dialog with rationale | &rarr; `onBluetoothPermissionsResult()` |
| | Permanently denied ("Don't ask again") | `Error` | "Open Settings" button | Manual settings &rarr; retry |
| | Already granted | Skip to Stage 2 | N/A | &rarr; `proceedAfterPermissionsGranted()` |
| **2. Bluetooth Enable** | Bluetooth OFF | `RequestingBluetoothEnable` | System dialog: "Turn on Bluetooth?" | &rarr; `onBluetoothEnableResult()` |
| | Bluetooth ON | Skip to Stage 3 | N/A | &rarr; `ShowingDiscoverabilityScreen` |
| **3. Discoverability (Custom Screen)** | BT ON + Permissions granted | `ShowingDiscoverabilityScreen` | Two options:<br>• "Allow Discoverability"<br>• "Skip for now" | • &rarr; `RequestingDiscoverability` &rarr; system dialog<br>• &rarr; `initializeBluetooth()` (HID registration) |
| **4. System Discoverability** | User clicked "Allow" | `RequestingDiscoverability` | System dialog: "Visible for 300 sec?" | &rarr; `onDiscoverabilityResult()` |
| **5. Discoverability Result** | Allowed (`resultCode > 0`) | `Completed` | N/A | &rarr; `initializeBluetooth()` |
| | Denied/Cancelled | `Error` | "Try Again" button | &rarr; `startInitialization()` |
| **6. HID Registration** | Success | `Completed + REGISTERED_WAITING` | N/A | Wait for Mac connection |
| | Failed | `Error` | "Try Again" button | &rarr; `startInitialization()` |

---

## Additional Edge Cases

| Scenario | Triggered By | Behavior |
| :--- | :--- | :--- |
| **App kill during flow** | Process death | `onStart()` re-checks: if `permissionState=Granted` AND `initializationState=Completed` &rarr; binds service |
| **Back press on custom screen** | `BackHandler` consumes | Stays on screen (user must explicitly choose) |
| **Rapid re-taps** | Multiple clicks | Guard in `startInitialization()` prevents duplicate flows |
| **Permissions revoked in settings** | User revokes | `onResume()` re-checks &rarr; shows Error &rarr; "Open Settings" |
| **Bluetooth turned OFF mid-flow** | System toggle | Next `startInitialization()` &rarr; `RequestingBluetoothEnable` |

---

## State Machine Summary

### Idle
* &rarr; `RequestingPermissions` (system dialog)
  * &rarr; **Granted** &rarr; [Bluetooth OFF] &rarr; `RequestingBluetoothEnable` (system dialog)
    * &rarr; **Granted** &rarr; `ShowingDiscoverabilityScreen` (custom)
    * &rarr; **Denied** &rarr; `Error`
  * &rarr; **Granted** &rarr; [Bluetooth ON] &rarr; `ShowingDiscoverabilityScreen` (custom)
  * &rarr; **Denied (rationale)** &rarr; `Idle` (retry)
  * &rarr; **Denied (permanent)** &rarr; `Error` (settings)

### ShowingDiscoverabilityScreen
* &rarr; **"Allow"** &rarr; `RequestingDiscoverability` (system dialog)
  * &rarr; **Allowed** &rarr; `Completed` &rarr; `initializeBluetooth()` &rarr; HID registration
  * &rarr; **Denied** &rarr; `Error`
* &rarr; **"Skip"** &rarr; `Completed` &rarr; `initializeBluetooth()` &rarr; HID registration

### Completed
* &rarr; Dashboard screen
