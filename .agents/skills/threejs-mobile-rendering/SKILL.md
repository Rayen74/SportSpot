---
name: threejs-mobile-rendering
description: Reduce Three.js WebGL rendering cost in SportSpot, including GPU fill rate, renderer configuration, materials, lighting, geometry, draw calls, allocations, and frame pacing on Android WebView. Use when editing or reviewing the Three.js scene in main.js.
---

# Three.js Mobile Rendering

## Goal

Keep the current SportSpot glass-shard visual recognizable while reducing frame time, GPU load, CPU work, heat, and memory usage on mid-range Android devices. Optimize for stable frame pacing and responsive touch, not a peak FPS screenshot.

## Inspect before changing

Read `app/src/main/assets/sportspot/main.js` and the local Three.js version used by the import map. Identify the renderer, scene, camera, render loop, environment generation, materials, geometry, raycasting, pointer updates, and resize handling.

The current scene uses a `WebGLRenderer`, `RoomEnvironment`/PMREM, directional lights, five extruded shard meshes, cloned `MeshPhysicalMaterial` instances, shader-injected touch glow, a `Raycaster` on pointer-down, tilt interpolation, shatter/reassembly animation, and a perpetual animation loop. Reconfirm these details in the current code.

## Renderer configuration

- Benchmark renderer settings on a physical Android device. Do not assume desktop defaults are safe for mobile.
- Do not force `devicePixelRatio` up to 2 on every phone. Start with a conservative cap (for example, around 1.25–1.5) and compare visual quality, frame time, and GPU load. Use a higher tier only when the device sustains it.
- Consider dynamic resolution or a small set of quality tiers when device performance varies materially. Change render resolution only at controlled points; avoid resizing the backing buffer every frame.
- Test antialiasing on and off. Keep it only when the visual gain justifies the GPU cost. Consider material edges, geometry bevels, and a subtle CSS treatment as alternatives where visually acceptable.
- Do not force `powerPreference: 'high-performance'` by default. Compare the default choice with lower-power behavior on representative devices; prefer measured battery/thermal outcomes.
- Keep CSS canvas size and drawing-buffer size separate. Avoid unnecessary style writes during resize; use the renderer's supported sizing options after checking the installed Three.js version.
- Create one renderer and one WebGL context for the scene lifecycle. Never create a new renderer on every screen transition or resize.

## Geometry and draw calls

- Measure `renderer.info.render.calls`, `renderer.info.render.triangles`, and `renderer.info.memory` before making geometry changes.
- Keep geometry as simple as the intended visual allows. Reduce bevel segments, bevel thickness, and unnecessary depth only after comparing the result visually.
- Avoid creating new `Shape`, `ExtrudeGeometry`, vectors, arrays, or temporary objects inside the per-frame update path.
- Reuse geometry and shared immutable resources when possible. If meshes require separate transforms, use separate object transforms rather than duplicating geometry.
- Preserve correct raycasting and shard identity after geometry changes.
- Do not add particle systems, post-processing passes, shadow maps, extra lights, or higher-resolution environment maps without a measured visual requirement and a device budget.

## Materials and environment

- `MeshPhysicalMaterial` features such as clearcoat, iridescence, transmission, and complex environment reflections can be expensive. Enable only the features that are visibly important.
- The current material sets transmission to zero while using clearcoat and iridescence. Evaluate whether a simpler `MeshStandardMaterial` or a reduced physical-material configuration can preserve the desired appearance on a low-quality tier.
- Avoid generating a new PMREM environment for each shard. Generate environment resources once per scene and share them.
- Reuse a common environment texture and lights. Do not clone large textures or render targets per object.
- Material clones may share GPU shader programs but still have distinct material state. Profile before changing the current per-shard highlight behavior. If using a shared material, ensure highlighting one shard does not unintentionally affect all shards.
- Avoid expensive real-time reflection or transmission features unless a real visual comparison demonstrates the benefit.
- Keep shader customizations minimal and compatible with the exact bundled Three.js version. Verify shader compilation on the Android WebView GPU, not only a desktop browser.

## Frame loop and animation work

- Use one animation scheduler and one clock for the scene.
- Keep per-frame work bounded and allocation-free. Reuse vectors and temporary objects.
- Clamp unusually large delta values after resume (for example, `Math.min(delta, 0.05)`) so the first frame after backgrounding does not jump through an animation.
- Do not update static objects or uniforms when their values have not changed.
- Render continuously only while a real animation needs it. If the scene is visually static, render once and stop the loop. A subtle sinusoidal drift is not automatically worth a permanent render loop.
- During tilt or shatter, update only the objects and uniforms required by that effect.
- Keep pointer event handlers small. Raycast on pointer-down or an intentional interaction, not on every animation frame.
- Do not run layout reads/writes or DOM queries from the render loop.
- Stop or suspend the loop when the canvas is hidden, the document is hidden, reduced motion is active, or the WebView is paused.

## Responsive quality policy

Provide a clear conservative quality baseline and optional higher tiers. Base quality decisions on actual signals where available, such as measured frame time and WebGL capabilities. Do not rely exclusively on `hardwareConcurrency`, device-memory hints, or user-agent sniffing; these signals can be missing or misleading.

A reduced-quality tier can use a lower pixel ratio, disabled antialiasing, simpler material features, fewer bevel segments, and less frequent/nonexistent idle motion. A very constrained or unsupported device should use the static fallback.

Avoid rapid quality oscillation. Use a cooldown and require sustained evidence before switching tiers.

## Visual and interaction constraints

- Preserve the dark SportSpot hero, glass-shard identity, touch feedback, shatter/reassembly meaning, and legible overlay text.
- Keep DOM controls independent from the canvas. The canvas must not intercept taps intended for the Find a field, How it works, and Back controls.
- Maintain correct touch coordinate mapping after pixel-ratio or resize changes.
- Do not make animation completion a prerequisite for navigation.
- Keep reduced-motion behavior meaningful: do not animate the shards when reduced motion is requested.

## Required verification

- Compare visual output on a physical Android device at low and normal quality.
- Record frame-time distribution and missed frames during tilt, tap feedback, shatter, and reassembly.
- Verify touch accuracy after orientation/size changes.
- Verify that no animation work continues while the scene is inactive.
- Check `renderer.info` before and after repeated scene creation/disposal to detect growing geometry, texture, or render-target counts.
- Confirm that any quality downgrade still leaves text, buttons, and native navigation fully functional.
