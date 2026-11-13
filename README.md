# ASTRA

**Package:** `com.spider.astra`  
**Platform:** Native Android · tablet-first  
**Primary test device:** Redmi Pad Pro 5G · Android 16 · 8 GB RAM

ASTRA is a personal Android assistant concept inspired by JARVIS / FRIDAY / EDITH. Its experience is a holographic dashboard, a user-controlled floating orb, a local task engine and voice interaction.

## Current milestones

### Phase 1 — App foundation

**Implemented and CI verified.** Holographic Compose dashboard, animated orb, command dock, quick actions, chat, task history and DataStore preferences.

### Phase 2 — Floating interface

**Implemented and CI verified · owner device test passed.** Permission-gated floating orb, expanded panel, drag-to-move, open/minimize/stop actions and persistent notification.

### Phase 3 — Safe background task engine

**Implemented and CI verified.** WorkManager queue, DataStore-backed task and note history, cancellation, timeout/error handling and explicit action allowlist.

Current actions:
- System status: read-only.
- Save private note: requires an explicit Confirm action.
- Unsupported requests: rejected without execution.

### Phase 4 — Voice interface

**Implemented in this build · Redmi Pad Pro voice test pending.**
- Tap-to-talk in Chat with runtime microphone permission.
- Partial transcript preview and final speech recognition.
- Prefer on-device recognition when Android reports it available; otherwise use the installed system recognizer.
- English (India) and Hindi (India) voice language preference.
- Text-to-speech for replies when enabled, plus per-message speaker controls.
- Recognition is cancelled when the app leaves the foreground; SpeechRecognizer and TextToSpeech resources are released when Chat is closed.

This is not an always-listening wake-word system. The microphone is activated only after the user taps the mic and grants Android's `RECORD_AUDIO` permission. If Android has no on-device recognizer, its configured speech service is used and may rely on a network connection.

## Android permissions and privacy

- `SYSTEM_ALERT_WINDOW`: user-approved floating overlay.
- `RECORD_AUDIO`: requested when the user taps the microphone.
- Notification permission: separately requested for the floating foreground-service notification.

ASTRA does not start background microphone capture and does not persist raw audio recordings. Speech recognition is delegated to Android's configured speech-recognition service.

## Build configuration

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.6.0 |
| Gradle wrapper distribution | 8.7 |
| Kotlin | 2.1.20 |
| Compose Compiler plugin | 2.1.20 |
| Compose BOM | 2024.06.00 |
| WorkManager | 2.12.0 |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 24 |
| JDK in CI | 21 |

GitHub Actions runs unit tests, builds the debug APK and uploads the artifact for 30 days.

## Install the debug APK

1. Open [Android CI runs](https://github.com/spider-man4u/Astra/actions/workflows/android-ci.yml).
2. Open the latest successful run on `master`.
3. Download the `astra-debug-apk` artifact and extract `app-debug.apk`.
4. Install it on the tablet.

## Next milestone — Phase 5

- Authenticated connection from Android app to the existing Python backend in Termux.
- Localhost bridge lifecycle, health check, request IDs, timeouts and clear offline status.
- Keep API keys and provider credentials in Termux, never in the APK.
- Continue routing sensitive operations through the task allowlist and approval flow.

## Security boundaries

- No provider API keys or credentials are embedded in the APK.
- No arbitrary shell execution, silent screen capture, messaging or payments.
- Sensitive future actions must use a small allowlist and explicit confirmation.
- The UI must report local/demo state honestly rather than fabricating success.
