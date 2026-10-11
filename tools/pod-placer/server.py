"""Pod Placer — laptop-side visual editor for odometry pod placement.

Serves index.html on http://127.0.0.1 and reads/writes
TeamCode/.../config/OdometryGeometry.java. Nothing runs on the robot; the
saved file reaches the hub the normal way (build + deploy in Android Studio).

Safety:
  * The file is only written when the page sends Save.
  * Every save carries the hash of the file version the page loaded. If the
    file changed on disk since then (Android Studio edit, git pull, ...),
    the save is refused with 409 unless the user explicitly chooses
    "overwrite".
  * Only the numbers on the known "NAME = value;" lines are replaced, so
    comments and anything else in the file survive.

Usage:  python tools/pod-placer/server.py      (Python 3.8+, stdlib only)
"""

import hashlib
import json
import math
import os
import re
import sys
import tempfile
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
GEOMETRY_FILE = REPO / "TeamCode/src/main/java/org/firstinspires/ftc/teamcode/config/OdometryGeometry.java"
INDEX = HERE / "index.html"

# name -> kind. Order is the order the page shows them.
FIELDS = {
    "CHASSIS_WIDTH_IN": "double",
    "CHASSIS_LENGTH_IN": "double",
    "PORT0_POD_FORWARD_IN": "double",
    "PORT0_POD_LEFT_IN": "double",
    "PORT0_POD_ARROW_DEG": "arrow",
    "PORT3_POD_FORWARD_IN": "double",
    "PORT3_POD_LEFT_IN": "double",
    "PORT3_POD_ARROW_DEG": "arrow",
}
ARROWS = (0, 90, 180, 270)
MAX_ABS_IN = 36.0  # anything bigger is a typo, not a robot


def line_pattern(name):
    return re.compile(r"(\b" + name + r"\s*=\s*)([-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?)(\s*;)")


def display_path():
    try:
        return str(GEOMETRY_FILE.relative_to(REPO)).replace("\\", "/")
    except ValueError:
        return str(GEOMETRY_FILE)


def file_hash(data):
    return hashlib.sha256(data).hexdigest()


def read_geometry():
    """Returns (values, hash). Raises ValueError naming any missing field."""
    data = GEOMETRY_FILE.read_bytes()
    text = data.decode("utf-8")
    values, missing = {}, []
    for name, kind in FIELDS.items():
        m = line_pattern(name).search(text)
        if not m:
            missing.append(name)
            continue
        values[name] = int(float(m.group(2))) if kind == "arrow" else float(m.group(2))
    if missing:
        raise ValueError("OdometryGeometry.java is missing a 'NAME = value;' line for: " + ", ".join(missing))
    return values, file_hash(data)


def validate(values):
    """Returns a cleaned dict, or raises ValueError with a readable reason."""
    clean = {}
    for name, kind in FIELDS.items():
        if name not in values:
            raise ValueError("missing " + name)
        v = values[name]
        if not isinstance(v, (int, float)) or isinstance(v, bool) or not math.isfinite(v):
            raise ValueError(name + " must be a number")
        if kind == "arrow":
            if int(v) != v or int(v) not in ARROWS:
                raise ValueError(name + " must be one of 0, 90, 180, 270")
            clean[name] = int(v)
        else:
            if abs(v) > MAX_ABS_IN:
                raise ValueError(name + " is more than " + str(MAX_ABS_IN) + " in")
            clean[name] = float(v)
    if clean["CHASSIS_WIDTH_IN"] <= 0 or clean["CHASSIS_LENGTH_IN"] <= 0:
        raise ValueError("chassis size must be positive")
    rolls_fwd = [clean[p + "_POD_ARROW_DEG"] in (0, 180) for p in ("PORT0", "PORT3")]
    if rolls_fwd[0] == rolls_fwd[1]:
        raise ValueError("one pod must roll front-to-back and the other side-to-side")
    return clean


def format_value(kind, v):
    if kind == "arrow":
        return str(int(v))
    s = ("%.4f" % v).rstrip("0")
    return s + "0" if s.endswith(".") else s


def write_geometry(values):
    text = GEOMETRY_FILE.read_bytes().decode("utf-8")
    for name, kind in FIELDS.items():
        text, n = line_pattern(name).subn(
            lambda m, k=kind, v=values[name]: m.group(1) + format_value(k, v) + m.group(3), text, count=1)
        if n != 1:
            raise ValueError("could not find the line for " + name)
    data = text.encode("utf-8")
    # Write to a temp file in the same folder, then swap it in, so a crash
    # mid-write can never leave a half-written source file.
    fd, tmp = tempfile.mkstemp(dir=str(GEOMETRY_FILE.parent), prefix=".OdometryGeometry.", suffix=".tmp")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        os.replace(tmp, GEOMETRY_FILE)
    except BaseException:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise
    return file_hash(data)


class Handler(BaseHTTPRequestHandler):
    lock = threading.Lock()

    def log_message(self, fmt, *args):  # keep the console quiet
        pass

    def send_json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            body = INDEX.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif self.path == "/api/geometry":
            try:
                values, h = read_geometry()
                self.send_json(200, {"values": values, "hash": h, "file": display_path()})
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
        else:
            self.send_json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/api/geometry":
            self.send_json(404, {"error": "not found"})
            return
        # Only accept saves from our own page (blocks other websites in the
        # browser from posting to this local port).
        origin = self.headers.get("Origin")
        port = self.server.server_address[1]
        if origin and origin not in ("http://127.0.0.1:%d" % port, "http://localhost:%d" % port):
            self.send_json(403, {"error": "bad origin"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            req = json.loads(self.rfile.read(length) or b"{}")
            values = validate(req.get("values") or {})
        except (ValueError, json.JSONDecodeError) as e:
            self.send_json(400, {"error": str(e)})
            return
        with Handler.lock:
            try:
                _, disk_hash = read_geometry()
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
                return
            if disk_hash != req.get("baseHash") and not req.get("force"):
                self.send_json(409, {"error": "OdometryGeometry.java changed on disk since you loaded it",
                                     "hash": disk_hash})
                return
            try:
                new_hash = write_geometry(values)
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
                return
        self.send_json(200, {"hash": new_hash, "values": values})


def main():
    if not GEOMETRY_FILE.exists():
        sys.exit("Can't find " + str(GEOMETRY_FILE))
    server = None
    for port in [8765, 8766, 8767, 0]:
        try:
            server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
            break
        except OSError:
            continue
    url = "http://127.0.0.1:%d/" % server.server_address[1]
    print("Pod Placer running at " + url)
    print("Editing " + str(GEOMETRY_FILE))
    print("Close this window (or Ctrl+C) to stop.")
    if "--no-browser" not in sys.argv:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
