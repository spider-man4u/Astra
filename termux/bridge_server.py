#!/usr/bin/env python3
"""Loopback-only authenticated HTTP bridge for the ASTRA v2 Termux backend.

This is a sidecar: it imports ~/astra/astra.py and calls handle_turn().
It never copies provider keys into the Android APK and never listens on LAN.
"""
from __future__ import annotations

import argparse
import hmac
import ipaddress
import json
import os
import secrets
import stat
import sys
import threading
from collections import OrderedDict
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Callable
from urllib.parse import urlsplit

HOST = "127.0.0.1"
DEFAULT_PORT = 8765
MAX_BODY_BYTES = 16 * 1024
MAX_COMMAND_CHARS = 4000
MAX_CACHE_ENTRIES = 64


def token_path() -> Path:
    config_home = Path(os.environ.get("XDG_CONFIG_HOME", str(Path.home() / ".config"))).expanduser()
    return config_home / "astra" / "bridge.token"


def load_or_create_token(path: Path | None = None) -> tuple[str, bool]:
    """Load the local bearer token or create it with owner-only permissions."""
    target = path or token_path()
    target.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    try:
        os.chmod(target.parent, 0o700)
    except OSError:
        pass

    if target.exists():
        if target.is_symlink() or not target.is_file():
            raise RuntimeError("Bridge token path must be a regular file, not a symlink.")
        try:
            os.chmod(target, 0o600)
        except OSError as exc:
            raise RuntimeError("Could not secure the bridge token file.") from exc
        token = target.read_text(encoding="utf-8").strip()
        if len(token) < 32:
            raise RuntimeError("Bridge token file is invalid. Remove it and restart to create a new token.")
        return token, False

    token = secrets.token_urlsafe(32)
    temporary = target.with_suffix(".tmp")
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
    fd = os.open(str(temporary), flags, 0o600)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as stream:
            stream.write(token + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, target)
        os.chmod(target, 0o600)
    except Exception:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass
        raise
    return token, True


class AstraBridgeHTTPServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def __init__(
        self,
        server_address: tuple[str, int],
        token: str,
        command_handler: Callable[[str], str],
    ) -> None:
        if server_address[0] != HOST:
            raise ValueError("ASTRA bridge may bind only to 127.0.0.1.")
        super().__init__(server_address, AstraBridgeRequestHandler)
        self.bridge_token = token
        self.command_handler = command_handler
        self.command_lock = threading.Lock()
        self.response_cache: OrderedDict[str, tuple[str, str]] = OrderedDict()


class AstraBridgeRequestHandler(BaseHTTPRequestHandler):
    server: AstraBridgeHTTPServer
    protocol_version = "HTTP/1.1"
    server_version = "ASTRA-Termux-Bridge/1"
    sys_version = ""

    def log_message(self, fmt: str, *args: object) -> None:
        # Do not log request bodies, tokens, prompts or backend responses.
        sys.stderr.write("[ASTRA bridge] " + (fmt % args) + "\n")

    def _json(self, status: int, payload: dict[str, object]) -> None:
        body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def _is_loopback_request(self) -> bool:
        try:
            peer = ipaddress.ip_address(self.client_address[0].split("%", 1)[0])
        except (ValueError, IndexError):
            return False
        host_header = self.headers.get("Host", "")
        host_name = host_header.rsplit(":", 1)[0].strip("[]").lower()
        return peer.is_loopback and host_name in {"127.0.0.1", "localhost"}

    def _authorized(self) -> bool:
        if not self._is_loopback_request():
            self._json(403, {"ok": False, "error": "loopback_only"})
            return False
        supplied = self.headers.get("Authorization", "")
        if not supplied.startswith("Bearer "):
            self._json(401, {"ok": False, "error": "unauthorized"})
            return False
        candidate = supplied[7:].strip()
        if not hmac.compare_digest(candidate, self.server.bridge_token):
            self._json(401, {"ok": False, "error": "unauthorized"})
            return False
        return True

    def do_GET(self) -> None:
        if urlsplit(self.path).path != "/v1/health":
            self._json(404, {"ok": False, "error": "not_found"})
            return
        if not self._authorized():
            return
        self._json(
            200,
            {
                "ok": True,
                "service": "astra-termux",
                "protocol": 1,
                "backend": "ready",
            },
        )

    def do_POST(self) -> None:
        if urlsplit(self.path).path != "/v1/command":
            self._json(404, {"ok": False, "error": "not_found"})
            return
        if not self._authorized():
            return

        content_type = self.headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
        if content_type != "application/json":
            self._json(415, {"ok": False, "error": "json_required"})
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self._json(400, {"ok": False, "error": "invalid_length"})
            return
        if length <= 0 or length > MAX_BODY_BYTES:
            self._json(413 if length > MAX_BODY_BYTES else 400, {"ok": False, "error": "invalid_body_size"})
            return

        try:
            raw = self.rfile.read(length)
            body = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self._json(400, {"ok": False, "error": "invalid_json"})
            return

        if not isinstance(body, dict):
            self._json(400, {"ok": False, "error": "invalid_request"})
            return

        request_id = body.get("request_id")
        command = body.get("text")
        if not isinstance(request_id, str) or not request_id.strip() or len(request_id) > 80:
            self._json(400, {"ok": False, "error": "invalid_request_id"})
            return
        if not isinstance(command, str) or not command.strip() or len(command) > MAX_COMMAND_CHARS:
            self._json(400, {"ok": False, "error": "invalid_command"})
            return

        request_id = request_id.strip()
        command = command.strip()

        try:
            # ASTRA v2 has one conversational SQLite history. Serialize turns so
            # concurrent Android requests cannot interleave context and DB updates.
            with self.server.command_lock:
                cached = self.server.response_cache.get(request_id)
                if cached is not None:
                    cached_command, cached_reply = cached
                    if not hmac.compare_digest(cached_command, command):
                        self._json(409, {"ok": False, "request_id": request_id, "error": "request_id_conflict"})
                        return
                    reply = cached_reply
                else:
                    reply = self.server.command_handler(command)
                    if not isinstance(reply, str):
                        raise TypeError("ASTRA v2 handler did not return text.")
                    reply = reply[:10000]
                    self.server.response_cache[request_id] = (command, reply)
                    self.server.response_cache.move_to_end(request_id)
                    while len(self.server.response_cache) > MAX_CACHE_ENTRIES:
                        self.server.response_cache.popitem(last=False)

            self._json(
                200,
                {
                    "ok": True,
                    "request_id": request_id,
                    "reply": reply,
                    "engine": "astra-termux",
                },
            )
        except Exception:
            # Keep exception messages out of the client response; they may contain
            # paths, provider details or other private backend context.
            sys.stderr.write("[ASTRA bridge] command handler failed (details withheld).\n")
            self._json(
                500,
                {
                    "ok": False,
                    "request_id": request_id,
                    "error": "backend_error",
                    "message": "ASTRA's Termux backend could not complete this request. Check Termux for details.",
                },
            )

    def do_PUT(self) -> None:
        self._json(405, {"ok": False, "error": "method_not_allowed"})

    def do_DELETE(self) -> None:
        self._json(405, {"ok": False, "error": "method_not_allowed"})


