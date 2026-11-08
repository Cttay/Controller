# ui/edge-to-edge Specification

## Purpose
Enables the app to draw behind system bars (status bar, navigation bar) on Android 15+ (API 35), ensuring content is not obscured by system UI and providing a modern full-screen experience.

## Requirements

### Requirement: App draws behind system bars on API 35+

The system SHALL opt into edge-to-edge display so that app content extends behind the status bar and navigation bar.

#### Scenario: Edge-to-edge enabled on API 35 device
- **WHEN** app launches on Android 15 (API 35) device
- **THEN** app content SHALL extend behind status bar and navigation bar
- **AND** system bars SHALL have transparent/semi-transparent background
- **AND** no content SHALL be clipped or obscured by system bars

#### Scenario: Graceful fallback on pre-API 35
- **WHEN** app launches on Android 14 (API 34) or lower
- **THEN** app SHALL function normally without crashes
- **AND** system bars SHALL use default behavior (content not behind bars)

### Requirement: Composable content handles system bar insets

The system SHALL ensure all screen composables apply appropriate padding for system bar insets to prevent content overlap.

#### Scenario: Top content not hidden behind status bar
- **WHEN** any screen displays content near top of viewport
- **THEN** content SHALL have padding equal to status bar height
- **AND** padding SHALL be applied via `Modifier.statusBarsPadding()`

#### Scenario: Bottom content not hidden behind navigation bar
- **WHEN** any screen displays content near bottom of viewport (buttons, controls)
- **THEN** content SHALL have padding equal to navigation bar height
- **AND** padding SHALL be applied via `Modifier.navigationBarsPadding()`

#### Scenario: Gamepad screen controls accessible
- **WHEN** GamepadScreen displays virtual controller
- **THEN** controller buttons/sticks SHALL not overlap with navigation bar
- **AND** bottom controls SHALL have navigation bar padding
