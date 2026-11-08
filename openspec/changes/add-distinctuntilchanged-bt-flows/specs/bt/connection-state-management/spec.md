## MODIFIED Requirements

### Requirement: Deduplicated connection state propagation

The system SHALL emit connection state, device name, and error message changes only when the value actually changes, eliminating redundant emissions through the 4-layer StateFlow chain.

#### Scenario: connectionState emits only on actual state change
- **WHEN** internal `_connectionState` value is set to the same `ConnectionState` as current
- **THEN** downstream collectors SHALL NOT receive a new emission
- **AND** when value changes to a different `ConnectionState`, downstream SHALL receive exactly one emission

#### Scenario: connectedDeviceName emits only on actual name change
- **WHEN** internal `_connectedDeviceName` value is set to the same string (or null) as current
- **THEN** downstream collectors SHALL NOT receive a new emission
- **AND** when value changes to a different string, downstream SHALL receive exactly one emission

#### Scenario: errorMessage emits only on actual error change
- **WHEN** internal `_errorMessage` value is set to the same string (or null) as current
- **THEN** downstream collectors SHALL NOT receive a new emission
- **AND** when value changes to a different string, downstream SHALL receive exactly one emission