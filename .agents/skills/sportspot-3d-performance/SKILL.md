---
name: sportspot-3d-performance
description: Optimize or review SportSpot's existing Android WebView and Three.js hero animation for fast startup, smooth interaction, low CPU/GPU/RAM use, and safe lifecycle cleanup without breaking native navigation or booking flows. Use for end-to-end 3D performance work in this repository.
---

# SportSpot 3D Performance

## Mission

Improve the perceived and measured performance of SportSpot's 3D hero experience. The goal is not an FPS number in isolation. The experience must:

1. Show usable UI and keep navigation responsive before 3D initialization finishes.
2. Avoid frame drops, input lag, long main-thread tasks, and interaction freezes.
3. Use a conservative GPU/CPU/memory budget suitable for mid-range Android phones.
4. Stop rendering when the scene is hidden, the app is backgrounded, or the scene is otherwise inactive.
5. Release WebGL resources and listeners when the scene is permanently disposed.
6. Keep all booking and native navigation behavior independent from the decorative 3D scene.
7. Degrade gracefully when WebGL is unavailable, initialization fails, or reduced motion is requested.

## Repository architecture

This repository is an Android Java/XML application with a Three.js scene loaded through a local Android WebView. It is not a native OpenGL, Unity, or SceneView application.

Relevant files:

- `app/src/main/java/com/example/sportspot/MainActivity.java`: creates the WebView, serves local assets with `WebViewAssetLoader`, handles Android lifecycle, and intercepts the `sportspot://find-fields` action.
- `app/src/main/res/layout/activity_main.xml`: hosts the WebView.
- `app/src/main/assets/sportspot/index.html`: hero and journey UI, import map, canvas, and fallback.
- `app/src/main/assets/sportspot/main.js`: Three.js scene, renderer, materials, geometry, pointer input, transitions, and animation loop.
- `app/src/main/assets/sportspot/styles.css`: overlay layout, controls, transitions, and reduced-motion CSS.
- `app/src/main/assets/sportspot/vendor/three.module.js` and `RoomEnvironment.js`: locally bundled Three.js dependencies.

The WebView entry point is loaded from `https://assets.sportspot.local/assets/sportspot/index.html`. The HTML import map resolves `three` and the RoomEnvironment addon from the local `./vendor/` directory. Preserve this local-asset loading model unless a measured, justified change requires otherwise.

## Repository-specific risks already identified

Re-check these findings against the current branch before acting:

- `main.js` statically imports Three.js and RoomEnvironment at module load, so the large Three.js dependency begins loading before the page has prioritized UI readiness.
- `init()` creates the WebGL renderer, PMREM environment and scene geometry during startup.
- The renderer enables antialiasing and allows device pixel ratio up to 2, which can multiply fill cost on high-density mobile screens.
- `animate()` runs a continuous `requestAnimationFrame` loop even when the canvas is faded out for the journey view.
- The `visibilitychange` handler only resets the clock when the page becomes visible; it does not stop the render loop while hidden.
- `CONFIG.reducedMotion` is computed but is not used to bypass or simplify the WebGL animation.
- `buildGlassShards()` creates bevelled extruded meshes and cloned physical materials. Preserve the visual intent, but measure the cost before retaining every expensive feature on every device tier.
- Resource teardown is incomplete in the current Three.js script: event listeners, geometries, cloned materials, the generated environment texture, and renderer resources do not have one explicit scene-disposal path.
- The Java activity already pauses the WebView and its timers in `onPause()` and resumes them in `onResume()`. The JavaScript scene should also maintain a coherent pause/resume state.
- The custom action `sportspot://find-fields` is intercepted by `MainActivity`. Its current native destination method is a placeholder; do not invent or silently change booking behavior while optimizing the hero.
- The repository has more than one vendor asset path. Check all import references before removing or relocating duplicate files.

These are code-inspection findings, not device benchmark measurements.

## Required workflow

### 1. Map the current behavior

Before editing, inspect the current versions of the files listed above. Trace:

- WebView entry and first visible UI.
- The import map and every Three.js import.
- Renderer creation, pixel ratio, size, environment, materials, geometry and draw calls.
- Every animation and event listener.
- Hero-to-journey and journey-to-hero transitions.
- WebView `onPause`, `onResume`, and `onDestroy` behavior.
- The `sportspot://find-fields` navigation path and fallback path.

Do not assume that a path, API, or behavior described in this skill is unchanged.

### 2. Establish a baseline

On at least one physical mid-range Android device, record cold start, first usable UI, first 3D frame, frame pacing during the animation, input response, idle CPU/GPU activity, and memory. Record the device model, Android version, WebView version, build variant, and test conditions.

Do not claim a performance improvement without a before/after comparison under comparable conditions.

### 3. Prioritize in this order

1. **Interaction-first startup:** make text, controls, fallback, and native navigation usable without waiting for Three.js.
2. **Lifecycle correctness:** pause hidden/background scenes and dispose permanently unused resources.
3. **Rendering cost:** tune pixel ratio, antialiasing, materials, environment, geometry, and draw calls using measurements.
4. **Validation:** profile representative devices and test navigation, background/foreground, repeated entry/exit, and fallback behavior.

Do not begin by rewriting the whole scene or replacing Three.js.

### 4. Change one performance dimension at a time

Make small, reviewable changes. Re-profile after each meaningful change. Preserve a known-good visual baseline so regressions can be identified.

### 5. Report evidence and trade-offs

Summarize:

- Files changed and why.
- The bottleneck addressed.
- Before/after measurements and device conditions.
- Visual trade-offs, if any.
- Lifecycle and navigation tests performed.
- Remaining risks or unmeasured assumptions.

## Non-negotiable guardrails

- Treat the 3D scene as decorative enhancement, never as a prerequisite for finding fields, booking, authentication, or navigation.
- UI controls must work even while Three.js is importing, WebGL is initializing, or WebGL fails.
- Do not block the Android main thread with model parsing, large computations, or synchronous waits.
- Do not introduce a remote CDN dependency for Three.js; the current app uses local WebView assets.
- Preserve the secure `WebViewAssetLoader` origin and strict navigation checks.
- Preserve the `sportspot://find-fields` custom action unless the owner explicitly requests a navigation change.
- Do not add an unrestricted `addJavascriptInterface` bridge.
- Do not leave a render loop active behind the journey view or after the app is backgrounded.
- Do not call `renderer.dispose()` for a temporary pause; distinguish pause/resume from permanent disposal.
- Do not dispose shared geometry, materials, or textures multiple times. Track owned resources and deduplicate cleanup.
- Do not remove the static fallback or reduced-motion support to make the scene appear faster.
- Do not change booking, backend, database, or authentication logic as part of a 3D optimization task.
- Avoid dependency upgrades or framework migrations unless profiling shows they are necessary and compatibility is verified.

## Definition of done

- The hero UI and primary actions are usable before the 3D scene is ready.
- Tapping Find a field follows the existing native handoff without waiting for a 3D animation.
- The journey view and Android back navigation behave as before.
- Rendering pauses when the scene is hidden or the app is backgrounded and resumes without a large delta-time jump.
- Permanent scene teardown removes listeners and releases all owned WebGL resources.
- Reduced-motion and WebGL-failure paths remain functional.
- No new crash, navigation regression, or persistent GPU activity is observed after leaving the scene.
- Performance claims are supported by repeatable measurements on physical Android hardware.
