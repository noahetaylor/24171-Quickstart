"""Tests for the Shot Table Editor server. Run: python tools/shot-table-editor/test_server.py

Works on a temp copy of ShotTables.java and a temp log folder, never the real ones.
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

LOG = ("time,shooter,result,distance_in,hood_deg,rpm_target,rpm_actual,distance_source,x,y,heading_deg\n"
       "2026-10-20T17:00:00,POLLEN,HIT,48.0,60.0,2700,2688,POSE,40.0,20.0,90.0\n"
       "2026-10-20T17:00:05,POLLEN,MISS,48.0,60.0,2600,2590,POSE,40.0,20.0,90.0\n"
       "2026-10-20T17:00:09,NECTAR,HIT,72.5,60.0,2950,2941,TAPE,0.0,0.0,0.0\n"
       "garbage line\n")


class ShotTableEditorTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.file = self.tmp / "ShotTables.java"
        shutil.copy(server.TABLES_FILE, self.file)
        self.real = (server.TABLES_FILE, server.LOG_DIR)
        server.TABLES_FILE = self.file
        server.LOG_DIR = self.tmp / "logs"
        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        self.port = self.httpd.server_address[1]
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()

    def tearDown(self):
        self.httpd.shutdown()
        self.httpd.server_close()
        server.TABLES_FILE, server.LOG_DIR = self.real
        shutil.rmtree(self.tmp)

    def request(self, method, path, body=None, origin=None):
        conn = http.client.HTTPConnection("127.0.0.1", self.port)
        headers = {"Content-Type": "application/json"}
        if origin:
            headers["Origin"] = origin
        conn.request(method, path, json.dumps(body) if body is not None else None, headers)
        res = conn.getresponse()
        data = json.loads(res.read())
        conn.close()
        return res.status, data

    def load(self):
        status, data = self.request("GET", "/api/tables")
        self.assertEqual(200, status, data)
        return data

    def test_reads_tables_and_physics(self):
        data = self.load()
        self.assertEqual({"POLLEN", "NECTAR"}, set(data["tables"]))
        p = data["physics"]
        self.assertIsNotNone(p, "physics constants should be readable from the Java files")
        self.assertAlmostEqual(0.5, p["efficiency"])
        self.assertAlmostEqual(60.0, p["hoodDeg"])
        self.assertAlmostEqual(53.5, p["targetHeight"])
        self.assertAlmostEqual(72.0 / 25.4, p["wheelDiameter"]["POLLEN"])

    def test_save_writes_rows_between_markers_only(self):
        before = self.file.read_text(encoding="utf-8")
        data = self.load()
        tables = {"POLLEN": [[72, 60, 3100], [48, 60, 2700]], "NECTAR": [[60.5, 60, 2900]]}
        status, res = self.request("POST", "/api/tables", {"tables": tables, "baseHash": data["hash"]})
        self.assertEqual(200, status, res)
        after = self.file.read_text(encoding="utf-8")
        self.assertIn(".add(48, 60, 2700)\n            .add(72, 60, 3100)", after)  # sorted
        self.assertIn(".add(60.5, 60, 2900)", after)
        # Everything outside the markers is untouched.
        self.assertEqual(before.split("// BEGIN POLLEN ROWS")[0], after.split("// BEGIN POLLEN ROWS")[0])
        self.assertEqual(before.split("// END NECTAR ROWS")[1], after.split("// END NECTAR ROWS")[1])
        # And it reads back.
        self.assertEqual([[48, 60, 2700], [72, 60, 3100]], self.load()["tables"]["POLLEN"])

    def test_refuses_when_file_changed_on_disk(self):
        data = self.load()
        self.file.write_text(self.file.read_text(encoding="utf-8") + "\n// edited elsewhere\n", encoding="utf-8")
        body = {"tables": {"POLLEN": [], "NECTAR": []}, "baseHash": data["hash"]}
        status, _ = self.request("POST", "/api/tables", body)
        self.assertEqual(409, status)
        body["force"] = True
        status, _ = self.request("POST", "/api/tables", body)
        self.assertEqual(200, status)
        self.assertIn("// edited elsewhere", self.file.read_text(encoding="utf-8"))

    def test_rejects_bad_rows(self):
        h = self.load()["hash"]
        for bad in ([[48, 60, 2700], [48, 60, 2800]],   # duplicate distance
                    [[48, 60, 99999]],                    # rpm out of range
                    [[48, 60]],                           # wrong shape
                    [["48", 60, 2700]]):                  # not a number
            status, _ = self.request("POST", "/api/tables", {"tables": {"POLLEN": bad, "NECTAR": []}, "baseHash": h})
            self.assertEqual(400, status, bad)
        self.assertEqual(h, self.load()["hash"])  # file untouched

    def test_rejects_other_origins(self):
        h = self.load()["hash"]
        status, _ = self.request("POST", "/api/tables", {"tables": {"POLLEN": [], "NECTAR": []}, "baseHash": h},
                                 origin="http://evil.example")
        self.assertEqual(403, status)

    def test_upload_and_read_logs(self):
        status, res = self.request("POST", "/api/upload", {"name": "shots-1.csv", "text": LOG})
        self.assertEqual(200, status, res)
        self.assertEqual(3, res["count"])
        status, data = self.request("GET", "/api/shots")
        self.assertEqual(200, status)
        self.assertEqual(3, len(data["shots"]))
        self.assertEqual(1, data["badLines"])
        nectar = [s for s in data["shots"] if s["shooter"] == "NECTAR"][0]
        self.assertTrue(nectar["hit"])
        self.assertEqual(72.5, nectar["distance"])

    def test_upload_rejects_odd_names(self):
        for name in ("../evil.csv", "shots.txt", ""):
            status, _ = self.request("POST", "/api/upload", {"name": name, "text": LOG})
            self.assertEqual(400, status, name)


if __name__ == "__main__":
    unittest.main()
