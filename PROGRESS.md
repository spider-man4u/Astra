# ASTRA — Development Progress

**Package:** `com.spider.astra`  
**Target device:** Redmi Pad Pro 5G · Android 16 · tablet-first  
**Last updated:** 13 November 2026

## Phase 0 — Environment inspection

**COMPLETE.** Android build environment was prepared in the OpenCode Mobile workspace. The Termux Python backend remains a separate project.

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
- Allowlisted read-only status and confirmed private-note save.
- Explicit confirmation, cancellation, 15-second timeout and failure reporting.
- Unsupported commands rejected; arbitrary shell input is never executed.

## Phase 4 — Voice interface

**IMPLEMENTED · CI BUILD SUCCESSFUL · DEVICE VOICE TEST PENDING**

- Tap-to-talk, microphone permission and partial transcription.
- On-device recognition preferred where available, system recognizer fallback.
- English (India) and Hindi (India) language preference.
- Optional spoken replies and per-message speaker controls.
- Listening is cancelled when the app backgrounds; voice resources are released when Chat closes.
- No wake word, always-listening mode or background recording.

## Phase 5 — Termux Core bridge

**IMPLEMENTED · CI BUILD SUCCESSFUL · DEVICE LINK TEST PENDING**

### Android app
- Fixed loopback endpoint: `127.0.0.1:8765`.
- AES-GCM token encryption using an Android Keystore key.
- Settings panel to save, test, disconnect and erase the bridge token.
- Chat commands route to the Termux backend when a token is configured.
- Configured-but-offline bridge displays an error and never falls back to mock execution.
- Cleartext is allowed only for the loopback host; other hosts remain cleartext-disabled.
- App version: 1.0.5 / versionCode 5.

### Termux sidecar
- `termux/bridge_server.py` imports the installed ASTRA v2 handler and calls `handle_turn()`.
- Binds only to `127.0.0.1`.
- Bearer token generated locally and stored at `~/.config/astra/bridge.token` with owner-only permissions.
- Authenticated `GET /v1/health` and `POST /v1/command` endpoints.
- Request size validation, loopback peer/Host check, request ID replay cache and serialised command execution.
- ASTRA v2 popup-confirmation mode is enabled before the backend is imported.
- `install_bridge.sh` copies only the sidecar into `~/astra/`; it does not overwrite `astra.py`, `hands.py`, `.env` or the SQLite database.
- Bridge refuses to start if the installed assistant does not expose the required ASTRA v2 handler API.

### CI coverage
- Python bridge unit tests: health auth, invalid token, command forwarding, duplicate request replay and unknown route rejection.
- Android unit tests: existing voice language tests.
- Debug APK build and artifact upload.
- CI does not prove the physical Termux process is running or that the tablet has accepted the token; the actual connection must be tested on the Redmi Pad Pro.

### Phase 5 device checklist

1. Confirm ASTRA v2 is installed at `~/astra` and fresh provider keys are in its private `.env`.
2. Install the sidecar using `termux/install_bridge.sh`.
3. Start `python ~/astra/bridge_server.py` and copy its generated access token.
4. Save the token in ASTRA → Settings → Termux Core Link.
5. Tap Test Link; confirm it reports Connected.
6. Send `/status` then a normal natural-language prompt from Chat.
7. Test a risky backend action and confirm the Termux:API permission dialog appears.
8. Stop the Termux server and verify the Android app reports offline instead of using mock mode.

## Security rules

- Never upload `~/astra/.env` or share provider keys.
- Revoke and rotate any provider keys found in older source copies with hard-coded credentials.
- Never add an arbitrary shell endpoint to the bridge.
- Do not bypass ASTRA v2's existing confirmation callbacks.
- Keep the bridge bound to loopback; never expose it to LAN.
- Do not overwrite the existing Termux backend or database during bridge installation.
