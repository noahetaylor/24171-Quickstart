"""Partner Planner — laptop tool for drawing where our partner's Auto goes.

Serves index.html on http://127.0.0.1, keeps plans as text files in
data/partner-plans/ (one per partner), and sends them to the robot with adb
("Send to robot"; in the pit or queue: R704 only bans laptops on the robot
Wi-Fi during matches). On the robot, Auto's INIT partner page picks one.

File format (read by TeamCode auto/PartnerPlan.fromText; keep in sync):
    name=14250 left push
    zone=x0,y0,x1,y1,fromS,toS        (inches, RED frame; seconds into Auto)

Safety: a plan file is only written on Save, and refused (409) if it
changed on disk since the page loaded it, unless the user overwrites.

Usage:  python tools/partner-planner/server.py      (Python 3.8+, stdlib only)
"""

import hashlib
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
PLAN_DIR = REPO / "data/partner-plans"
INDEX = HERE / "index.html"
ROBOT_PLAN_DIR = "/sdcard/FIRST/data/partner-plans"
ROBOT_ADB_ADDRESS = "192.168.43.1:5555"

MAX_ZONES = 40
FIELD = 144.0
AUTO_S = 30.0
NAME_OK = re.compile(r"^[A-Za-z0-9 _.,()#+-]{1,60}$")


def file_hash(data):
    return hashlib.sha256(data).hexdigest()


def slug(name):
    s = re.sub(r"[^A-Za-z0-9]+", "-", name).strip("-").lower()
    return (s or "plan")[:50] + ".txt"


def fmt(v):
    s = ("%.2f" % v).rstrip("0").rstrip(".")
    return s if s not in ("", "-0") else "0"


def to_text(name, zones):
    lines = ["name=" + name]
    for z in zones:
        lines.append("zone=" + ",".join(fmt(v) for v in z))
    return "\n".join(lines) + "\n"


def parse(text):
    """(name, zones) from the file format; bad lines skipped (same rules as the robot)."""
    name, zones = "", []
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("name="):
            name = line[5:].strip()
        elif line.startswith("zone="):
            f = line[5:].split(",")
            if len(f) != 6:
                continue
            try:
                v = [float(x) for x in f]
            except ValueError:
                continue
            if v[5] > v[4]:
                zones.append([min(v[0], v[2]), min(v[1], v[3]), max(v[0], v[2]), max(v[1], v[3]), v[4], v[5]])
    return name, zones


def validate(name, zones):
    if not isinstance(name, str) or not NAME_OK.match(name.strip()):
        raise ValueError("name: 1-60 letters, numbers, spaces or _ . , ( ) # + -")
    if not isinstance(zones, list) or len(zones) > MAX_ZONES:
        raise ValueError("at most %d zones" % MAX_ZONES)
    clean = []
    for z in zones:
        if not isinstance(z, list) or len(z) != 6:
            raise ValueError("each zone is [x0, y0, x1, y1, fromS, toS]")
        for v in z:
            if not isinstance(v, (int, float)) or isinstance(v, bool) or not math.isfinite(v):
                raise ValueError("zones must be numbers")
        x0, y0, x1, y1, t0, t1 = (float(v) for v in z)
        if not all(0 <= v <= FIELD for v in (x0, y0, x1, y1)):
            raise ValueError("zone outside the field")
        if abs(x1 - x0) < 1 or abs(y1 - y0) < 1:
            raise ValueError("zone too small")
        if not (0 <= t0 < t1 <= AUTO_S):
            raise ValueError("zone times must be 0 <= from < to <= 30")
        clean.append([min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), t0, t1])
    return name.strip(), clean


def rel(path):
    try:
        return str(path.relative_to(REPO)).replace("\\", "/")
    except ValueError:
        return str(path)


def safe_file(f):
    if not isinstance(f, str) or not re.match(r"^[a-z0-9-]{1,50}\.txt$", f):
        raise ValueError("bad file name")
    return PLAN_DIR / f


def read_plans():
    plans = []
    if PLAN_DIR.is_dir():
        for p in sorted(PLAN_DIR.glob("*.txt")):
            data = p.read_bytes()
            name, zones = parse(data.decode("utf-8", errors="replace"))
            plans.append({"file": p.name, "name": name or p.stem, "zones": zones, "hash": file_hash(data)})
    return plans


