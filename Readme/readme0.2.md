Flashmobile — README v0.2

Overview
--------
Flashmobile is a minimal single-screen Android app (Jetpack Compose) that provides a simple flashlight controller with an interactive semicircular gauge and a large central toggle button. The UI is optimized for one-handed use and quick access to the device torch.

What this app does (features)
-----------------------------
- Single-screen Compose UI with a black background for high contrast.
- Large central circular toggle button (160dp):
  - Gray when OFF, yellow when ON (animated color transition).
  - Tap the button to toggle the torch state.
- Semicircular draggable gauge wrapped around the toggle:
  - Left = 0, Right = 100.
  - Drag along the semicircle to change a numeric value (displayed under the control).
  - Gauge maps 0..100 to torch behavior (frequency of blinking).
- Torch control using CameraManager.setTorchMode(cameraId, boolean):
  - Toggle OFF: torch always OFF.
  - Toggle ON & gauge == 0: torch stays continuously ON (special-case steady-on behavior).
  - Toggle ON & gauge > 0: torch blinks with frequency mapped from gauge value.
- Runtime CAMERA permission request using the Activity Result API (launcher pattern).
- Robust handling around camera access:
  - Null-safe camera id resolution (first available camera id used as a pragmatic default).
  - Try/catch around CameraAccessException and all camera operations to prevent crashes.
- Coroutine-based blinking implementation that cancels cleanly when the UI or toggle state changes.
- Edge-to-edge layout enabled so the app content can extend to screen edges on supported devices.
- Preview composable included for quick UI previewing in Android Studio.

Technical details (how it works)
--------------------------------
- Permission: the app requests CAMERA permission at startup using Activity Result API. If the user denies permission the app will not attempt to control the torch.
- Camera id: CameraManager.getCameraIdList().firstOrNull() is used to pick a camera ID. This is a pragmatic default (may be changed per-device if required).
- Blinking implementation: a coroutine toggles the torch on/off repeatedly. For a requested frequency f (Hz) the coroutine turns the torch ON for period/2 ms then OFF for period/2 ms where period = 1000 / f.
- Frequency special-case: when gauge value == 0 the torch is kept steadily on while the toggle is ON.

Frequency mapping
-----------------
- Anchors given by the original design: 50 -> 4 Hz, 100 -> 16 Hz.
- Linear mapping used in the app: f(v) = 0.24 * v - 8 (derived from the two anchors).
- Implementation details:
  - v == 0 -> continuous ON (special-case).
  - v > 0 -> f(v) = max(0.24*v - 8, 1) Hz (clamped to a minimum of 1 Hz).
  - This keeps f(50) ≈ 4 Hz and f(100) ≈ 16 Hz, while preventing negative or extremely small frequencies.

Safety, battery & compatibility
-------------------------------
- Battery: high-frequency blinking (e.g., 16 Hz) consumes more power than steady-on or low-frequency blinking.
- Safety / seizure risk: flashing lights can trigger photosensitive epilepsy. The app currently warns in the README; future releases should add an in-app warning and a reduced-flash or opt-in for high-frequency modes.
- Device compatibility: some devices or firmware may not support setTorchMode reliably or may require a specific camera id. The app catches exceptions to prevent crashes, but behavior may vary across devices.

What's new in v0.2 (changes vs v0.1)
-----------------------------------
This release (v0.2) documents and includes several code and UX improvements made after the initial README (v0.1):

1) Permission and lifecycle robustness
   - Explicit use of the Activity Result API with a typed permission launcher and a LaunchedEffect to trigger the permission request when needed.
   - hasCameraPermission is tracked in Compose state and updated from the launcher callback so UI reacts correctly.

2) Cleaner torch lifecycle handling
   - A coroutine Job is used for blink control and is canceled whenever toggle state, value, or permissions change.
   - DisposableEffect ensures the blinking job is cancelled and the torch is turned off when the composable leaves the composition.
   - Camera operations wrapped in try/catch to avoid crashes on CameraAccessException or other runtime issues.

3) Visual & interaction improvements
   - Animated toggle color (smooth transition between gray and yellow) using Compose's animateColorAsState.
   - Toggle button now responds to tap gestures explicitly (detectTapGestures) to avoid accidental toggles from other pointer interactions.
   - Semicircular gauge interaction improved to use pointerInput + detectDragGestures and to only accept touches on the top semicircle (angles 180..360° mapped to 0..100).
   - The UI shows the numeric value under the control so users get exact feedback for the gauge position.

4) Edge-to-edge support and preview
   - enableEdgeToEdge() is invoked so the app can draw behind system bars where appropriate.
   - A @Preview composable is available to preview the screen in Android Studio's design tools.

5) Minor code quality and safety updates
   - Null-safe camera id selection using firstOrNull().
   - Guarding frequency calculation to avoid negative frequencies by clamping to a sensible minimum (1 Hz) for v > 0.
   - Ensured that the torch is turned off when toggle is set to OFF or the composable is disposed.

Notes
-----
- The functional behavior described in v0.1 remains: toggle + gauge determine steady/blinking behavior and frequency mapping still uses the same anchor points.
- This release focuses on robustness, lifecycle correctness, and improved interactivity. No breaking behavior changes were introduced.

Limitations & next steps
------------------------
- UX: add a visible thumb on the gauge, support tap-to-set for the gauge, and show haptic feedback when value changes.
- Accessibility: include content descriptions, better focus handling, and a "reduced flashing" accessibility option.
- Safety: add an in-app warning and a configurable maximum frequency to reduce seizure risk.
- Device coverage: add a camera id selection fallback UI for devices where the default camera id does not control a torch.

Build & run (quick)
-------------------
- Open the project in Android Studio (recommended) and run on a physical device that has a flashlight.
- Or from the command line (Windows PowerShell):

```powershell
cd "C:\Users\Hitesh Kuber\AndroidStudioProjects\Flashmobile";
./gradlew assembleDebug
# then install and run on device with adb if desired:
# ./gradlew installDebug
# adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Testing checklist (quick)
-------------------------
- Confirm CAMERA permission is requested on launch and granting it allows torch control.
- Toggle OFF -> torch stays OFF.
- Toggle ON + gauge = 0 -> torch steady ON.
- Toggle ON + gauge = 50 -> torch blinks ~4 times/second.
- Toggle ON + gauge = 100 -> torch blinks ~16 times/second.
- Verify that turning the toggle OFF or leaving the screen cancels blinking and turns the torch OFF.
- Test denying permission and verify the app does not crash.

Contact / developer notes
-------------------------
- Implemented by the project maintainer in this workspace.
- If you want the mapping or UI changed (e.g., different max frequency, continuous-on semantics, or alternative UX for the gauge), specify the change and it can be implemented.

Version history
---------------
- v0.1 — initial README describing core implementation, design choices, and testing notes.
- v0.2 — robustness, lifecycle, and interaction improvements; animated toggle; edge-to-edge support; safer camera handling.


