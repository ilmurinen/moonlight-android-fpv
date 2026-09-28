# Moonlight Android — Agent Guide

## Project Overview

Moonlight Android is an open-source Android client for NVIDIA GameStream and Sunshine. It streams games from a Windows PC to Android devices.

## Key Directories

- `app/src/main/java/com/limelight/` — main Java source
- `app/src/main/java/com/limelight/binding/input/virtual_controller/` — on-screen controller (OSC)
- `app/src/main/java/com/limelight/binding/input/` — ControllerHandler, touch input
- `app/src/main/java/com/limelight/preferences/` — preferences system
- `app/src/main/res/xml/preferences.xml` — preference UI layout
- `app/src/main/res/values/strings.xml` — string resources

## On-Screen Controller Architecture

All OSC elements are custom `android.view.View` subclasses rendered via `Canvas` drawing, added programmatically as children of the `FrameLayout` containing `StreamView`.

### Core Files

| File | Role |
|---|---|
| `VirtualController.java` | Orchestrator — manages elements, aggregates `ControllerInputContext`, sends state to host |
| `VirtualControllerElement.java` | Abstract base View — drawing, touch dispatch, move/resize config mode, JSON persistence, and layout-change release hook |
| `DigitalButton.java` | Oval button (A/B/X/Y, LB/RB, START/BACK, L3/R3, GUIDE) — `drawOval` + text/icon |
| `DigitalPad.java` | D-pad — 4 rectangular zones with diagonal lines |
| `AnalogStick.java` | Concentric circles with movable nub — dead zones, click/double-click, `holdYAxis` throttle mode |
| `LeftAnalogStick.java` / `RightAnalogStick.java` | Wires callbacks to `ControllerInputContext` |
| `LeftTrigger.java` / `RightTrigger.java` | DigitalButton subclasses setting trigger bytes |
| `VirtualControllerConfigurationLoader.java` | Default layout (128×72 grid at 16:9), saves/loads custom positions as JSON in SharedPreferences |

### Touch Flow

1. OSC elements receive `MotionEvent` directly via Android View dispatch
2. Subclass `onTouchEvent()` → listener callbacks → `ControllerInputContext`
3. `VirtualController.sendControllerInputContext()` → `ControllerHandler.reportOscState()` → `MoonBridge.sendMultiControllerInput()` → JNI → UDP to host

Touches outside OSC fall through to `backgroundTouchView` → mouse events.

### Configuration Mode

A gear button cycles **Active → Move → Resize → Save**. Positions persist as JSON in `SharedPreferences("OSC")` keyed by element ID.

## Toggle Mode (LT Latch)

Added in `DigitalButton.java` — `toggleMode` flag makes a momentary button behave as a latching toggle.

### How It Works

- `toggleMode` field (default `false`) + `setToggleMode()` setter
- On ACTION_DOWN: inverts `isPressed()`, fires `listener.onClick()` if now latched on, `listener.onRelease()` if now latched off
- ACTION_MOVE / ACTION_UP / ACTION_CANCEL: ignored for a toggle button itself; toggle buttons are also excluded as targets of momentary-button slide transfer
- Long-click timer is cancelled on each press
- Visual feedback uses existing `pressedColor`/`getDefaultColor()` — shows blue when latched on, gray when off
- Layout recreation preserves the trigger value and initializes the new toggle view's pressed state from `ControllerInputContext`

### Preference

- `PreferenceConfiguration.ltToggle` — read from `checkbox_lt_toggle` in SharedPreferences
- Wired in `VirtualControllerConfigurationLoader.createLeftTrigger()` via `button.setToggleMode(config.ltToggle)`
- UI checkbox in `category_onscreen_controls` with dependency on `checkbox_show_onscreen_controls`

## Throttle Mode (Left Stick Y-Axis Hold)

Added in `AnalogStick.java` — `holdYAxis` flag prevents the Y-axis from returning to 0 on release.

### How It Works

- `holdYAxis` field (default `false`) + `setHoldYAxis()` setter
- `lastMovementY` captures the normalized Y value (-1.0 to 1.0) on each `notifyOnMovement()` call inside `updatePosition()`
- On release (ACTION_UP/ACTION_CANCEL): if `holdYAxis` is true, calls `notifyOnMovement(0, lastMovementY)` instead of `notifyOnMovement(0, 0)`; the throttle/Y value is held, while X returns to zero and the nub recenters horizontally
- On NO_MOVEMENT draw: if `holdYAxis` is true, draws nub at `(position_stick_x, position_stick_y)` instead of center
- `setDeadZonePercent()` controls the radial deadzone (default 30%). At 0%, every touch position, including exact center, is sent immediately without the normal deadzone timeout
- `setInputPosition()` initializes the nub from current controller-axis values; layout recreation uses this to keep the display in sync with retained input

