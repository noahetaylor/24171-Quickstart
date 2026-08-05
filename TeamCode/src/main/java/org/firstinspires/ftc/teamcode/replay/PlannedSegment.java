package org.firstinspires.ftc.teamcode.replay;

/**
 * A planned segment between two waypoints, computed by PathSegmentPlanner.
 * Plain data — no Pedro dependency — so the planning logic that produces
 * these is fully unit-testable before ever touching a real PathChain.
 */
public class PlannedSegment {

    public final double fromX, fromY, fromHeadingRadians;
    public final double toX, toY, toHeadingRadians;

    /**
     * True if this segment represents an in-place rotation (position barely
     * changed, heading did) rather than a genuine drive. Still built as a
     * BezierLine downstream for now (Path 1's deliberately simple choice —
     * see conversation/commit history), but flagged here so we have the
     * information available for logging, debugging, or a future upgrade
     * to handle rotations as their own distinct segment type.
     */
    public final boolean isRotationInPlace;

    public PlannedSegment(double fromX, double fromY, double fromHeadingRadians,
                           double toX, double toY, double toHeadingRadians,
                           boolean isRotationInPlace) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromHeadingRadians = fromHeadingRadians;
        this.toX = toX;
        this.toY = toY;
        this.toHeadingRadians = toHeadingRadians;
        this.isRotationInPlace = isRotationInPlace;
    }

    @Override
    public String toString() {
        return String.format("PlannedSegment{(%.2f,%.2f)@%.2f -> (%.2f,%.2f)@%.2f, rotation=%b}",
                fromX, fromY, fromHeadingRadians, toX, toY, toHeadingRadians, isRotationInPlace);
    }
}
