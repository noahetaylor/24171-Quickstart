"""Tests for the Pod Placer server. Run: python tools/pod-placer/test_server.py

Works on a temp copy of OdometryGeometry.java, never the real file.
"""

import http.client
import json
import shutil
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from pathlib import Path

import server


class PodPlacerServerTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.file = self.tmp / "OdometryGeometry.java"
        shutil.copy(server.GEOMETRY_FILE, self.file)
        self.real_file = server.GEOMETRY_FILE
        server.GEOMETRY_FILE = self.file
        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        self.port = self.httpd.server_address[1]
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()

    def tearDown(self):
        self.httpd.shutdown()
        self.httpd.server_close()
        server.GEOMETRY_FILE = self.real_file
        shutil.rmtree(self.tmp)

    def request(self, method, body=None, origin=None):
        conn = http.client.HTTPConnection("127.0.0.1", self.port)
        headers = {"Content-Type": "application/json"}
        if origin:
            headers["Origin"] = origin
        conn.request(method, "/api/geometry", json.dumps(body) if body is not None else None, headers)
        res = conn.getresponse()
        data = json.loads(res.read())
        conn.close()
        return res.status, data

    def load(self):
        status, data = self.request("GET")
        self.assertEqual(200, status)
        return data["values"], data["hash"]

    def test_reads_current_values(self):
        values, _ = self.load()
        self.assertEqual(-7.5, values["PORT0_POD_FORWARD_IN"])
        self.assertEqual(90, values["PORT0_POD_ARROW_DEG"])

    def test_save_rewrites_only_the_numbers(self):
        before = self.file.read_text(encoding="utf-8")
        values, h = self.load()
        values["PORT3_POD_LEFT_IN"] = -4.3
        status, _ = self.request("POST", {"values": values, "baseHash": h})
        self.assertEqual(200, status)
        after = self.file.read_text(encoding="utf-8")
        self.assertIn("PORT3_POD_LEFT_IN = -4.3;", after)
        # Every line except the edited one is byte-identical (comments kept).
        changed = [(a, b) for a, b in zip(before.splitlines(), after.splitlines()) if a != b]
        self.assertEqual(1, len(changed))
        self.assertEqual(len(before.splitlines()), len(after.splitlines()))

    def test_refuses_to_overwrite_a_file_changed_on_disk(self):
        values, h = self.load()
        self.file.write_text(self.file.read_text(encoding="utf-8") + "\n// edited in Android Studio\n", encoding="utf-8")
        values["PORT3_POD_LEFT_IN"] = -1.0
        status, _ = self.request("POST", {"values": values, "baseHash": h})
        self.assertEqual(409, status)
        self.assertIn("edited in Android Studio", self.file.read_text(encoding="utf-8"))
        self.assertNotIn("= -1.0;", self.file.read_text(encoding="utf-8"))

    def test_force_overwrite_keeps_outside_edits_except_values(self):
        values, h = self.load()
        self.file.write_text(self.file.read_text(encoding="utf-8") + "\n// edited in Android Studio\n", encoding="utf-8")
        values["PORT3_POD_LEFT_IN"] = -1.0
        status, _ = self.request("POST", {"values": values, "baseHash": h, "force": True})
        self.assertEqual(200, status)
        text = self.file.read_text(encoding="utf-8")
        self.assertIn("PORT3_POD_LEFT_IN = -1.0;", text)
        self.assertIn("edited in Android Studio", text)

    def test_rejects_two_pods_on_the_same_axis(self):
        values, h = self.load()
        values["PORT0_POD_ARROW_DEG"] = 0
        status, _ = self.request("POST", {"values": values, "baseHash": h})
        self.assertEqual(400, status)

    def test_rejects_bad_numbers(self):
        values, h = self.load()
        for bad in [{"PORT0_POD_ARROW_DEG": 45}, {"PORT0_POD_LEFT_IN": 500}, {"CHASSIS_WIDTH_IN": 0},
                    {"PORT0_POD_LEFT_IN": "4"}, {"PORT0_POD_LEFT_IN": True}]:
            status, _ = self.request("POST", {"values": {**values, **bad}, "baseHash": h})
            self.assertEqual(400, status, bad)

    def test_rejects_posts_from_other_websites(self):
        values, h = self.load()
        status, _ = self.request("POST", {"values": values, "baseHash": h}, origin="https://evil.example")
        self.assertEqual(403, status)

    def test_saved_file_parses_back_identically(self):
        values, h = self.load()
        values.update(PORT0_POD_FORWARD_IN=-7.55, PORT0_POD_LEFT_IN=5.0, CHASSIS_WIDTH_IN=14.0625)
        self.request("POST", {"values": values, "baseHash": h})
        again, _ = self.load()
        self.assertEqual(values, again)


if __name__ == "__main__":
    unittest.main()