### Preference

- `PreferenceConfiguration.leftStickHoldY` — read from `checkbox_left_stick_hold_y` in SharedPreferences
- Wired in `VirtualControllerConfigurationLoader.createLeftStick()` via `stick.setHoldYAxis(config.leftStickHoldY)`
- UI checkbox in `category_onscreen_controls` with dependency on `checkbox_show_onscreen_controls`

## Flight Sim Layout

An alternative layout selectable via `list_osc_layout` preference in Settings → On-screen Controls. Activated when `PreferenceConfiguration.oscLayout` equals `"flight_sim"`.

### Layout Contents

| Element | Type | Behavior | Position (128×72 grid) |
|---|---|---|---|
| Left stick | AnalogStick | Zero deadzone; throttle/Y holds on release, X resets to zero | (6, 4), 26×26 |
| Right stick | AnalogStick | Zero deadzone; both axes reset on release | (98, 42), 26×26 |
| ARM | LeftTrigger (toggle) | Toggles `leftTrigger` 0xFF/0x00 | Bottom row, centered |
| BRAKE | RightTrigger (toggle) | Toggles `rightTrigger` 0xFF/0x00 | Bottom row |
| FIRE | DigitalButton (momentary) | Sets `A_FLAG` while held | Bottom row |
| ESC | DigitalButton (momentary) | Sends Escape key to host via `KeyboardPacket` | Bottom row |

### Implementation

- Wired in `VirtualControllerConfigurationLoader.createFlightSimLayout()`
- Branched from `createDefaultLayout()` based on `config.oscLayout`
- Uses distinct element IDs for all Flight Sim controls (`EID_FS_ARM=17`, `EID_FS_BRAKE=18`, `EID_FS_FIRE=19`, `EID_FS_SEC=20`, `EID_FS_LS=21`, `EID_FS_RS=22`) so saved positions do not collide with default-layout controls
- The ARM/BRAKE trigger subclasses accept an explicit element ID; their standard constructors continue to use `EID_LT`/`EID_RT`
- The bottom button row is centered using its actual occupied width and the screen width, rather than assuming a 16:9 grid
- ESC sends the host Escape key map `0x801B` for both key-down and key-up via `controller.sendKeyboardInput()` → `ControllerHandler.sendKeyboardInput()` → `conn.sendKeyboardInput()`
- Layout removal calls `releaseForLayoutChange()` on each element before detaching it. Momentary buttons and active sticks release their transient input; latched triggers and held throttle values persist, and recreated views are initialized from the retained context
- Excludes D-pad, bumpers, BACK, START, and GUIDE

### Adding New OSC Preferences

1. Add string constant in `PreferenceConfiguration.java`
2. Add field + read in `readPreferences()`
3. Add XML element in `res/xml/preferences.xml`
4. Add string resources in `res/values/strings.xml`
5. Wire in `VirtualControllerConfigurationLoader.createDefaultLayout()`

## Element ID Constants

Defined in `VirtualControllerElement.java`:
- `EID_DPAD = 1`, `EID_LT = 2`, `EID_RT = 3`, `EID_LB = 4`, `EID_RB = 5`
- `EID_A = 6`, `EID_B = 7`, `EID_X = 8`, `EID_Y = 9`
- `EID_BACK = 10`, `EID_START = 11`, `EID_LS = 12`, `EID_RS = 13`
- `EID_LSB = 14`, `EID_RSB = 15`, `EID_GDB = 16`
- `EID_FS_ARM = 17`, `EID_FS_BRAKE = 18`, `EID_FS_FIRE = 19`, `EID_FS_SEC = 20`
- `EID_FS_LS = 21`, `EID_FS_RS = 22`

ARM/BRAKE previously used the default LT/RT IDs. Those old saved positions are not migrated; after upgrading, FPV ARM/BRAKE use their default positions until customized and saved again.

Flight Sim sticks use their own IDs starting with this layout version. Previously saved stick positions used the default stick IDs, so they remain the default layout's saved positions; Flight Sim starts at its default stick positions until customized and saved again.

## Build

- Requires Android Studio + NDK
- `git submodule update --init --recursive`
- Create `local.properties` with `ndk.dir=`
- Build via Android Studio or `./gradlew assembleDebug`
