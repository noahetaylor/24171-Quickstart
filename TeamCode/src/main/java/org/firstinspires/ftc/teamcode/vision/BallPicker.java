package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Blocker;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.shooting.ShotSpots;
import org.firstinspires.ftc.teamcode.tuning.VisionTuning;

import java.util.List;

/**
 * Chooses which ball to go get: one we're allowed and want (POLLEN, or
 * NECTAR of our color), on our half of the field in Auto (crossing can be
 * a major foul), reachable (not against a wall, not under the HIVE frame),
 * not in a blocked area (partner plan), and cheapest to reach counting
 * the turn.
 *
 * Pure logic, unit-tested in BallPickerTest.
 */
public final class BallPicker {
    private BallPicker() {}

    /** Robot center can't get closer to a wall than this, so balls tighter than ~this are skipped. */
    public static final double WALL_REACH_IN = 3.0;

    public static boolean onOurSide(Alliance a, double x) {
        double redX = a.xFromRed(x);   // same transform both ways
        return redX <= Field.CENTER_IN - VisionTuning.OWN_SIDE_MARGIN_IN;
    }

    public static boolean reachable(double x, double y) {
        if (x < WALL_REACH_IN || y < WALL_REACH_IN
                || x > Field.SIZE_IN - WALL_REACH_IN || y > Field.SIZE_IN - WALL_REACH_IN) return false;
        // Under the HIVE frame (its base, no robot margin: the intake reaches in a little).
        return !(Math.abs(x - Field.CENTER_IN) < ShotSpots.FRAME_WIDTH_X_IN / 2
                && Math.abs(y - Field.CENTER_IN) < ShotSpots.FRAME_DEPTH_Y_IN / 2);
    }

    /**
     * @param ownSideOnly true in Auto
     * @return the best ball, or null if none qualifies
     */
    public static BallSighting pick(List<BallSighting> balls, Alliance a, boolean ownSideOnly,
                                    double robotX, double robotY, double robotHeadingRad) {
        return pick(balls, a, ownSideOnly, robotX, robotY, robotHeadingRad, Blocker.NONE);
    }

    /** Same, also skipping balls in (or behind) blocked areas, e.g. our partner's Auto tiles. */
    public static BallSighting pick(List<BallSighting> balls, Alliance a, boolean ownSideOnly,
                                    double robotX, double robotY, double robotHeadingRad, Blocker blocker) {
        BallSighting best = null;
        double bestCost = Double.MAX_VALUE;
        for (BallSighting b : balls) {
            if (!b.kind.wantedBy(a)) continue;
            if (ownSideOnly && !onOurSide(a, b.x)) continue;
            if (!reachable(b.x, b.y)) continue;
            if (blocker.pointBlocked(b.x, b.y) || blocker.segmentBlocked(robotX, robotY, b.x, b.y)) continue;
            double dist = Math.hypot(b.x - robotX, b.y - robotY);
            double turn = Math.abs(Field.normalizeRadians(Math.atan2(b.y - robotY, b.x - robotX) - robotHeadingRad));
            double cost = dist + VisionTuning.TURN_COST_IN_PER_90 * turn / (Math.PI / 2);
            if (cost < bestCost) {
                bestCost = cost;
                best = b;
            }
        }
        return best;
    }
}
