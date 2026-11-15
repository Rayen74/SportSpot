---
name: mobile-3d-performance-validation
description: Measure and regression-test SportSpot's Three.js animation performance in Android WebView on physical devices. Use for profiling, benchmarking, acceptance checks, or validating performance claims.
---

# Mobile 3D Performance Validation

## Purpose

Prove that an optimization improves the actual SportSpot experience on Android. Do not infer success from code appearance, desktop FPS, or a single emulator run.

## Test environment record

For every benchmark, record:

- Physical device model and chipset/RAM class.
- Android version and WebView/Chrome version.
- SportSpot commit, build variant, and APK version.
- Screen resolution, refresh rate, display scaling, and battery state where available.
- Whether the run is cold or warm and whether the device is thermally throttled.
- Scene quality settings and whether reduced motion/fallback is enabled.

Use at least one representative mid-range Android device. Add a lower-end device and a higher-end comparison device when available. Emulators are useful for functional regression but do not replace physical-device GPU and thermal tests.

## Metrics to collect

### Startup and perceived performance

- Time from app launch to visible, legible hero UI.
- Time until Find a field and How it works can be tapped.
- Time until the first valid 3D frame appears.
- Whether 3D loading delays any UI, input, or navigation task.
- WebView/module/scene initialization duration, measured separately where practical.

The essential UI must become usable independently from the 3D scene. Do not hide a slow UI behind a static splash to make the metric look better.

### Rendering and interactions

- Frame-time distribution (not just average FPS), including 50th and 95th percentile where practical.
- Missed vsyncs/janky frames during idle, pointer tilt, shard highlight, shatter, and reassembly.
- Input-to-visible-feedback latency for touch interactions.
- Main-thread long tasks and scripting time.
- Renderer draw calls, triangles, geometry count, and texture/render-target count using `renderer.info` where supported.
- Whether animation work continues after the scene becomes hidden.

A 60 Hz display has about 16.7 ms per frame; a 30 Hz budget is about 33.3 ms. Treat these as frame budgets, not guaranteed device targets. Prefer stable pacing over short-lived peaks.

### Resource use

- CPU activity while animating and while the scene is visible but idle.
- CPU activity after opening the journey view, backgrounding the app, or entering native navigation.
- GPU load/power behavior where tooling and device support allow it.
- Java heap, native heap, WebView renderer memory, and process memory.
- Growth in WebGL resource counts after repeated enter/exit cycles.
- Thermal behavior and sustained performance during a longer run.

Do not claim exact GPU memory from `renderer.info.memory`; its geometry/texture counts are useful indicators, not a direct byte-accurate GPU memory measurement.

## Tools

Use tools appropriate to the available environment:

- Android Studio CPU Profiler and Memory Profiler.
- Android Studio/Perfetto system traces for scheduling, frame timing, main-thread stalls, and CPU behavior.
- Chrome DevTools remote inspection (`chrome://inspect/#devices`) for WebView Performance, Memory, and console inspection.
- `adb shell dumpsys gfxinfo <application-id> framestats` for Android UI frame statistics where applicable.
- `adb logcat` for WebView, renderer, and WebGL initialization errors.
- Three.js `renderer.info` counters for scene-level resource/draw-call observations.

Enable WebView remote debugging only in debug builds (for example, behind a build/debug check). Do not enable debugging in production builds as a profiling shortcut.

## Repeatable test procedure

1. Install a clean build and record its commit/build/device details.
2. Perform a cold launch and measure first visible UI and first tap availability.
3. Measure first 3D frame separately; confirm the UI was already interactive.
4. Run a consistent 10–20 second scene sequence: resting hero, touch tilt, shard tap, shatter, and reassembly.
5. Open How it works and observe whether the canvas stops rendering and CPU activity falls.
6. Return to the hero and verify smooth, correct resume without a timing jump or duplicate loop.
7. Background the app during loading and during animation; foreground it again.
8. Repeat hero/journey entry and exit at least 10–20 times while watching resource counts and process memory.
9. Force WebGL failure/reduced motion and verify the static fallback and primary controls.
10. Run the same procedure on the same device with the baseline build and compare results.

For thermal validation, repeat the workload for several minutes and check whether frame pacing degrades as the device heats.

## Functional regression checklist

- Find a field works before, during, and after 3D initialization.
- How it works works even if the 3D scene fails.
- The journey Back button works.
- Android system Back works from the journey view.
- Native `sportspot://find-fields` interception remains unchanged.
- The canvas does not intercept control taps.
- No duplicate animation loop starts after repeated resume events.
- No persistent render activity remains while hidden/backgrounded.
- No unbounded growth in renderer resource counters or process memory appears after repeated entry/exit.
- Reduced-motion and WebGL-failure paths remain usable.

## Reporting rules

- Show baseline and optimized values side by side under matching conditions.
- Include both improvements and regressions; do not cherry-pick one metric.
- Separate measured facts from estimates and assumptions.
- State when GPU power or memory could not be measured directly.
- Do not promise zero lag or universal 60 FPS. State the tested device range and remaining limitations.
- If a change improves average FPS but worsens input latency, startup, memory, thermals, or navigation, do not declare it a success without resolving the trade-off.
