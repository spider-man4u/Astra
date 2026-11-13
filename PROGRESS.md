# ASTRA — Development Progress

**Project:** ASTRA  
**Package:** `com.spider.astra`  
**Target:** Redmi Pad Pro 5G · Android 16 · tablet-first  
**Last verified:** 13 November 2026

## Phase 0 — Environment inspection

**Status: COMPLETE**

The initial coding environment was inspected and the required JDK, Android SDK command-line tools, API 35 platform, Build Tools 35.0.0 and Gradle 8.x toolchain were prepared in the OpenCode Mobile workspace. The Termux Python backend is separate and has not been modified.

## Phase 1 — Application foundation

**Status: IMPLEMENTED · CI BUILD SUCCESSFUL · DEVICE VISUAL TEST COMPLETED BY OWNER**

Delivered:
- Android app module, manifest, theme resources and launcher icon.
- Kotlin 1.9.24, AGP 8.5.0, Gradle 8.7 and Compose Compiler 1.5.14.
- Holographic dashboard, animated orb, command dock and quick-access tiles.
- Reactive mock conversation, task state and recent activity.
- Dashboard / Chat / Settings navigation.
- DataStore-backed display name, appearance mode and voice preference.

The owner installed the Phase 1 build and provided dashboard / settings screenshots. This feedback prompted the holographic redesign.

## Phase 2 — Floating interface

**Status: IMPLEMENTED · CI BUILD SUCCESSFUL · REDMI PAD PRO RUNTIME TEST PENDING**

Delivered:
- `SYSTEM_ALERT_WINDOW` permission declaration and explicit Settings hand-off.
- Permission status refresh when returning to ASTRA.
- Foreground service using the Android 14+ `specialUse` type and manifest subtype explanation.
- Persistent low-priority notification with open-app and Stop action.
- Floating orb overlay using `TYPE_APPLICATION_OVERLAY` (with legacy window type fallback on API 24–25).
- Tap-to-expand compact panel, drag-to-move, minimize-to-orb, open ASTRA and stop controls.
- Optional Android 13+ notification permission request, separate from overlay approval.
- Settings controls to grant/manage overlay permission and start/stop the orb.

The overlay is deliberately independent from the mock assistant. It does not inspect other applications, capture the screen, use the microphone, execute commands or perform device actions.

### CI verification

- **Commit:** `6aecff3959022848456f5c25de5ec9840b6b9b6a`
- **Workflow:** [Android CI — run 43398815358](https://github.com/spider-man4u/Astra/actions/runs/43398815358)
- **Build task:** `:app:assembleDebug` — successful
- **Artifact:** `astra-debug-apk`
- **Artifact size:** 9,260,371 bytes (about 9.26 MB)
- **Artifact ID:** `14024515502`

CI confirms compilation and packaging only. The floating service must still be tested on the Redmi Pad Pro for permission behaviour, display positioning, dragging, expand/minimize, notification Stop action and service shutdown.

## Phase 3 — Next milestone

- Background task orchestration and state persistence.
- Safe action registry with allowlisted operations.
- Explicit confirmation for sensitive actions.
- Robust cancellation, timeout and error reporting.

## Phase 4–6 — Future scope

- Phase 4: speech recognition and text-to-speech.
- Phase 5: authenticated local connection to the existing Termux Python backend.
- Phase 6: allowlisted Android device integrations and permission gates.

## Project safety notes

- Do not overwrite the existing Termux backend while working on this Android app.
- Do not hardcode provider credentials in source or APK.
- Do not claim that mock commands performed real actions.
- Do not execute arbitrary shell commands or perform sensitive actions without explicit user approval.
