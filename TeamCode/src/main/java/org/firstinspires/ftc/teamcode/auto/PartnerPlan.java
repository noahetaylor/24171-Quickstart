package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Blocker;
import org.firstinspires.ftc.teamcode.field.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pre-match partner plan: the zones on our half our partner's Auto uses,
 * and when. Auto won't chase balls, pick shot spots, or drive through a
 * zone while it's in use.
 *
 * Made on a laptop with the Partner Planner (tools/partner-planner), sent
 * to the robot over adb in the pit/queue (R704 only bans laptops on the
 * robot Wi-Fi DURING matches), and picked by name in Auto's INIT. Whole
 * tiles can also be marked on the Driver Hub as a last-minute fallback.
 *
 * Zones are stored for RED (inches, Pedro coordinates); BLUE is the same
 * plan rotated 180 degrees, so a plan means the same thing for either
 * alliance. Text format (one plan per file):
 *   name=14250 left push
 *   zone=x0,y0,x1,y1,fromS,toS
 * Pure logic, unit-tested in PartnerPlanTest.
 */
public final class PartnerPlan {

    /** Time windows for marking whole tiles on the Driver Hub. */
    public enum Window {
        FREE('.', 0, 0), ALL('#', 0, 30), EARLY('1', 0, 10), MID('2', 10, 20), LATE('3', 20, 30);

        public final char symbol;
        final double from, to;

        Window(char symbol, double from, double to) {
            this.symbol = symbol;
            this.from = from;
            this.to = to;
        }

        Window next() {
            return values()[(ordinal() + 1) % values().length];
        }

        static Window of(double from, double to) {
            for (Window w : values()) {
                if (w != FREE && Math.abs(w.from - from) < 1e-6 && Math.abs(w.to - to) < 1e-6) return w;
            }
            return null;
        }
    }

    /** A rectangle (RED frame) in use from fromS to toS seconds into Auto. */
    public static final class Zone {
        public final double x0, y0, x1, y1, fromS, toS;

        public Zone(double x0, double y0, double x1, double y1, double fromS, double toS) {
            this.x0 = Math.min(x0, x1);
            this.y0 = Math.min(y0, y1);
            this.x1 = Math.max(x0, x1);
            this.y1 = Math.max(y0, y1);
            this.fromS = fromS;
            this.toS = toS;
        }

        boolean active(double t) {
            return t >= fromS - 1e-9 && t < toS;
        }

        boolean near(double rx, double ry, double margin) {
            return rx > x0 - margin && rx < x1 + margin && ry > y0 - margin && ry < y1 + margin;
        }

        boolean isTile(int col, int row) {
            return x0 == col * Field.TILE_IN && x1 == (col + 1) * Field.TILE_IN
                    && y0 == row * Field.TILE_IN && y1 == (row + 1) * Field.TILE_IN;
        }

        boolean overlapsTile(int col, int row) {
            return x0 < (col + 1) * Field.TILE_IN && x1 > col * Field.TILE_IN
                    && y0 < (row + 1) * Field.TILE_IN && y1 > row * Field.TILE_IN;
        }
    }

    public static final int COLS = 3, ROWS = 6;
    /** Keep the robot center this far out of a zone (robot half-width, roughly). */
    public static double MARGIN_IN = 7.0;

    private String name = "";
    private final List<Zone> zones = new ArrayList<>();

    public String name() {
        return name;
    }

    public void setName(String n) {
        name = n == null ? "" : n.trim();
    }

    public List<Zone> zones() {
        return Collections.unmodifiableList(zones);
    }

    public void add(Zone z) {
        zones.add(z);
    }

    public void clear() {
        zones.clear();
        name = "";
    }

    /** Replace this plan's contents with another's. */
    public void copyFrom(PartnerPlan other) {
        zones.clear();
        zones.addAll(other.zones);
        name = other.name;
    }

    public int zoneCount() {
        return zones.size();
    }

    // ------------------------------------------------------------ whole tiles (Driver Hub fallback)

    public Window get(int col, int row) {
        for (Zone z : zones) {
            if (z.isTile(col, row)) {
                Window w = Window.of(z.fromS, z.toS);
                if (w != null) return w;
            }
        }
        return Window.FREE;
    }

    public void set(int col, int row, Window w) {
        for (int i = zones.size() - 1; i >= 0; i--) if (zones.get(i).isTile(col, row)) zones.remove(i);
        if (w != Window.FREE) {
            zones.add(new Zone(col * Field.TILE_IN, row * Field.TILE_IN,
                    (col + 1) * Field.TILE_IN, (row + 1) * Field.TILE_IN, w.from, w.to));
        }
    }

    public void cycle(int col, int row) {
        set(col, row, get(col, row).next());
    }

    /** "A1"-style name of a tile (red frame; BLUE uses the mirrored letters D..F). */
    public static String tileName(int col, int row, Alliance a) {
        char letter = a == Alliance.RED ? (char) ('A' + col) : (char) ('F' - col);
        int number = a == Alliance.RED ? row + 1 : ROWS - row;
        return "" + letter + number;
    }

