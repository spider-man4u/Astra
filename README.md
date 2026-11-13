# ASTRA

**Package:** `com.spider.astra`  
**Platform:** Native Android · tablet-first  
**Primary test device:** Redmi Pad Pro 5G · Android 16 · 8 GB RAM

ASTRA is a personal Android assistant inspired by JARVIS / FRIDAY / EDITH. It combines a holographic Android UI, user-controlled floating orb, safe task engine, voice interface and a private Termux Python assistant.

## Current milestones

### Phase 1 — App foundation

**Implemented and CI verified.** Holographic Compose dashboard, animated orb, command dock, quick actions, chat, task history and DataStore preferences.

### Phase 2 — Floating interface

**Implemented and CI verified · owner device test passed.** Permission-gated floating orb, expanded panel, drag-to-move, open/minimize/stop actions and persistent notification.

### Phase 3 — Safe background task engine

**Implemented and CI verified.** WorkManager queue, DataStore-backed task and note history, cancellation, timeout/error handling and explicit action allowlist.

### Phase 4 — Voice interface

**Implemented and CI verified · device voice test pending.** Tap-to-talk, microphone permission, partial transcription, English (India) / Hindi (India), optional spoken replies and lifecycle cleanup. There is no always-listening wake word or background microphone capture.

### Phase 5 — Termux Core bridge

**Implemented and CI verified · live device connection test pending.**

- Android connects to `http://127.0.0.1:8765` only.
- Android stores the bridge bearer token encrypted with an AES-GCM key in Android Keystore.
- A separate Termux sidecar imports the existing ASTRA v2 `handle_turn()` function; it does not copy or replace `astra.py`, the SQLite database or provider keys.
- The bridge binds to `127.0.0.1` only, requires a bearer token for health and command requests, rejects other routes and caps request size.
- Commands are serialised through the existing handler to protect the conversational SQLite history.
- Request IDs are cached for the lifetime of the bridge process to avoid re-running a request after a client retry.
- The bridge enables ASTRA v2 popup-confirmation mode so risky device tools continue to require Termux:API confirmation.
- If the token is saved but Termux is offline, Android shows an error and does **not** silently fall back to the mock assistant.

The bridge requires the ASTRA v2 backend API: `handle_turn()`, `doctor_report()` and `command_response()`. It intentionally refuses an older CLI-only backend. Use the ASTRA v2 upgrade pack first if those functions are not present.

## Set up the Termux bridge

1. Make sure ASTRA v2 is installed at `~/astra` and its fresh provider keys are kept in `~/astra/.env`.
2. Download and extract the **astra-termux-bridge** artifact from the latest successful Android CI run.
3. In Termux, enter the extracted `termux` folder and run:
   ```bash
   chmod 700 install_bridge.sh
   ./install_bridge.sh
   python ~/astra/bridge_server.py
   ```
4. On first start, copy the generated access token printed in Termux. To show it again later:
   ```bash
   python ~/astra/bridge_server.py --show-token
   ```
5. Keep the Termux session alive during the test. In ASTRA Android open **Settings → Termux Core Link**, paste the token, tap **Save Token**, then **Test Link**.
6. Open Chat and send `/status` or a normal assistant request. Existing ASTRA v2 memory, model routing and permission dialogs are handled by the Termux backend.

The sidecar install script copies only `bridge_server.py` into `~/astra/`; if a prior bridge file exists it makes a timestamped backup. It does not overwrite your Python assistant or database. The server is foreground-only in this phase; it does not install a hidden daemon or boot service.

## Security and privacy

- Keep Gemini/Groq/local-provider credentials in Termux's private `.env`; never place them in Android source or the APK.
- Older ASTRA source copies may contain embedded provider keys. Revoke/rotate any such keys and use fresh values in `~/astra/.env`.
- The bridge bearer token is generated on the tablet and stored in a Termux config file with owner-only permissions. Android stores its copy encrypted with Android Keystore.
- HTTP cleartext is permitted only for the `127.0.0.1` loopback endpoint. The app's base network policy continues to reject cleartext to other hosts.
- The bridge does not listen on Wi-Fi, mobile data or LAN interfaces.
- Risky backend device actions continue through ASTRA v2's existing confirmation flow. The bridge does not introduce arbitrary shell execution.
- The Android app never silently uses mock mode after a configured backend connection fails.

## Android permissions

- `SYSTEM_ALERT_WINDOW`: user-approved floating overlay.
- `RECORD_AUDIO`: requested only when the user taps the microphone.
- `INTERNET`: local loopback HTTP connection to the Termux sidecar.
- Notification permission: separately requested for the floating foreground-service notification.

The current app targets SDK 35. Android 17's `ACCESS_LOCAL_NETWORK` runtime permission applies to apps targeting API 37 or higher; this build does not request that unrelated LAN permission.

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

GitHub Actions runs Python bridge tests, Android unit tests, builds the debug APK and uploads both the APK and Termux bridge package for 30 days.

## Install the Android debug APK

1. Open [Android CI runs](https://github.com/spider-man4u/Astra/actions/workflows/android-ci.yml).
2. Open the latest successful run on `master`.
3. Download `astra-debug-apk`, extract `app-debug.apk`, and install it on the tablet.
4. Download `astra-termux-bridge` separately for the Termux sidecar.

## Next milestone — Phase 6

- Native app integrations and device controls through the Termux bridge.
- Task/memory visibility from the Termux SQLite database.
- Explicit approval surfaces for risky tool calls and more robust background lifecycle handling.

## Security boundaries

- No provider API keys or credentials are embedded in the APK.
- No arbitrary shell endpoint is exposed by the bridge.
- Sensitive actions remain controlled by ASTRA v2's registered tools and confirmation callbacks.
- The UI reports bridge offline state honestly rather than fabricating success.