def write_atomic(path, text):
    PLAN_DIR.mkdir(parents=True, exist_ok=True)
    data = text.encode("utf-8")
    fd, tmp = tempfile.mkstemp(dir=str(PLAN_DIR), prefix=".plan.", suffix=".tmp")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        os.replace(tmp, path)
    except BaseException:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise
    return file_hash(data)


def find_adb():
    found = shutil.which("adb")
    if found:
        return found
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") \
        or os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk")
    cand = os.path.join(sdk, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
    return cand if os.path.isfile(cand) else None


def push_to_robot():
    adb = find_adb()
    if not adb:
        raise ValueError("adb not found. Install Android Studio's platform-tools.")
    files = sorted(PLAN_DIR.glob("*.txt")) if PLAN_DIR.is_dir() else []
    out = []

    def run(*args):
        r = subprocess.run([adb] + list(args), capture_output=True, text=True, timeout=60)
        out.append(("$ adb " + " ".join(args) + "\n" + (r.stdout + r.stderr).strip()).strip())
        return r.returncode

    run("connect", ROBOT_ADB_ADDRESS)   # harmless if already connected (or on USB)
    if run("shell", "mkdir", "-p", ROBOT_PLAN_DIR) != 0:
        raise ValueError("Can't reach the robot. Laptop on the robot's Wi-Fi (or USB)?\n\n" + "\n\n".join(out))
    # Mirror: the robot gets exactly the plans on this laptop.
    run("shell", "rm", "-f", ROBOT_PLAN_DIR + "/*.txt")
    for f in files:
        if run("push", str(f), ROBOT_PLAN_DIR + "/" + f.name) != 0:
            raise ValueError("Push failed for " + f.name + "\n\n" + "\n\n".join(out))
    return {"sent": [f.name for f in files], "log": "\n\n".join(out)}


class Handler(BaseHTTPRequestHandler):
    lock = threading.Lock()

    def log_message(self, fmt_, *args):
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
        elif self.path == "/api/plans":
            try:
                self.send_json(200, {"plans": read_plans(), "dir": rel(PLAN_DIR)})
            except OSError as e:
                self.send_json(500, {"error": str(e)})
        else:
            self.send_json(404, {"error": "not found"})

    def do_POST(self):
        origin = self.headers.get("Origin")
        port = self.server.server_address[1]
        if origin and origin not in ("http://127.0.0.1:%d" % port, "http://localhost:%d" % port):
            self.send_json(403, {"error": "bad origin"})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            req = json.loads(self.rfile.read(length) or b"{}")
        except (ValueError, json.JSONDecodeError) as e:
            self.send_json(400, {"error": str(e)})
            return
        try:
            if self.path == "/api/save":
                self.save(req)
            elif self.path == "/api/delete":
                with Handler.lock:
                    path = safe_file(req.get("file"))
                    if path.exists():
                        path.unlink()
                self.send_json(200, {"ok": True})
            elif self.path == "/api/push":
                self.send_json(200, push_to_robot())
            else:
                self.send_json(404, {"error": "not found"})
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
        except (OSError, subprocess.SubprocessError) as e:
            self.send_json(502, {"error": str(e)})

    def save(self, req):
        name, zones = validate(req.get("name"), req.get("zones"))
        new_file = slug(name)
        old_file = req.get("file")
        with Handler.lock:
            path = PLAN_DIR / new_file
            if old_file and old_file != new_file:
                # Renamed: don't silently replace a different plan that already has the new name.
                if path.exists() and not req.get("force"):
                    self.send_json(409, {"error": "A plan named like \"%s\" already exists" % name})
                    return
            elif path.exists() and not req.get("force"):
                disk = file_hash(path.read_bytes())
                if disk != req.get("baseHash"):
                    self.send_json(409, {"error": "This plan's file changed on disk since you opened it"})
                    return
            h = write_atomic(path, to_text(name, zones))
            if old_file and old_file != new_file:
                old = safe_file(old_file)
                if old.exists():
                    old.unlink()
        self.send_json(200, {"file": new_file, "hash": h, "name": name, "zones": zones})


def main():
    server = None
    for port in [8785, 8786, 8787, 0]:
        try:
            server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
            break
        except OSError:
            continue
    url = "http://127.0.0.1:%d/" % server.server_address[1]
    print("Partner Planner running at " + url)
    print("Plans in " + str(PLAN_DIR))
    print("Close this window (or Ctrl+C) to stop.")
    if "--no-browser" not in sys.argv:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