    // ------------------------------------------------------------ blocking

    /** True if the RED-frame point is in or within MARGIN_IN of a zone in use at time t. */
    boolean redPointBlocked(double rx, double ry, double t) {
        for (Zone z : zones) if (z.active(t) && z.near(rx, ry, MARGIN_IN)) return true;
        return false;
    }

    /** The plan as a Blocker for this alliance at match time t (seconds since Auto started). */
    public Blocker at(final Alliance a, final double t) {
        return new Blocker() {
            @Override
            public boolean pointBlocked(double x, double y) {
                return redPointBlocked(a.xFromRed(x), a.yFromRed(y), t);
            }

            /**
             * Starting inside a blocked area is allowed (we're leaving it);
             * entering one, or re-entering after leaving, is not.
             */
            @Override
            public boolean segmentBlocked(double ax, double ay, double bx, double by) {
                double len = Math.hypot(bx - ax, by - ay);
                int steps = Math.max(1, (int) Math.ceil(len / 2.0));
                boolean leaving = pointBlocked(ax, ay);
                for (int i = 1; i <= steps; i++) {
                    double f = (double) i / steps;
                    boolean blocked = pointBlocked(ax + f * (bx - ax), ay + f * (by - ay));
                    if (leaving) {
                        if (!blocked) leaving = false;
                    } else if (blocked) {
                        return true;
                    }
                }
                return leaving;   // never got out: the end is blocked too
            }
        };
    }

    /** Blocked if blocked at ANY time in [t0, t1] (checked every 0.5 s): "will it be clear while I'm there?" */
    public Blocker between(final Alliance a, final double t0, final double t1) {
        return new Blocker() {
            @Override
            public boolean pointBlocked(double x, double y) {
                for (double t = t0; t <= t1 + 1e-9; t += 0.5) if (at(a, t).pointBlocked(x, y)) return true;
                return false;
            }

            @Override
            public boolean segmentBlocked(double ax, double ay, double bx, double by) {
                for (double t = t0; t <= t1 + 1e-9; t += 0.5) if (at(a, t).segmentBlocked(ax, ay, bx, by)) return true;
                return false;
            }
        };
    }

    /** True if any of [t0, t1) is blocked at this point (checked every 0.5 s). */
    public boolean blockedDuring(Alliance a, double x, double y, double t0, double t1) {
        for (double t = t0; t < t1; t += 0.5) if (at(a, t).pointBlocked(x, y)) return true;
        return false;
    }

    // ------------------------------------------------------------ text form

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("name=").append(name).append('\n');
        for (Zone z : zones) {
            sb.append(String.format(Locale.US, "zone=%s,%s,%s,%s,%s,%s%n",
                    num(z.x0), num(z.y0), num(z.x1), num(z.y1), num(z.fromS), num(z.toS)));
        }
        return sb.toString();
    }

    /** Reads toText()'s format; skips blank, comment (#) and malformed lines. */
    public static PartnerPlan fromText(String text) {
        PartnerPlan p = new PartnerPlan();
        if (text == null) return p;
        for (String raw : text.split("\r?\n")) {
            String line = raw.trim();
            if (line.startsWith("name=")) {
                p.setName(line.substring(5));
            } else if (line.startsWith("zone=")) {
                String[] f = line.substring(5).split(",");
                if (f.length != 6) continue;
                try {
                    double[] v = new double[6];
                    for (int i = 0; i < 6; i++) v[i] = Double.parseDouble(f[i].trim());
                    if (v[5] > v[4]) p.add(new Zone(v[0], v[1], v[2], v[3], v[4], v[5]));
                } catch (NumberFormatException ignored) {
                    // skip the line
                }
            }
        }
        return p;
    }

    private static String num(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    // ------------------------------------------------------------ Driver Hub view

    /**
     * Grid as the drive team sees it from its own wall: the nearest column
     * at the bottom, their left on the left (far wall for RED, audience for
     * BLUE). Whole-tile marks show their window; '+' = part of the tile is in
     * a zone drawn on the laptop.
     */
    public String[] render(int cursorCol, int cursorRow, Alliance a) {
        String[] lines = new String[COLS + 2];
        lines[0] = a == Alliance.RED ? "far wall <--          --> audience" : "audience <--          --> far wall";
        for (int c = COLS - 1, line = 1; c >= 0; c--, line++) {
            StringBuilder sb = new StringBuilder();
            sb.append(a == Alliance.RED ? (char) ('A' + c) : (char) ('F' - c)).append(' ');
            for (int r = ROWS - 1; r >= 0; r--) {
                boolean cur = c == cursorCol && r == cursorRow;
                sb.append(cur ? '[' : ' ').append(symbol(c, r)).append(cur ? ']' : ' ');
            }
            lines[line] = sb.toString();
        }
        lines[COLS + 1] = "      (our wall / drive team)";
        return lines;
    }

    private char symbol(int col, int row) {
        Window w = get(col, row);
        if (w != Window.FREE) return w.symbol;
        for (Zone z : zones) if (z.overlapsTile(col, row)) return '+';
        return '.';
    }
}
