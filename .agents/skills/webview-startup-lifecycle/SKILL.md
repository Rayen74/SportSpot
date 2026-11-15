---
name: webview-startup-lifecycle
description: Make SportSpot's Three.js WebView scene load without blocking the hero UI, pause when hidden or backgrounded, resume safely, and dispose WebGL resources and listeners on permanent exit. Use for startup, visibility, lifecycle, memory-leak, or cleanup changes.
---

# WebView 3D Startup and Lifecycle

## Goal

The WebView must become interactive before Three.js finishes loading. The 3D scene is progressive enhancement: it may appear after the UI, pause when not visible, and fall back to a static presentation without preventing navigation.

## Startup: UI first, 3D second

- Keep the essential hero content and controls in HTML/CSS so they can appear without WebGL.
- Register the Find a field, How it works, Back, and fallback interactions immediately. Do not wait for renderer initialization before binding controls.
- Avoid top-level static imports of the large Three.js module in the initial UI controller. Load Three.js and RoomEnvironment dynamically from the existing local import-map aliases after the initial UI has rendered and the browser has an idle opportunity.
- Use a scheduling strategy with a timeout/fallback so scene loading cannot be starved indefinitely. The scheduler must not delay user input or navigation.
- Keep the local import map and relative asset paths valid under `WebViewAssetLoader`; do not replace local dependencies with a CDN.
- Show a neutral/static hero while the scene is loading. Reveal the canvas only after the renderer and first valid frame are ready.
- Catch import, WebGL-context, renderer, and shader initialization failures. Keep HTML navigation usable and show the existing static fallback or equivalent safe presentation.
- If the user navigates away while dynamic imports are pending, mark the scene as inactive and check that state before creating the renderer or attaching listeners. Dynamic module fetch itself may not be cancellable, but scene initialization must be skipped when no longer needed.
- Guard initialization so repeated lifecycle events cannot create multiple renderers, canvases, or animation loops.

## Animation scheduling

Use an explicit lifecycle with states such as `not-started`, `loading`, `running`, `paused`, `disposing`, and `disposed`. Keep state transitions idempotent.

- Start a `requestAnimationFrame` loop only while the 3D scene is visible and an animation is active.
- Store the frame request ID and cancel it on pause/dispose.
- Render a single frame for a static scene. Restart the loop for tilt, shatter, reassembly, or another intentional animation.
- Avoid a permanent RAF loop just to maintain very subtle idle drift. Prefer a static rest state or a deliberately low-cost, measured idle effect.
- Pause when `document.hidden` is true and when the journey view covers/fades out the canvas.
- Resume when the scene becomes visible again. Reset the clock/timestamp before the next frame and clamp delta to avoid a large animation jump.
- Handle `visibilitychange`, `pagehide`, and `pageshow` where appropriate. Remove these listeners during permanent disposal.
- Honor `prefers-reduced-motion`. When enabled, skip or simplify shard motion and present the static view. Re-check the media query if the preference can change while the WebView remains open.

## SportSpot view transitions

- The hero-to-journey transition must not wait for a long 3D sequence. Navigation should remain immediate even if the animation is skipped or the scene is still loading.
- When the journey view becomes active and the canvas is no longer visible, pause the render loop and stop pointer-driven rendering.
- When Back returns to the hero, resume or reconstruct the scene safely. Do not create a second WebGL renderer while the first is still alive.
- Check the existing timing relationship between `CONFIG.shatterDuration` and UI transition timers before adjusting them. Preserve a coherent visual transition, but never make the user wait for the effect to finish.
- If the hero is permanently left (for example, the native booking screen takes over), release the 3D scene resources rather than leaving a hidden renderer running.

## Pause versus dispose

These operations are different and must not be conflated.

### Pause (temporary)

- Cancel the RAF loop and stop scheduling new renders.
- Remove or disable high-frequency interaction work if the view is inactive.
- Preserve the renderer, scene, geometry, and materials so a quick return can resume without paying the full initialization cost.
- Do not call `renderer.dispose()` or force WebGL context loss for an ordinary temporary pause.

### Dispose (permanent)

- Enter a `disposing` state and make repeated dispose calls safe.
- Cancel RAF and any timers or delayed startup callbacks.
- Remove every event listener registered on `window`, `document`, the canvas, and other DOM nodes. Keep handler references or use a controlled listener lifetime so removal is possible.
- Stop any renderer animation loop and release owned geometries, materials, textures, render targets, environment resources, and PMREM resources.
- If resources are shared, deduplicate them before disposal so they are disposed once and only when no longer used.
- Dispose cloned materials created for individual shards and geometry created for each shard.
- Clear scene references, shard arrays, and callbacks after release so they do not retain the old renderer or WebView.
- Call renderer disposal as part of permanent teardown. Use forced context loss only when truly terminating that WebGL context and after checking compatibility; do not use it as a routine pause mechanism.
- Make sure failure during initialization also cleans up any resources already created.

## Android lifecycle integration

Inspect `MainActivity.java` before changing it. It currently forwards `onPause()` and `onResume()` to the WebView and pauses/resumes WebView timers. Preserve this behavior and make JavaScript state robust to suspension/resume.

- Ensure returning from the Android background does not produce a huge delta-time jump.
- Ensure leaving/destroying the Activity cannot leave JavaScript timers or a WebGL render loop alive.
- If a native screen transition is implemented, explicitly pause the hero scene before handing focus to the booking screen. Dispose when the hero will not be reused.
- Keep lifecycle calls safe if WebView or JavaScript is not initialized.
- Do not introduce an unrestricted JavaScript bridge to solve lifecycle communication. Prefer browser lifecycle events and the existing narrow custom-scheme navigation unless a narrowly scoped, security-reviewed bridge is genuinely required.

## Failure handling

- WebGL unsupported, context creation failed, context lost, module import failed, or shader compile failed must leave an operable HTML UI.
- Avoid showing a fallback over healthy content after an unrelated JavaScript error. Scope error handling to scene initialization/render failures where possible.
- Listen for WebGL context loss/restoration if the scene is expected to recover. Stop rendering immediately on loss; attempt a controlled rebuild only if restoration is supported and useful. Otherwise retain the static fallback.
- Do not retry initialization in an unbounded loop.

## Acceptance checks

- Cold start shows usable UI without waiting for Three.js/PMREM initialization.
- Tapping navigation during scene loading works.
- The journey view has no hidden continuous RAF/render activity.
- Android background/foreground does not cause an animation jump or duplicate loop.
- Repeated hero/back transitions do not grow WebGL resource counts or memory monotonically.
- Permanent exit removes event listeners and releases scene resources.
- Reduced-motion and WebGL failure remain usable.
