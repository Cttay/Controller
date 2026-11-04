## MODIFIED Requirements

### Requirement: Efficient input change detection
The system SHALL detect meaningful input state changes using bitmask comparison instead of per-field conditionals, reducing CPU overhead on the UI thread.

#### Scenario: Button release detected via bitmask diff
- **WHEN** previous button bitmask differs from current button bitmask
- **THEN** an input report is sent immediately (release transition detected)

#### Scenario: Stick release detected via delta threshold
- **WHEN** a stick axis transitions from non-zero to zero (absolute delta > 0.01)
- **THEN** an input report is sent immediately

#### Scenario: Minimum 4ms interval between reports preserved
- **WHEN** rapid input changes occur (e.g., stick wiggle)
- **THEN** reports are throttled to maximum ~250Hz (4ms minimum interval) to prevent BT queue buildup

### Requirement: No false negatives on release transitions
The system SHALL reliably detect all button releases and stick returns-to-center without missing transitions.

#### Scenario: All button releases trigger report
- **WHEN** any button transitions from pressed to released
- **THEN** `onInputChanged` sends a report (subject to 4ms throttle)

#### Scenario: Stick return-to-center triggers report
- **WHEN** any stick axis transitions from non-zero to zero
- **THEN** `onInputChanged` sends a report (subject to 4ms throttle)