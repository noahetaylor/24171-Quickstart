package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.FieldRect;
import org.firstinspires.ftc.teamcode.tuning.AimTuning;

/**
 * Pre-align: where the robot should drive to take a shot. The nearest point
 * to the robot on a ring of PREALIGN_DISTANCE_IN around the target, kept
 * inside the field walls and clear of the HIVE frame.
 *
 * Pure logic, unit-tested in ShotSpotsTest.
 */
public final class ShotSpots {
    private ShotSpots() {}

    /** Robot is about 18 in square; keep its center this far from walls. */
    public static final double WALL_MARGIN_IN = 10.0;
    /** HIVE frame base, centered on the field (manual 9.6.1). */
    public static final double FRAME_WIDTH_X_IN = 49.46;
    public static final double FRAME_DEPTH_Y_IN = 38.95;
    /** Keep the robot center this far outside the frame base (half-diagonal of 18 in + margin). */
    public static final double FRAME_MARGIN_IN = 14.0;

    private static final FieldRect FRAME_KEEP_OUT = new FieldRect(
            Field.CENTER_IN - FRAME_WIDTH_X_IN / 2 - FRAME_MARGIN_IN,
            Field.CENTER_IN - FRAME_DEPTH_Y_IN / 2 - FRAME_MARGIN_IN,
            Field.CENTER_IN + FRAME_WIDTH_X_IN / 2 + FRAME_MARGIN_IN,
            Field.CENTER_IN + FRAME_DEPTH_Y_IN / 2 + FRAME_MARGIN_IN);

    public static boolean allowed(double x, double y) {
        double lo = WALL_MARGIN_IN, hi = Field.SIZE_IN - WALL_MARGIN_IN;
        return x >= lo && x <= hi && y >= lo && y <= hi && !FRAME_KEEP_OUT.contains(x, y);
    }

    /**
     * Robot-center spot to shoot from, or null if no allowed spot exists on
     * the ring. Ring radius is to the robot CENTER.
     */
    public static FieldPoint nearest(double robotX, double robotY, FieldPoint target, double radiusIn) {
        double start = Math.atan2(robotY - target.y, robotX - target.x);
        // Walk outward from the robot's own bearing, alternating sides, 2 deg steps.
        for (int k = 0; k <= 90; k++) {
            for (int side : new int[]{1, -1}) {
                if (k == 0 && side == -1) continue;
                double a = start + side * Math.toRadians(2.0 * k);
                double x = target.x + radiusIn * Math.cos(a);
                double y = target.y + radiusIn * Math.sin(a);
                if (allowed(x, y)) return new FieldPoint(x, y);
            }
        }
        return null;
    }

    /** nearest() at the tuned pre-align distance. */
    public static FieldPoint nearest(double robotX, double robotY, FieldPoint target) {
        return nearest(robotX, robotY, target, AimTuning.PREALIGN_DISTANCE_IN);
    }
}
