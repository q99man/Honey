# Mobile Kakao Smoke Test

This document describes the repeatable smoke flow for the Flutter dev app Kakao native map integration.

## Purpose

Use this flow after changing mobile map, location, place marker, or Kakao SDK configuration code.

The smoke test verifies:

- the dev Android app can be built with `KAKAO_NATIVE_APP_KEY`
- the APK can be installed on an emulator or Android device
- the app launches with Kakao native map configuration
- relevant Kakao, Honeytong, Flutter, map, auth, and error logs can be captured

Manual UI checks are still required for visual map behavior.

## Prerequisites

1. Add the Kakao native app key to the root `.env` file:

```properties
KAKAO_NATIVE_APP_KEY=your-kakao-native-app-key
HONEY_DEV_API_BASE_URL=http://10.0.2.2:8080
```

2. Register the Android dev package in Kakao Developers:

```text
Package name: com.honeytong.app.dev
```

3. Print the local debug key hashes and register them in Kakao Developers:

```powershell
.\scripts\check-mobile-kakao.ps1 -ShowDevices
```

4. Start an emulator or connect an Android device.

## Readiness Check

Run this first. It does not build, install, or launch the app.

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -CheckOnly
```

The check intentionally does not print secret key values.

If Android Studio has an emulator but no device is attached yet, the check should list available AVD names. The current local AVD name is expected to be similar to:

```text
Pixel_10_Pro
```

## Smoke Run

Run the default dev smoke:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1
```

To let the script start the first available Android Studio emulator before running the smoke:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -StartEmulator
```

To start a specific AVD:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -StartEmulator -AvdName Pixel_10_Pro
```

If multiple devices are attached, pass the target device ID:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -DeviceId emulator-5554
```

To capture logs longer after launch:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -LogcatSeconds 30
```

If the dev APK was already built and Flutter/Gradle is stuck returning from the build step, reuse the existing APK and continue with install, launch, and log capture:

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -DeviceId R3CX306ZWGW -LogcatSeconds 45 -SkipBuild
```

For a physical USB-connected Android device that cannot reach the PC's LAN IP because of Wi-Fi isolation or firewall rules, use ADB reverse. This makes the app call `http://127.0.0.1:8080` on the device and forwards it to the PC backend on port 8080. Do not use `-SkipBuild` the first time because the API base URL is embedded during Flutter build.

```powershell
.\scripts\run-mobile-kakao-smoke.ps1 -DeviceId R3CX306ZWGW -LogcatSeconds 45 -UseAdbReverse
```

## Manual Checks

After the app launches:

- Home screen shows the Kakao map, not the missing-key state
- map tiles load without authentication errors
- current location marker is visible when location permission is available
- API place markers are visible when nearby place data has coordinates
- tapping a place marker opens the place detail screen
- bottom navigation and map provider controls do not overlap
- visible user-facing text remains Korean

## Current Local Blockers

The current local `Pixel_10_Pro` emulator uses `x86_64`. The Kakao Maps native library currently packaged by this project includes `arm64-v8a` and `armeabi-v7a`, but not `x86_64`, so the real Kakao native map smoke test cannot run on that emulator.

An ARM64 AVD can be created from `system-images;android-35;google_apis;arm64-v8a`, but on the current x86_64 Windows host the Android emulator exits with:

```text
Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host. System image must match the host architecture.
```

For real native Kakao map verification, use an ARM64 Android device or a hosted ARM64 Android device service.
