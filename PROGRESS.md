# ASTRA — Development Progress

**Package:** `com.spider.astra`  
**Target device:** Redmi Pad Pro 5G · Android 16 · tablet-first  
**Last updated:** 13 November 2026

## Phase 0 — Environment inspection

**COMPLETE.** Android build environment was prepared in the OpenCode Mobile workspace. The Termux Python backend remains a separate project and has not been overwritten.

## Phase 1 — App foundation

**IMPLEMENTED · CI BUILD SUCCESSFUL · OWNER VISUAL TEST COMPLETED**

- Kotlin Android app, Compose navigation and theme.
- Holographic dashboard, animated orb, quick actions and command dock.
- Chat and local repository.
- DataStore preferences.

## Phase 2 — Floating interface

**IMPLEMENTED · CI BUILD SUCCESSFUL · OWNER DEVICE TEST PASSED**

- User-granted overlay permission.
- Foreground service and persistent notification.
- Movable orb, expanded panel, open/minimize/stop controls.
- Orb glow/window bounds corrected after owner reported clipping.

## Phase 3 — Safe task engine

**IMPLEMENTED · CI BUILD SUCCESSFUL · UNIT TESTS PASSED**

- WorkManager-backed task execution.
- DataStore-backed task history and private notes.
- Allowlisted read-only system status and confirmed private-note save.
- Explicit confirmation, cancellation, 15-second execution timeout and failure reporting.
- Unsupported commands rejected; arbitrary shell input is never executed.

## Phase 4 — Voice interface

**IMPLEMENTED · CURRENT CI RUN VERIFYING · DEVICE VOICE TEST PENDING**

- Tap-to-talk control on Chat.
- Runtime `RECORD_AUDIO` permission requested only when the microphone is tapped.
- Partial transcript preview and final recognizer result.
- On-device recognition is preferred where Android reports it available; configured system recognizer is fallback.
- English (India) and Hindi (India) language preference persisted in DataStore.
- Optional automatic spoken replies and per-message speaker controls.
- Listening is cancelled when the app backgrounds; TextToSpeech and SpeechRecognizer resources are released when Chat closes.
- No background microphone service, wake word or audio recording archive.

### Phase 4 test checklist on Redmi Pad Pro

1. Open Chat and tap the microphone; confirm Android shows the microphone permission prompt.
2. Grant permission and say a short English command such as “status”.
3. Select Hindi (India) in Settings and test a short Hindi command.
4. Confirm the recognized text creates the expected task.
5. Enable “Speak assistant replies” and verify ASTRA reads its reply aloud.
6. Tap the speaker control on an ASTRA message to replay it.
7. Switch to another app while listening; confirm listening stops and no late command is submitted.

## Phase 5 — Next milestone

- Authenticated connection from Android app to the existing Termux Python backend.
- Localhost bridge lifecycle, health checks, request IDs, timeouts and clear offline status.
- Keep provider API keys in Termux, not in the APK.
- Continue routing future device actions through the task allowlist and explicit approval.

## Security rules

- Do not overwrite the Termux backend during Android app development.
- Do not hardcode provider credentials in source or APK.
- Never interpret raw user text as shell commands.
- Do not claim queued work has completed before the task store reports completion.
- Microphone capture must remain user-initiated and visible.
