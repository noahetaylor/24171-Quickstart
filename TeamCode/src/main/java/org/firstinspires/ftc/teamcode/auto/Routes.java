package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.FieldRect;
import org.firstinspires.ftc.teamcode.shooting.ShotSpots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Straight-line driving that doesn't clip the HIVE frame: if the segment
 * from A to B passes through the frame's keep-out box, go via the box's
 * corner(s) instead (shortest option). Pure logic, unit-tested in RoutesTest.
 */
public final class Routes {
    private Routes() {}

    /** Frame base plus the robot's half-diagonal; corners pushed out 1 in more. */
    static final FieldRect KEEP_OUT = new FieldRect(
            Field.CENTER_IN - ShotSpots.FRAME_WIDTH_X_IN / 2 - ShotSpots.FRAME_MARGIN_IN,
            Field.CENTER_IN - ShotSpots.FRAME_DEPTH_Y_IN / 2 - ShotSpots.FRAME_MARGIN_IN,
            Field.CENTER_IN + ShotSpots.FRAME_WIDTH_X_IN / 2 + ShotSpots.FRAME_MARGIN_IN,
            Field.CENTER_IN + ShotSpots.FRAME_DEPTH_Y_IN / 2 + ShotSpots.FRAME_MARGIN_IN);
    private static final double CORNER_OUT = 1.0;

    /** Waypoints from a to b, including both ends. */
    public static List<FieldPoint> route(FieldPoint a, FieldPoint b) {
        if (!crosses(a, b)) return Arrays.asList(a, b);
        FieldPoint[] c = corners();
        List<FieldPoint> best = null;
        double bestLen = Double.MAX_VALUE;
        for (FieldPoint p : c) {                          // one corner
            if (crosses(a, p) || crosses(p, b)) continue;
            double len = dist(a, p) + dist(p, b);
            if (len < bestLen) { bestLen = len; best = Arrays.asList(a, p, b); }
        }
        if (best == null) {
            for (FieldPoint p : c) for (FieldPoint q : c) {   // two corners (around a side)
                if (p == q || crosses(a, p) || crosses(p, q) || crosses(q, b)) continue;
                double len = dist(a, p) + dist(p, q) + dist(q, b);
                if (len < bestLen) { bestLen = len; best = Arrays.asList(a, p, q, b); }
            }
        }
        if (best == null) {                               // a or b inside the box: just go straight
            List<FieldPoint> straight = new ArrayList<>();
            straight.add(a);
            straight.add(b);
            return straight;
        }
        return best;
    }

    static FieldPoint[] corners() {
        return new FieldPoint[]{
                new FieldPoint(KEEP_OUT.minX - CORNER_OUT, KEEP_OUT.minY - CORNER_OUT),
                new FieldPoint(KEEP_OUT.maxX + CORNER_OUT, KEEP_OUT.minY - CORNER_OUT),
                new FieldPoint(KEEP_OUT.maxX + CORNER_OUT, KEEP_OUT.maxY + CORNER_OUT),
                new FieldPoint(KEEP_OUT.minX - CORNER_OUT, KEEP_OUT.maxY + CORNER_OUT)};
    }

    /** True if segment a-b passes through the inside of the keep-out box (Liang-Barsky). */
    public static boolean crosses(FieldPoint a, FieldPoint b) {
        double dx = b.x - a.x, dy = b.y - a.y;
        double t0 = 0, t1 = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {a.x - KEEP_OUT.minX, KEEP_OUT.maxX - a.x, a.y - KEEP_OUT.minY, KEEP_OUT.maxY - a.y};
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i]) < 1e-12) {
                if (q[i] <= 0) return false;              // parallel and outside (or on the edge)
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) t0 = Math.max(t0, t); else t1 = Math.min(t1, t);
                if (t0 >= t1) return false;
            }
        }
        return t1 - t0 > 1e-9;
    }

    private static double dist(FieldPoint a, FieldPoint b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }
}