def load_astra_v2_handler(astra_home: Path) -> Callable[[str], str]:
    """Import ASTRA v2 without starting its interactive CLI."""
    backend_file = astra_home / "astra.py"
    if not backend_file.is_file():
        raise RuntimeError(f"ASTRA backend not found: {backend_file}")

    os.environ["ASTRA_HOME"] = str(astra_home)
    # Risky ASTRA v2 tools use Termux:API confirmation dialogs in popup mode.
    # If that confirmation UI is unavailable, the backend fails closed.
    os.environ["ASTRA_POPUP_MODE"] = "1"
    sys.path.insert(0, str(astra_home))
    os.chdir(astra_home)

    import astra  # imported after ASTRA_HOME and popup mode are set

    required = ("init_db", "handle_turn", "doctor_report", "command_response")
    missing = [name for name in required if not callable(getattr(astra, name, None))]
    if missing:
        raise RuntimeError(
            "The installed ~/astra/astra.py is not the ASTRA v2 handler build. "
            "Use the ASTRA v2 upgrade pack first; this bridge will not modify your backend."
        )

    astra.init_db()
    astra.doctor_report(short=True)
    return astra.handle_turn


def main() -> int:
    parser = argparse.ArgumentParser(description="ASTRA Termux loopback bridge")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--show-token", action="store_true", help="print the local bridge token")
    args = parser.parse_args()

    if not 1024 <= args.port <= 65535:
        parser.error("--port must be between 1024 and 65535")

    try:
        token, created = load_or_create_token()
        if args.show_token:
            print(token)
            return 0

        astra_home = Path(os.environ.get("ASTRA_HOME", str(Path.home() / "astra"))).expanduser()
        command_handler = load_astra_v2_handler(astra_home)
        server = AstraBridgeHTTPServer((HOST, args.port), token, command_handler)
    except Exception as exc:
        print(f"ASTRA bridge setup failed: {exc}", file=sys.stderr)
        return 1

    print(f"ASTRA Termux bridge listening on http://{HOST}:{args.port}")
    print("Loopback only. Keep this Termux session alive while ASTRA uses the backend.")
    if created:
        print("A new access token was created. Copy it into ASTRA → Settings → Termux Core Link:")
        print(token)
        print("The token is stored with owner-only file permissions in ~/.config/astra/bridge.token")
    else:
        print("Bridge token already exists. Run: python ~/astra/bridge_server.py --show-token")
    print("Risky backend actions will use Termux:API confirmation dialogs (popup mode).")
    try:
        server.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        print("\nStopping ASTRA bridge.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
