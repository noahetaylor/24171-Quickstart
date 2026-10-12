"""Tests for the Partner Planner server. Run: python tools/partner-planner/test_server.py

Works in a temp plan folder, never the real one.
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


class PartnerPlannerTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.real = server.PLAN_DIR
        server.PLAN_DIR = self.tmp / "plans"
        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        self.port = self.httpd.server_address[1]
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()

    def tearDown(self):
        self.httpd.shutdown()
        self.httpd.server_close()
        server.PLAN_DIR = self.real
        shutil.rmtree(self.tmp)

    def call(self, method, path, body=None, origin=None):
        conn = http.client.HTTPConnection("127.0.0.1", self.port)
        headers = {"Content-Type": "application/json"}
        if origin:
            headers["Origin"] = origin
        conn.request(method, path, json.dumps(body) if body is not None else None, headers)
        res = conn.getresponse()
        data = json.loads(res.read())
        conn.close()
        return res.status, data

    def plans(self):
        status, data = self.call("GET", "/api/plans")
        self.assertEqual(200, status)
        return data["plans"]

    def test_save_writes_the_robot_format(self):
        status, res = self.call("POST", "/api/save", {"name": "14250 left push",
                                                      "zones": [[30, 40, 50, 80.25, 0, 12.5]]})
        self.assertEqual(200, status, res)
        text = (server.PLAN_DIR / "14250-left-push.txt").read_text(encoding="utf-8")
        self.assertEqual("name=14250 left push\nzone=30,40,50,80.25,0,12.5\n", text)
        p = self.plans()[0]
        self.assertEqual("14250 left push", p["name"])
        self.assertEqual([[30, 40, 50, 80.25, 0, 12.5]], p["zones"])

    def test_zones_are_normalized(self):
        self.call("POST", "/api/save", {"name": "flip", "zones": [[50, 80, 30, 40, 0, 30]]})
        self.assertEqual([[30, 40, 50, 80, 0, 30]], self.plans()[0]["zones"])

    def test_rejects_bad_input(self):
        for body in ({"name": "", "zones": []},
                     {"name": "../x", "zones": []},
                     {"name": "ok", "zones": [[0, 0, 200, 10, 0, 30]]},     # off the field
                     {"name": "ok", "zones": [[0, 0, 10, 10, 20, 10]]},     # from > to
                     {"name": "ok", "zones": [[0, 0, 10, 10, 0, 45]]},      # past Auto
                     {"name": "ok", "zones": [[0, 0, 0.5, 10, 0, 30]]},     # too thin
                     {"name": "ok", "zones": [["a", 0, 10, 10, 0, 30]]}):
            status, _ = self.call("POST", "/api/save", body)
            self.assertEqual(400, status, body)
        self.assertEqual([], self.plans())

    def test_refuses_to_clobber_an_outside_edit(self):
        self.call("POST", "/api/save", {"name": "p", "zones": []})
        p = self.plans()[0]
        (server.PLAN_DIR / p["file"]).write_text("name=p\nzone=0,0,10,10,0,30\n", encoding="utf-8")
        status, _ = self.call("POST", "/api/save", {"file": p["file"], "name": "p", "zones": [], "baseHash": p["hash"]})
        self.assertEqual(409, status)
        status, _ = self.call("POST", "/api/save", {"file": p["file"], "name": "p", "zones": [], "baseHash": p["hash"], "force": True})
        self.assertEqual(200, status)

    def test_rename_moves_the_file_but_never_overwrites_another_plan(self):
        self.call("POST", "/api/save", {"name": "a", "zones": []})
        self.call("POST", "/api/save", {"name": "b", "zones": []})
        status, _ = self.call("POST", "/api/save", {"file": "a.txt", "name": "b", "zones": []})
        self.assertEqual(409, status)
        status, _ = self.call("POST", "/api/save", {"file": "a.txt", "name": "c", "zones": []})
        self.assertEqual(200, status)
        self.assertEqual(["b.txt", "c.txt"], sorted(p["file"] for p in self.plans()))

    def test_delete_and_origin_check(self):
        self.call("POST", "/api/save", {"name": "gone", "zones": []})
        status, _ = self.call("POST", "/api/delete", {"file": "gone.txt"}, origin="http://evil.example")
        self.assertEqual(403, status)
        status, _ = self.call("POST", "/api/delete", {"file": "../../etc.txt"})
        self.assertEqual(400, status)
        status, _ = self.call("POST", "/api/delete", {"file": "gone.txt"})
        self.assertEqual(200, status)
        self.assertEqual([], self.plans())

    def test_parser_matches_the_robot_rules(self):
        name, zones = server.parse("name=x\nzone=1,2,3\nzone=a,b,c,d,e,f\nzone=0,0,10,10,9,4\nzone=0,0,10,10,1,2\n")
        self.assertEqual("x", name)
        self.assertEqual([[0, 0, 10, 10, 1, 2]], zones)


if __name__ == "__main__":
    unittest.main()
