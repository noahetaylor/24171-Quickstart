"""Shot Table Editor — laptop-side tool for tuning the shot tables from data.

Serves index.html on http://127.0.0.1 and:
  * reads shot logs (CSV from the Shot Tuner OpMode) in data/shot-logs/,
  * pulls new logs from the robot with adb ("Pull from robot"),
  * reads and writes the table rows in
    TeamCode/.../tuning/ShotTables.java (only between the BEGIN/END markers).

Nothing runs on the robot; the saved file reaches the hub the normal way
(build + deploy in Android Studio).

Safety (same rules as the Pod Placer):
  * ShotTables.java is only written when the page sends Save.
  * Every save carries the hash of the version the page loaded; if the file
    changed on disk since, the save is refused (409) unless the user
    explicitly chooses to overwrite.
  * Only the text between the markers is replaced.

Usage:  python tools/shot-table-editor/server.py      (Python 3.8+, stdlib only)
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
JAVA = REPO / "TeamCode/src/main/java/org/firstinspires/ftc/teamcode"
TABLES_FILE = JAVA / "tuning/ShotTables.java"
SHOOTER_TUNING = JAVA / "tuning/ShooterTuning.java"
SHOOTER_FILE = JAVA / "shooting/Shooter.java"
FIELD_FILE = JAVA / "field/Field.java"
LOG_DIR = REPO / "data/shot-logs"
INDEX = HERE / "index.html"

ROBOT_LOG_DIR = "/sdcard/FIRST/data/shots"
ROBOT_ADB_ADDRESS = "192.168.43.1:5555"   # Control Hub over its own Wi-Fi

SHOOTERS = ("POLLEN", "NECTAR")
CSV_FIELDS = ["time", "shooter", "result", "distance_in", "hood_deg", "rpm_target",
              "rpm_actual", "distance_source", "x", "y", "heading_deg"]
NUM = r"[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?"
ROW_RE = re.compile(r"\.add\(\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*\)")

LIMITS = {"distance": (6.0, 200.0), "hood": (20.0, 80.0), "rpm": (0.0, 6500.0)}


def file_hash(data):
    return hashlib.sha256(data).hexdigest()


def rel(path):
    try:
        return str(path.relative_to(REPO)).replace("\\", "/")
    except ValueError:
        return str(path)


# ------------------------------------------------------------------ tables

def region(text, shooter):
    """(start, end) of the text strictly between the BEGIN and END marker lines."""
    begin = re.search(r"^[ \t]*// BEGIN " + shooter + r" ROWS[^\n]*\n", text, re.M)
    end = re.search(r"^[ \t]*// END " + shooter + r" ROWS", text, re.M)
    if not begin or not end or end.start() < begin.end():
        raise ValueError("ShotTables.java is missing the '// BEGIN %s ROWS' / '// END %s ROWS' markers"
                         % (shooter, shooter))
    return begin.end(), end.start()


def read_tables():
    data = TABLES_FILE.read_bytes()
    text = data.decode("utf-8")
    tables = {}
    for s in SHOOTERS:
        a, b = region(text, s)
        tables[s] = [[float(m.group(1)), float(m.group(2)), float(m.group(3))]
                     for m in ROW_RE.finditer(text[a:b])]
    return tables, file_hash(data)


def validate_rows(rows, shooter):
    if not isinstance(rows, list):
        raise ValueError(shooter + " rows must be a list")
    clean = []
    for r in rows:
        if not isinstance(r, list) or len(r) != 3:
            raise ValueError(shooter + ": each row is [distance, hood, rpm]")
        for v in r:
            if not isinstance(v, (int, float)) or isinstance(v, bool) or not math.isfinite(v):
                raise ValueError(shooter + ": rows must be numbers")
        d, h, rpm = float(r[0]), float(r[1]), float(r[2])
        for name, v in (("distance", d), ("hood", h), ("rpm", rpm)):
            lo, hi = LIMITS[name]
            if not lo <= v <= hi:
                raise ValueError("%s: %s %.1f is outside %.0f..%.0f" % (shooter, name, v, lo, hi))
        clean.append([round(d, 1), round(h, 1), round(rpm)])
    clean.sort(key=lambda r: r[0])
    for i in range(1, len(clean)):
        if clean[i][0] == clean[i - 1][0]:
            raise ValueError("%s: two rows at %.1f in" % (shooter, clean[i][0]))
    return clean


def fmt(v):
    s = ("%.1f" % v)
    return s[:-2] if s.endswith(".0") else s


def render(shooter, rows):
    lines = ["    public static final ShotTable %s = new ShotTable()" % shooter]
    for d, h, rpm in rows:
        lines.append("            .add(%s, %s, %d)" % (fmt(d), fmt(h), rpm))
    lines.append("            ;")
    return "\n".join(lines) + "\n"


def write_tables(tables):
    text = TABLES_FILE.read_bytes().decode("utf-8")
    # Replace from the last region backwards so earlier offsets stay valid.
    spans = sorted(((region(text, s), s) for s in SHOOTERS), key=lambda t: -t[0][0])
    for (a, b), s in spans:
        text = text[:a] + render(s, tables[s]) + text[b:]
    data = text.encode("utf-8")
    fd, tmp = tempfile.mkstemp(dir=str(TABLES_FILE.parent), prefix=".ShotTables.", suffix=".tmp")
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)
        os.replace(tmp, TABLES_FILE)
    except BaseException:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise
    return file_hash(data)


# ------------------------------------------------------------------ physics constants

def find_number(path, name):
    m = re.search(r"\b" + name + r"\s*=\s*(" + NUM + r")\s*;", path.read_text(encoding="utf-8"))
    return float(m.group(1)) if m else None


def physics():
    """Numbers the page needs to draw the physics (drag-free) curve; None if unreadable."""
    try:
        shooter_src = SHOOTER_FILE.read_text(encoding="utf-8")
        heights, wheels = {}, {}
        for s in SHOOTERS:
            m = re.search(s + r"\(\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*,\s*(" + NUM + r")\s*,\s*("
                          + NUM + r")\s*/\s*(" + NUM + r")\s*\)", shooter_src)
            heights[s] = float(m.group(3))
            wheels[s] = float(m.group(4)) / float(m.group(5))
        p = {
            "efficiency": find_number(SHOOTER_TUNING, "EXIT_EFFICIENCY"),
            "hoodDeg": find_number(SHOOTER_TUNING, "HOOD_MIN_DEG"),
            "maxRpm": find_number(SHOOTER_TUNING, "MAX_RPM"),
            "targetHeight": find_number(FIELD_FILE, "UP_CELL_OPENING_HEIGHT_IN"),
            "releaseHeight": heights,
            "wheelDiameter": wheels,
        }
        return p if all(v is not None for v in p.values()) else None
    except (OSError, AttributeError, ValueError):
        return None


# ------------------------------------------------------------------ shot logs

def parse_csv(text, source):
    """Rows as dicts; bad lines are skipped and counted."""
    shots, bad = [], 0
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("time,"):
            continue
        f = line.split(",")
        try:
            if len(f) != len(CSV_FIELDS) or f[1] not in SHOOTERS or f[2] not in ("HIT", "MISS"):
                raise ValueError
            shots.append({"time": f[0], "shooter": f[1], "hit": f[2] == "HIT",
                          "distance": float(f[3]), "hood": float(f[4]),
                          "rpmTarget": float(f[5]), "rpmActual": float(f[6]),
                          "distanceSource": f[7], "file": source})
        except ValueError:
            bad += 1
    return shots, bad


def read_logs():
    shots, files, bad = [], [], 0
    if LOG_DIR.is_dir():
        for p in sorted(LOG_DIR.glob("*.csv")):
            s, b = parse_csv(p.read_text(encoding="utf-8", errors="replace"), p.name)
            shots += s
            bad += b
            files.append({"name": p.name, "count": len(s)})
    return {"shots": shots, "files": files, "badLines": bad, "dir": rel(LOG_DIR)}


SAFE_NAME = re.compile(r"^[A-Za-z0-9._-]{1,100}\.csv$")


def save_upload(name, text):
    if not SAFE_NAME.match(name or ""):
        raise ValueError("file name must look like shots-....csv")
    shots, _ = parse_csv(text, name)
    if not shots:
        raise ValueError("no shot rows found in " + name)
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    (LOG_DIR / name).write_text(text, encoding="utf-8")
    return len(shots)


def find_adb():
    found = shutil.which("adb")
    if found:
        return found
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") \
        or os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk")
    cand = os.path.join(sdk, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
    return cand if os.path.isfile(cand) else None


def pull_from_robot():
    adb = find_adb()
    if not adb:
        raise ValueError("adb not found. Install Android Studio's platform-tools, or drag the CSV files in instead.")
    LOG_DIR.mkdir(parents=True, exist_ok=True)
    out = []

    def run(*args):
        r = subprocess.run([adb] + list(args), capture_output=True, text=True, timeout=60)
        out.append(("$ adb " + " ".join(args) + "\n" + (r.stdout + r.stderr).strip()).strip())
        return r.returncode

    run("connect", ROBOT_ADB_ADDRESS)  # harmless if already connected (or on USB)
    before = {p.name for p in LOG_DIR.glob("*.csv")}
    code = run("pull", ROBOT_LOG_DIR + "/.", str(LOG_DIR))
    after = {p.name for p in LOG_DIR.glob("*.csv")}
    if code != 0:
        raise ValueError("adb pull failed. Is the laptop on the robot's Wi-Fi (or plugged in by USB)?\n\n"
                         + "\n\n".join(out))
    return {"newFiles": sorted(after - before), "log": "\n\n".join(out)}


# ------------------------------------------------------------------ HTTP

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
        elif self.path == "/api/tables":
            try:
                tables, h = read_tables()
                self.send_json(200, {"tables": tables, "hash": h, "file": rel(TABLES_FILE),
                                     "physics": physics()})
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
        elif self.path == "/api/shots":
            try:
                self.send_json(200, read_logs())
            except OSError as e:
                self.send_json(500, {"error": str(e)})
        else:
            self.send_json(404, {"error": "not found"})

    def do_POST(self):
        # Only accept requests from our own page (blocks other websites in the
        # browser from posting to this local port).
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

        if self.path == "/api/tables":
            self.save_tables(req)
        elif self.path == "/api/pull":
            try:
                self.send_json(200, pull_from_robot())
            except (OSError, ValueError, subprocess.SubprocessError) as e:
                self.send_json(502, {"error": str(e)})
        elif self.path == "/api/upload":
            try:
                n = save_upload(req.get("name"), req.get("text") or "")
                self.send_json(200, {"count": n})
            except (OSError, ValueError) as e:
                self.send_json(400, {"error": str(e)})
        else:
            self.send_json(404, {"error": "not found"})

    def save_tables(self, req):
        try:
            tables = {s: validate_rows((req.get("tables") or {}).get(s), s) for s in SHOOTERS}
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
            return
        with Handler.lock:
            try:
                _, disk_hash = read_tables()
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
                return
            if disk_hash != req.get("baseHash") and not req.get("force"):
                self.send_json(409, {"error": "ShotTables.java changed on disk since you loaded it",
                                     "hash": disk_hash})
                return
            try:
                new_hash = write_tables(tables)
            except (OSError, ValueError) as e:
                self.send_json(500, {"error": str(e)})
                return
        self.send_json(200, {"hash": new_hash, "tables": tables})


def main():
    if not TABLES_FILE.exists():
        sys.exit("Can't find " + str(TABLES_FILE))
    server = None
    for port in [8775, 8776, 8777, 0]:
        try:
            server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
            break
        except OSError:
            continue
    url = "http://127.0.0.1:%d/" % server.server_address[1]
    print("Shot Table Editor running at " + url)
    print("Editing " + str(TABLES_FILE))
    print("Shot logs in " + str(LOG_DIR))
    print("Close this window (or Ctrl+C) to stop.")
    if "--no-browser" not in sys.argv:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
