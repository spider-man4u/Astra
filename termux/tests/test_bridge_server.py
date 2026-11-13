import json
import sys
import threading
from pathlib import Path
import unittest
import urllib.error
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from bridge_server import AstraBridgeHTTPServer


class BridgeServerTests(unittest.TestCase):
    def setUp(self):
        self.calls = []
        self.token = "unit-test-token-" + ("x" * 40)

        def handler(command):
            self.calls.append(command)
            return "ASTRA reply: " + command

        self.server = AstraBridgeHTTPServer(("127.0.0.1", 0), self.token, handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = "http://127.0.0.1:" + str(self.server.server_address[1])

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def request(self, path, method="GET", token=None, payload=None):
        headers = {}
        if token is not None:
            headers["Authorization"] = "Bearer " + token
        data = None
        if payload is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(payload).encode()
        req = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=3) as response:
                return response.status, json.loads(response.read().decode())
        except urllib.error.HTTPError as exc:
            return exc.code, json.loads(exc.read().decode())

    def test_health_requires_token(self):
        status, body = self.request("/v1/health", token=self.token)
        self.assertEqual(200, status)
        self.assertTrue(body["ok"])
        status, body = self.request("/v1/health", token="wrong")
        self.assertEqual(401, status)
        self.assertEqual("unauthorized", body["error"])

    def test_command_calls_backend_and_returns_reply(self):
        payload = {"request_id": "req-1", "text": "hello ASTRA"}
        status, body = self.request("/v1/command", method="POST", token=self.token, payload=payload)
        self.assertEqual(200, status)
        self.assertEqual("ASTRA reply: hello ASTRA", body["reply"])
        self.assertEqual(["hello ASTRA"], self.calls)

    def test_request_id_replay_does_not_execute_twice(self):
        payload = {"request_id": "req-2", "text": "show status"}
        first = self.request("/v1/command", method="POST", token=self.token, payload=payload)
        second = self.request("/v1/command", method="POST", token=self.token, payload=payload)
        self.assertEqual(first, second)
        self.assertEqual(["show status"], self.calls)

    def test_request_id_cannot_be_reused_for_different_command(self):
        self.request(
            "/v1/command", method="POST", token=self.token,
            payload={"request_id": "req-3", "text": "first"}
        )
        status, body = self.request(
            "/v1/command", method="POST", token=self.token,
            payload={"request_id": "req-3", "text": "second"}
        )
        self.assertEqual(409, status)
        self.assertEqual("request_id_conflict", body["error"])
        self.assertEqual(["first"], self.calls)

    def test_unknown_routes_are_rejected(self):
        status, body = self.request("/anything", token=self.token)
        self.assertEqual(404, status)
        self.assertEqual("not_found", body["error"])


if __name__ == "__main__":
    unittest.main()
