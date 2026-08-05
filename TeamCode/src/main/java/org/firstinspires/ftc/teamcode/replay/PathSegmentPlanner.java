package org.firstinspires.ftc.teamcode.replay;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a simplified list of waypoints into a list of PlannedSegments,
 * one per consecutive pair. Plain Java, no Pedro dependency — this is the
 * layer where "is this actually a drive or an in-place rotation" gets
 * decided, kept separate from actually building a Pedro PathChain so the
 * classification logic can be verified with ordinary JUnit tests.
 */
public class PathSegmentPlanner {

    /**
     * Below this much positional movement (same units as x/y, e.g. inches),
     * a segment is treated as an in-place rotation rather than a drive.
     */
    public static final double DEFAULT_ROTATION_POSITION_THRESHOLD = 1.0;

    public static List<PlannedSegment> planSegments(List<PoseSample> waypoints) {
        return planSegments(waypoints, DEFAULT_ROTATION_POSITION_THRESHOLD);
    }

    public static List<PlannedSegment> planSegments(List<PoseSample> waypoints, double rotationPositionThreshold) {
        List<PlannedSegment> segments = new ArrayList<>();

        for (int i = 0; i < waypoints.size() - 1; i++) {
            PoseSample from = waypoints.get(i);
            PoseSample to = waypoints.get(i + 1);

            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            boolean isRotation = distance < rotationPositionThreshold;

            segments.add(new PlannedSegment(
                    from.x, from.y, from.headingRadians,
                    to.x, to.y, to.headingRadians,
                    isRotation
            ));
        }

        return segments;
    }
}
