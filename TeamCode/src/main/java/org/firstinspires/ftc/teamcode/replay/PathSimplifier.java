package org.firstinspires.ftc.teamcode.replay;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces a dense list of recorded PoseSamples down to the "corner" points
 * that describe the same path shape, using a heading-aware variant of the
 * Ramer-Douglas-Peucker algorithm.
 *
 * A point survives simplification if it deviates significantly from what
 * you'd expect by straight-line interpolating between its neighbors, in
 * EITHER position or heading. Position-only RDP would silently collapse a
 * pure in-place rotation (heading changes, position barely does) down to
 * nothing, since there'd be almost no positional deviation to detect. The
 * heading check exists specifically to catch that case (e.g. a driver who
 * drives, rotates in place to aim, then drives again).
 *
 * This is still the "Option A" simplification step: connecting the
 * resulting waypoints with straight lines (done in a later step) loses
 * curvature during actual driving segments. A future upgrade can feed this
 * same simplified point list into a smooth curve-fit instead.
 */
public class PathSimplifier {

    /**
     * @param points               dense recorded points, in order
     * @param epsilonPosition      max allowed perpendicular position deviation
     *                             (same units as x/y, e.g. inches) before a
     *                             point is considered significant enough to keep
     * @param epsilonHeadingRadians max allowed heading deviation (radians) from
     *                             the interpolated expectation before a point
     *                             is considered significant enough to keep
     */
    public static List<PoseSample> simplify(List<PoseSample> points, double epsilonPosition, double epsilonHeadingRadians) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;

        simplifyRecursive(points, 0, points.size() - 1, epsilonPosition, epsilonHeadingRadians, keep);

        List<PoseSample> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    private static void simplifyRecursive(List<PoseSample> points, int startIndex, int endIndex,
                                           double epsilonPosition, double epsilonHeadingRadians, boolean[] keep) {
        if (endIndex <= startIndex + 1) return; // no points strictly between them

        PoseSample start = points.get(startIndex);
        PoseSample end = points.get(endIndex);
        long timeSpan = end.timestampMs - start.timestampMs;
        double headingSpan = angleDelta(start.headingRadians, end.headingRadians);

        // Each candidate point gets scored against BOTH epsilons, normalized
        // so a value of 1.0 means "exactly at the tolerance limit." Taking
        // the max of the two scores means a point that's significant in
        // either dimension gets picked, not just position.
        double maxScore = -1;
        int maxIndex = -1;

        for (int i = startIndex + 1; i < endIndex; i++) {
            PoseSample p = points.get(i);

            double positionDeviation = perpendicularDistance(p, start, end);

            double frac = (timeSpan == 0) ? 0.0 : (p.timestampMs - start.timestampMs) / (double) timeSpan;
            double expectedHeading = start.headingRadians + frac * headingSpan;
            double headingDeviation = Math.abs(angleDelta(expectedHeading, p.headingRadians));

            double positionScore = epsilonPosition > 0 ? positionDeviation / epsilonPosition : 0;
            double headingScore = epsilonHeadingRadians > 0 ? headingDeviation / epsilonHeadingRadians : 0;
            double score = Math.max(positionScore, headingScore);

            if (score > maxScore) {
                maxScore = score;
                maxIndex = i;
            }
        }

        if (maxScore > 1.0) {
            keep[maxIndex] = true;
            simplifyRecursive(points, startIndex, maxIndex, epsilonPosition, epsilonHeadingRadians, keep);
            simplifyRecursive(points, maxIndex, endIndex, epsilonPosition, epsilonHeadingRadians, keep);
        }
        // else: every point between start and end is within tolerance of
        // both the straight line AND the interpolated heading, so none of
        // them get kept.
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

    /** Shortest signed angular difference from `from` to `to`, in radians, in range (-PI, PI]. */
    private static double angleDelta(double from, double to) {
        double diff = to - from;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return diff;
    }
}
