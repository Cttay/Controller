## Context

See proposal.md - Why. Current state:
- `AndroidManifest.xml` line 36: `android:foregroundServiceType="connectedDevice|dataSync"`
- `GamepadService.kt` line 121: Uses `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` correctly

## Goals / Non-Goals

**Goals:**
- Remove `dataSync` from manifest foreground service type declaration
- Ensure runtime `startForeground()` call matches (already correct)

**Non-Goals:**
- Change any other manifest declarations
- Modify service behavior

## Decisions

### 1. Simple manifest edit
**Change:** Line 36 from `connectedDevice|dataSync` to `connectedDevice`
**Rationale:** Single source of truth in manifest; runtime code already uses correct type

### 2. Verify runtime matches
**Location:** `GamepadService.kt` line 117-121
```kotlin
val notification = NotificationCompat.Builder(this, CHANNEL_ID)
    // ...
    .build()

startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
```
Already correct — no change needed.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Play Store rejects update if dataSync was previously declared | `connectedDevice` is the correct type for BT HID; removal is policy-compliant |
| Accidental removal of connectedDevice | Verify manifest change is exactly `connectedDevice` only |