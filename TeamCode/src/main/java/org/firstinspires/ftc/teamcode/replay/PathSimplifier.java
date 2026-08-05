package org.firstinspires.ftc.teamcode.replay;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces a dense list of recorded PoseSamples down to the "corner" points
 * that describe the same path shape, using the Ramer-Douglas-Peucker
 * algorithm. Operates purely on (x, y) geometry — heading is preserved on
 * whichever PoseSamples survive simplification, but doesn't factor into
 * which points get kept or dropped.
 *
 * This is intentionally the "Option A" simplification step: it produces
 * fewer waypoints, but connecting them with straight lines (done in a
 * later step) loses curvature. A future upgrade can feed this same
 * simplified point list into a smooth curve-fit instead.
 */
public class PathSimplifier {

    /**
     * @param points  dense recorded points, in order
     * @param epsilon max allowed perpendicular deviation (same units as
     *                x/y, e.g. inches) before a point is considered
     *                significant enough to keep. Larger epsilon = fewer
     *                resulting waypoints = more aggressive simplification.
     */
    public static List<PoseSample> simplify(List<PoseSample> points, double epsilon) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;

        simplifyRecursive(points, 0, points.size() - 1, epsilon, keep);

        List<PoseSample> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    private static void simplifyRecursive(List<PoseSample> points, int startIndex, int endIndex,
                                           double epsilon, boolean[] keep) {
        if (endIndex <= startIndex + 1) return; // no points strictly between them

        PoseSample start = points.get(startIndex);
        PoseSample end = points.get(endIndex);

        double maxDistance = -1;
        int maxIndex = -1;

        for (int i = startIndex + 1; i < endIndex; i++) {
            double distance = perpendicularDistance(points.get(i), start, end);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }

        if (maxDistance > epsilon) {
            keep[maxIndex] = true;
            simplifyRecursive(points, startIndex, maxIndex, epsilon, keep);
            simplifyRecursive(points, maxIndex, endIndex, epsilon, keep);
        }
        // else: every point between start and end is within epsilon of the
        // straight line between them, so none of them get kept.
    }

    /** Perpendicular distance from `point` to the infinite line through lineStart/lineEnd. */
    private static double perpendicularDistance(PoseSample point, PoseSample lineStart, PoseSample lineEnd) {
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;

        if (dx == 0 && dy == 0) {
            // Degenerate case: start and end are the same point.
            double ddx = point.x - lineStart.x;
            double ddy = point.y - lineStart.y;
            return Math.sqrt(ddx * ddx + ddy * ddy);
        }

        double numerator = Math.abs(dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x);
        double denominator = Math.sqrt(dx * dx + dy * dy);
        return numerator / denominator;
    }
}
