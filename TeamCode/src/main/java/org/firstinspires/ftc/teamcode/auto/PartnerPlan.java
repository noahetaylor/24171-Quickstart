package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Blocker;
import org.firstinspires.ftc.teamcode.field.Field;

/**
 * Pre-match partner plan: which tiles on our half our partner's Auto uses,
 * and when. Set on the Driver Hub in Auto's INIT (R704: no laptops at
 * matches). Auto won't chase balls, pick shot spots, or drive through those
 * tiles while they're blocked.
 *
 * Tiles are stored for RED (columns A..C = x 0..72, rows 1..6 = y 0..144);
 * BLUE's tiles are the same plan rotated, so a plan means the same thing
 * from either alliance's wall. Pure logic, unit-tested in PartnerPlanTest.
 */
public final class PartnerPlan {

    /** When a tile is in use by the partner. */
    public enum Window {
        FREE('.', 0, 0), ALL('#', 0, 30), EARLY('1', 0, 10), MID('2', 10, 20), LATE('3', 20, 30);

        public final char symbol;
        final double from, to;

        Window(char symbol, double from, double to) {
            this.symbol = symbol;
            this.from = from;
            this.to = to;
        }

        boolean covers(double t) {
            return this != FREE && t >= from - 1e-9 && t < to;
        }

        Window next() {
            return values()[(ordinal() + 1) % values().length];
        }

        static Window of(char c) {
            for (Window w : values()) if (w.symbol == c) return w;
            return FREE;
        }
    }

    public static final int COLS = 3, ROWS = 6;
    /** Keep the robot center this far out of a blocked tile (robot half-width, roughly). */
    public static double MARGIN_IN = 7.0;

    private final Window[][] tiles = new Window[COLS][ROWS];

    public PartnerPlan() {
        clear();
    }

    public void clear() {
        for (Window[] col : tiles) java.util.Arrays.fill(col, Window.FREE);
    }

    public Window get(int col, int row) {
        return tiles[col][row];
    }

    public void set(int col, int row, Window w) {
        tiles[col][row] = w;
    }

    public void cycle(int col, int row) {
        tiles[col][row] = tiles[col][row].next();
    }

    public int blockedCount() {
        int n = 0;
        for (Window[] col : tiles) for (Window w : col) if (w != Window.FREE) n++;
        return n;
    }

    /** "A1"-style name of a tile (red frame; BLUE uses the mirrored letters D..F). */
    public static String tileName(int col, int row, Alliance a) {
        char letter = a == Alliance.RED ? (char) ('A' + col) : (char) ('F' - col);
        int number = a == Alliance.RED ? row + 1 : ROWS - row;
        return "" + letter + number;
    }

    /** True if the RED-frame point is in or within MARGIN_IN of a tile blocked at time t. */
    boolean redPointBlocked(double rx, double ry, double t) {
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (!tiles[c][r].covers(t)) continue;
                double x0 = c * Field.TILE_IN - MARGIN_IN, x1 = (c + 1) * Field.TILE_IN + MARGIN_IN;
                double y0 = r * Field.TILE_IN - MARGIN_IN, y1 = (r + 1) * Field.TILE_IN + MARGIN_IN;
                if (rx > x0 && rx < x1 && ry > y0 && ry < y1) return true;
            }
        }
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

    /** 18 symbols, column A rows 1..6, then B, then C. */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Window[] col : tiles) for (Window w : col) sb.append(w.symbol);
        return sb.toString();
    }

    public static PartnerPlan decode(String s) {
        PartnerPlan p = new PartnerPlan();
        if (s == null) return p;
        for (int i = 0; i < Math.min(s.length(), COLS * ROWS); i++) {
            p.tiles[i / ROWS][i % ROWS] = Window.of(s.charAt(i));
        }
        return p;
    }

    /**
     * Grid as the drive team sees it from its own wall: the nearest column
     * at the bottom, their left on the left (far wall for RED, audience for BLUE).
     */
    public String[] render(int cursorCol, int cursorRow, Alliance a) {
        String[] lines = new String[COLS + 2];
        lines[0] = a == Alliance.RED ? "far wall <--          --> audience" : "audience <--          --> far wall";
        for (int c = COLS - 1, line = 1; c >= 0; c--, line++) {
            StringBuilder sb = new StringBuilder();
            sb.append(a == Alliance.RED ? (char) ('A' + c) : (char) ('F' - c)).append(' ');
            for (int r = ROWS - 1; r >= 0; r--) {
                boolean cur = c == cursorCol && r == cursorRow;
                sb.append(cur ? '[' : ' ').append(tiles[c][r].symbol).append(cur ? ']' : ' ');
            }
            lines[line] = sb.toString();
        }
        lines[COLS + 1] = "      (our wall / drive team)";
        return lines;
    }
}
