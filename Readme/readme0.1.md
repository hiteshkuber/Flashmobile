Flashmobile — README v0.1

Overview
--------
Flashmobile is a minimal single-screen Android app (Jetpack Compose) that demonstrates a flashlight controller with an interactive semicircular gauge and a large central toggle button. The screen has a black background, a big circular toggle (gray when off, yellow when on), and an interactable semicircular gauge that maps 0..100 to a blinking frequency for the device torch.

What (features implemented)
---------------------------
- Single screen Compose UI with black background.
- Large central circular toggle button (160dp).
  - Gray when OFF, yellow when ON.
- Semicircular draggable gauge around the button (left = 0, right = 100).
  - Drag the arc to change a numeric value (displayed under the control).
- Torch control (CameraManager.setTorchMode):
  - Toggle OFF: torch always off.
  - Toggle ON & gauge == 0: torch stays continuously ON.
  - Toggle ON & gauge > 0: torch blinks with frequency mapped from gauge value.
- Runtime CAMERA permission request and basic error handling for camera access.

Why (design decisions & motivations)
------------------------------------
- Single-screen simplicity: the user flow is intentionally minimal — one control for power and one for intensity/frequency, so it's easy to operate from the lock screen or while using the device one-handed.
- Compose UI: modern, concise, and easy to iterate on for visual components like the gauge and animated toggle.
- Semicircular gauge: provides a natural left-to-right mapping for a 0..100 range while leaving space for a large central toggle.
- Torch behavior:
  - Special-case 0 as continuous ON because the user requested "if gauge value is 0 and toggle is on flash of mobile is on" (interpreted as steady on).
  - Blink frequency mapping uses a linear interpolation that matches the requested anchors (50 -> 4 Hz, 100 -> 16 Hz).
  - Safety: enforce a lower bound on blink frequency (>= 1 Hz for non-zero values) to avoid extremely slow toggles.

How it works (technical details)
--------------------------------
- Permission: the app requests CAMERA permission at startup using the Activity Result API. Without permission, the app will not attempt to control the torch.
- Camera access: the app uses CameraManager.getCameraIdList().firstOrNull() to pick a camera ID and CameraManager.setTorchMode(cameraId, boolean) to toggle the torch.
- Blinking implementation: a coroutine toggles the torch on/off repeatedly. For a requested frequency f (Hz) the coroutine turns the torch ON for period/2 ms then OFF for period/2 ms where period = 1000 / f.

Frequency mapping
-----------------
- Anchors given: 50 -> 4 Hz, 100 -> 16 Hz.
- Linear mapping derived: f(v) = 0.24 * v - 8, solved from the two anchors.
- Implementation details:
  - v == 0 -> continuous ON (special case).
  - For v > 0 -> f(v) = max(0.24*v - 8, 1) Hz (lower-clamped at 1 Hz).
  - This keeps f(50) ≈ 4 Hz and f(100) ≈ 16 Hz while preventing negative or too-small frequencies.

Impact & important notes
------------------------
- Battery: high-frequency blinking (e.g., 16 Hz) will drain battery faster than steady-on or low-frequency blinking.
- Safety and seizure risk: flashing lights can trigger photosensitive epilepsy in some people. Use of rapid blinking should include a visible warning and an option to limit maximum frequency in future releases.
- Privacy & permissions: CAMERA permission is required to control the torch. We only use it to operate the torch; no camera frames are captured or stored.
- Device compatibility: some devices or camera hardware/firmware might not support setTorchMode reliably. The implementation catches exceptions to avoid crashes, but behavior may vary.

How to build & run (quick)
---------------------------
- Open the project in Android Studio (recommended) and run on a physical device that has a flashlight.
- Or from the command line (Windows PowerShell):

```powershell
cd "C:\Users\Hitesh Kuber\AndroidStudioProjects\Flashmobile"
./gradlew assembleDebug
# then install and run on device with adb if desired:
# ./gradlew installDebug
# adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Testing checklist
-----------------
- Confirm CAMERA permission is requested on launch and granting it allows torch control.
- Toggle OFF -> torch stays OFF.
- Toggle ON + gauge = 0 -> torch steady ON.
- Toggle ON + gauge = 50 -> torch blinks ~4 times/second.
- Toggle ON + gauge = 100 -> torch blinks ~16 times/second.
- Try denying permission and verify the app does not crash.
- Test on multiple physical devices, because emulators generally do not provide torch hardware.

Troubleshooting
---------------
- "Torch doesn't respond": ensure the device has a rear flashlight and that CAMERA permission was granted.
- "App crashes on torch access": check logcat for CameraAccessException; some devices require using a specific camera ID. We pick the first ID as a pragmatic default.
- "Blinking is too fast/slow": see Frequency mapping section — the mapping can be adjusted. To limit maximum, clamp f(v) to a desired maximum.

Limitations & future work
-------------------------
- UX: add a visible thumb on the gauge and support tap-to-set as well as drag.
- Accessibility: add content descriptions, better focus handling, and an option for reduced flashing (for photosensitive users).
- Safety: add an explicit warning about flashing lights and an opt-in for high-frequency modes.
- Frequency/curve: provide a slider to fine-tune the mapping or use a non-linear curve for perceptual control.

Contact / developer notes
-------------------------
- Implemented by the project maintainer in this workspace.
- If you want the mapping or UI changed (e.g., different max frequency, continuous-on semantics, or alternative UX for the gauge), tell me which change you prefer and I will update the code.

Version history
---------------
- v0.1 — initial README describing core implementation, design choices, and testing notes.

