## Purpose

Provides pure input throttling logic to prevent Bluetooth queue buildup by limiting report frequency to approximately 120 Hz (8ms minimum interval) while ensuring release transitions (buttons/sticks returning to neutral) are transmitted immediately without delay.

## ADDED Requirements

### Requirement: Throttle input reports to minimum interval

The system SHALL enforce a minimum time interval between consecutive input report transmissions to prevent Bluetooth queue saturation.

#### Scenario: Reports within throttle window are suppressed
- **WHEN** a new input state arrives within 8ms of the last transmitted report
- **AND** the new state does not represent a release transition
- **THEN** the new state is stored as pending and NOT transmitted immediately

#### Scenario: Reports outside throttle window are transmitted
- **WHEN** a new input state arrives 8ms or more after the last transmitted report
- **THEN** the new state is transmitted immediately
- **AND** the throttle timer resets to the current time

### Requirement: Detect release transitions for immediate transmission

The system SHALL identify when any input control transitions from active to neutral (released) and transmit immediately regardless of throttle window.

#### Scenario: Button release detected
- **WHEN** previous state has a button pressed (true) and new state has it released (false)
- **THEN** isReleaseTransition SHALL be true
- **AND** the state SHALL be transmitted immediately

#### Scenario: Stick axis return to zero detected
- **WHEN** previous state has a stick axis non-zero and new state has it at zero
- **THEN** isReleaseTransition SHALL be true
- **AND** the state SHALL be transmitted immediately

#### Scenario: Trigger return to zero detected
- **WHEN** previous state has a trigger non-zero and new state has it at zero
- **THEN** isReleaseTransition SHALL be true
- **AND** the state SHALL be transmitted immediately

#### Scenario: D-Pad return to center detected
- **WHEN** previous state has any D-Pad direction pressed and new state has all D-Pad directions released
- **THEN** isReleaseTransition SHALL be true
- **AND** the state SHALL be transmitted immediately

### Requirement: Trailing-edge delivery of throttled states

The system SHALL ensure that the most recent throttled state is not lost and gets transmitted on the next eligible transmission opportunity.

#### Scenario: Pending state sent on next eligible call
- **WHEN** a state was stored as pending due to throttle window
- **AND** a subsequent call occurs outside the throttle window
- **THEN** the pending state SHALL be transmitted first
- **AND** the new state SHALL be transmitted second
- **AND** pending state SHALL be cleared

#### Scenario: Latest state wins while pending
- **WHEN** multiple states arrive within the throttle window while one is pending
- **THEN** only the most recent state SHALL be kept as pending
- **AND** earlier pending states SHALL be discarded

### Requirement: Stateless pure function API

The throttling logic SHALL be implemented as a pure class with no Android framework dependencies, suitable for unit testing in isolation.

#### Scenario: Throttler can be instantiated and used without Android Context
- **WHEN** creating an InputThrottler instance
- **THEN** no Android dependencies SHALL be required
- **AND** all logic SHALL be deterministic given identical inputs

#### Scenario: Throttler maintains internal state across calls
- **WHEN** calling process() multiple times on the same instance
- **THEN** internal state (last report time, last state, pending state) SHALL persist between calls