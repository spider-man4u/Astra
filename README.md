# ASTRA

**Package:** `com.spider.astra`  
**Platform:** Native Android (tablet-first)  
**Primary device:** Redmi Pad Pro 5G · Android 16 · 8 GB RAM

ASTRA is a personal Android assistant concept inspired by JARVIS / FRIDAY / EDITH. Its planned experience is a floating orb, conversational AI and carefully permissioned device actions.

## Current milestone status

### Phase 1 — Application foundation

**Implemented and CI build verified.** Includes the holographic Compose dashboard, animated orb, quick prompts, reactive mock chat, task history and DataStore-backed user preferences.

### Phase 2 — Floating interface

**Implemented and CI build verified · target-device testing pending.**

- User-triggered overlay permission flow from Settings.
- Floating orb hosted by a foreground service.
- Tap the orb to expand the compact control panel.
- Drag the orb or expanded panel to reposition it.
- Open ASTRA from the panel, minimize back to the orb, or stop the service.
- Persistent low-priority foreground notification with a Stop action.
- Overlay access can be managed or revoked by the user in Android Settings.

The service is an interface shell only. It does not read other apps, capture the screen, use the microphone, execute shell commands, or perform device actions. The assistant backend remains mock-only.

## Permissions and Android behaviour

ASTRA requests `SYSTEM_ALERT_WINDOW` because Android requires explicit user approval before an app can display a `TYPE_APPLICATION_OVERLAY` window. The floating service starts only from an explicit action while the app is visible.

On Android 14 and later, the service declares the `specialUse` foreground-service type and explains its floating-interface use in the manifest. This is a system declaration, not a Play Store approval. A store release would still need the applicable foreground-service policy review.

Android 13+ notification permission is requested separately. It is optional for starting the foreground service; denying it does not grant overlay access and does not block the user-enabled orb.

## What works in the current build

- **Dashboard:** holographic orb, state indicator, command dock, quick-access tiles and recent activity.
- **Chat:** live message history, send controls and clear mock-mode disclosure.
- **Settings:** display name, System / Dark / Light appearance, voice preference, overlay permission and floating-orb controls.
- **Floating interface:** movable orb, expanded mini panel, open-app / minimize / stop controls.
- **Mock assistant:** simulates Thinking → Planning → Executing → Completed and records a task.

No real AI provider is connected. Voice preference is stored only; speech recognition and speech output are not implemented yet.

## Build configuration

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.5.0 |
| Gradle wrapper distribution | 8.7 |
| Kotlin | 1.9.24 |
| Compose Compiler | 1.5.14 |
| Compose BOM | 2024.06.00 |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 24 |
| JDK in CI | 21 |

GitHub Actions generates the Gradle wrapper JAR on the hosted runner, validates it, builds the debug APK and uploads the artifact for 30 days.

## Download the debug APK

1. Open [Android CI runs](https://github.com/spider-man4u/Astra/actions/workflows/android-ci.yml).
2. Open the latest successful run on `master`.
3. In **Artifacts**, download `astra-debug-apk`.
4. Extract and install `app-debug.apk` on the tablet.

## Planned next phases

| Phase | Scope |
|---|---|
| 3 | Background task orchestration and safe action execution |
| 4 | Speech recognition and text-to-speech |
| 5 | Secure connection to the existing Python backend in Termux |
| 6 | Device integrations, permission gates and allowlisted actions |

The existing Termux Python backend is separate from this Android project and must not be overwritten during app development.

## Security boundaries

- No provider API keys or credentials are embedded in the APK.
- No arbitrary shell execution, silent screen capture, messaging or payments.
- Sensitive future actions must use an allowlist and explicit confirmation.
- The UI must report mock / offline state honestly rather than fabricate success.
