# ASTRA Termux Bridge (Phase 5)

This is a **sidecar** for the ASTRA v2 Python assistant. It does not overwrite `astra.py`, `hands.py`, `senses.py`, `.env` or the SQLite database.

## Requirements

- ASTRA v2 installed at `~/astra` (its `astra.py` must expose `handle_turn()`, `doctor_report()`, and `command_response()`).
- Python 3 and the dependencies already used by ASTRA v2.
- Termux:API installed and working for confirmation dialogs on risky actions.

The bridge deliberately refuses to start against an older CLI-only backend. It will not guess an output format or scrape terminal text.

## Install

From this folder in Termux:

```bash
chmod 700 install_bridge.sh
./install_bridge.sh
```

The installer copies only `bridge_server.py` into `~/astra/`. If a bridge file already exists, it makes a timestamped backup first. The assistant source and database are not replaced.

## Start

```bash
python ~/astra/bridge_server.py
```

The server binds to **127.0.0.1:8765 only**. It does not listen on Wi-Fi, mobile data, Ethernet or other LAN interfaces.

On first launch it creates a random bearer token in:

```
~/.config/astra/bridge.token
```

The file is created with owner-only permissions. The first launch prints the token once. To display it later:

```bash
python ~/astra/bridge_server.py --show-token
```

Do not paste this token into chats, screenshots, GitHub, or `.env`.

Keep this Termux session running while the Android app uses the bridge. Press Ctrl+C to stop the server. This first version is foreground-only; it does not install a boot receiver or keep a hidden daemon alive.

## Connect ASTRA Android

1. Start the Termux bridge.
2. Open ASTRA → Settings → **TERMUX CORE LINK**.
3. Paste the token printed by the bridge.
4. Tap **SAVE TOKEN**, then **TEST CONNECTION**.
5. Open Chat and send a normal request. When a token is configured, commands go to the Termux assistant, not the local demo planner.

The Android app stores the token encrypted using an AES-GCM key held by Android Keystore. The URL is fixed to `http://127.0.0.1:8765`; there is no user-editable remote host.

## Protocol

- `GET /v1/health` — authenticated health check.
- `POST /v1/command` — authenticated JSON body: `{"request_id":"...","text":"..."}`.
- Responses include `ok`, `request_id`, `reply` and `engine`.
- Duplicate request IDs are cached for the current server process to prevent a retry from executing the same command twice.
- Commands are serialised through the existing ASTRA v2 `handle_turn()` so its conversational SQLite history is not interleaved.

Both routes require `Authorization: Bearer <token>`. The server checks loopback peer and Host header, rejects other routes, caps request bodies and never logs prompts, tokens or replies.

## Confirmation and privacy

The bridge sets ASTRA v2 popup mode before importing the backend. Existing risky tools therefore use the backend's Termux:API confirmation dialog. If that confirmation UI is unavailable, ASTRA v2 denies the risky action. No raw shell endpoint is added.

Provider API keys stay in ASTRA's private `~/astra/.env`; they are never copied into the APK or sent to the Android app. If an older ASTRA source copy contains embedded provider keys, revoke those keys and replace them before using the backend.

## Troubleshooting

- **Backend handler not found:** install the ASTRA v2 pack first; the bridge requires its callable `handle_turn()` interface.
- **Connection refused:** leave the Termux bridge running and retry Test Connection.
- **401 Unauthorized:** run `python ~/astra/bridge_server.py --show-token` and replace the saved token in ASTRA Settings.
- **Confirmation dialog missing:** check Termux:API installation and its Android permission.
- **App returns to local demo:** no token is configured. A saved token is never silently replaced with mock responses; if the bridge is configured but unavailable, ASTRA shows an offline error and does not fall back to mock mode.
