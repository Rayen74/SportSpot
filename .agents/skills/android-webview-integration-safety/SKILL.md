---
name: android-webview-integration-safety
description: Protect SportSpot's Android Java/WebView boundary while optimizing the Three.js hero. Use when changing MainActivity, WebView lifecycle, local asset loading, deep-link interception, fallback navigation, or handoff to field discovery and booking screens.
---

# Android WebView Integration Safety

## Goal

Optimize the decorative Three.js experience without coupling it to Android navigation, field discovery, booking, or security-sensitive behavior.

## Existing integration to preserve

Re-open the current code before editing. The inspected repository has:

- `MainActivity.java` creating a `WebView` from `activity_main.xml`.
- `WebViewAssetLoader` serving local assets on `assets.sportspot.local`.
- The hero loaded from `https://assets.sportspot.local/assets/sportspot/index.html`.
- JavaScript and DOM storage enabled, file/content access disabled, and mixed content disallowed.
- A `SecureHeroClient` that serves local assets and intercepts the custom `sportspot://find-fields` scheme.
- WebView `onResume()`/`resumeTimers()` and `onPause()`/`pauseTimers()` lifecycle forwarding.
- An Android back callback that asks the journey view to return to the hero before delegating to system back navigation.
- `navigateToNativeFieldDiscovery()` currently acting as a placeholder. Do not silently invent its destination or change its behavior.

## Navigation is independent of WebGL

- Keep the Find a field action usable when the Three.js module has not loaded, WebGL is unavailable, or scene creation fails.
- Preserve the current custom URI scheme and action host `sportspot://find-fields` unless the owner requests a product navigation change.
- Never make native handoff wait for the shatter animation, a render callback, a WebGL promise, or a model load.
- The 3D canvas is decorative. It must not cover or intercept taps on DOM controls.
- The HTML fallback must retain the primary navigation action.
- Keep the journey Back button and Android system-back behavior working if the WebGL scene is paused, loading, disposed, or unavailable.
- Do not route booking requests through the animation loop or put booking logic in Three.js callbacks.

## WebView security and local assets

- Preserve `WebViewAssetLoader` and the trusted virtual HTTPS origin.
- Preserve strict handling of unexpected main-frame navigations.
- Keep `allowFileAccess` and `allowContentAccess` disabled unless an independently reviewed feature requires a change.
- Keep mixed content disabled.
- Do not add a broad `addJavascriptInterface` bridge. If native communication is necessary, design a minimal allowlisted API, validate every input, document the threat model, and review it independently from the rendering work.
- Do not load Three.js from a remote CDN. Keep dependencies local and version-consistent with the import map.
- Do not relocate or remove vendor files until every import map, module import, and asset reference has been searched and verified.

## Lifecycle coordination

- Preserve native WebView pause/resume behavior while ensuring the JavaScript render loop also pauses/resumes cleanly.
- If the app transitions to a native discovery/booking Activity, pause the hero scene before the handoff so it does not consume GPU resources behind the native screen.
- Resume the scene only when returning to the hero and only if the scene is still valid.
- Dispose permanently when the WebView/Activity is being destroyed or when the hero will not be reused.
- Make all cleanup operations safe if `webView` is null, the JavaScript module has not initialized, or WebGL creation failed.
- Avoid blocking the Android main thread with JavaScript evaluation, asset processing, model decoding, or synchronous waits.
- Avoid changing `onBackPressed` behavior in the name of performance without testing the journey view and native back stack.

## Regression scenarios

Test all of these with WebGL enabled and with the fallback forced:

1. Launch the app and tap Find a field before the 3D scene is ready.
2. Launch and immediately tap How it works.
3. Navigate to the journey view and use its Back button.
4. Navigate to the journey view and use Android system Back.
5. Trigger the native field-discovery action while a shatter animation is in progress.
6. Background and foreground the app during scene loading and during animation.
7. Rotate/resize if supported by the Activity configuration.
8. Repeatedly enter and leave the hero.
9. Destroy and recreate the Activity.
10. Simulate WebGL initialization failure and verify primary navigation still works.

A performance change is unacceptable if it makes any of these paths less reliable.
