package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;

/**
 * Where to point and how hard to shoot, from a robot pose (Pedro field
 * coordinates, inches / radians) to a target point (e.g. HiveState.target).
 *
 * Shooters are off the robot's center, so the robot heading that lines the
 * ball up with the target is not simply "face the target". The planner
 * finds it, then takes hood + rpm from the measured table if it covers the
 * distance, otherwise from drag-free physics.
 */
public final class ShotPlanner {
    private ShotPlanner() {}

    private static final int AIM_ITERATIONS = 8;

    public static ShotSolution plan(double robotX, double robotY, double robotHeadingRad,
                                    FieldPoint target, Shooter shooter, ShotTable table) {
        double yaw = Math.toRadians(ShooterTuning.LAUNCH_YAW_DEG);

        // Aim: the launch ray from the release point must pass through the
        // target. The release point moves as the robot turns, so iterate
        // (converges fast: the side offset is small next to the distance).
        double heading = Math.atan2(target.y - robotY, target.x - robotX) - yaw;
        double[] release = releasePoint(robotX, robotY, heading, shooter);
        for (int i = 0; i < AIM_ITERATIONS; i++) {
            heading = Math.atan2(target.y - release[1], target.x - release[0]) - yaw;
            release = releasePoint(robotX, robotY, heading, shooter);
        }
        heading = Field.normalizeRadians(heading);
        double turn = Field.normalizeRadians(heading - robotHeadingRad);

        double d = Math.hypot(target.x - release[0], target.y - release[1]);
        double h = target.z - shooter.releaseHeightIn;

        ShotTable.Row row = table == null ? null : table.lookup(d);
        if (row != null) {
            double a = Math.toRadians(row.hoodDeg);
            double v = shooter.speedForRpm(row.rpm);
            return new ShotSolution(heading, turn, d, h, row.hoodDeg, row.rpm,
                    ShotPhysics.timeOfFlight(d, v, a), ShotPhysics.descendingAt(d, v, a),
                    true, "", ShotSolution.Source.TABLE);
        }

        double angle = Shooter.hoodIsFixed()
                ? Math.toRadians(ShooterTuning.HOOD_MIN_DEG)
                : clamp(ShotPhysics.minSpeedAngle(d, h),
                        Math.toRadians(ShooterTuning.HOOD_MIN_DEG), Math.toRadians(ShooterTuning.HOOD_MAX_DEG));
        double hoodDeg = Math.toDegrees(angle);
        double v = ShotPhysics.speedFor(d, h, angle);
        if (Double.isNaN(v)) {
            return new ShotSolution(heading, turn, d, h, hoodDeg, 0, Double.NaN, false,
                    false, "too close for this hood angle", ShotSolution.Source.PHYSICS);
        }
        double rpm = shooter.rpmForSpeed(v);
        boolean ok = rpm <= ShooterTuning.MAX_RPM;
        return new ShotSolution(heading, turn, d, h, hoodDeg, rpm,
                ShotPhysics.timeOfFlight(d, v, angle), ShotPhysics.descendingAt(d, v, angle),
                ok, ok ? "" : "needs more than MAX_RPM", ShotSolution.Source.PHYSICS);
    }

    /** Release point in field coordinates for a robot at (x, y) facing heading. */
    static double[] releasePoint(double robotX, double robotY, double heading, Shooter s) {
        double c = Math.cos(heading), sn = Math.sin(heading);
        return new double[] {
                robotX + s.releaseForwardIn * c - s.releaseLeftIn * sn,
                robotY + s.releaseForwardIn * sn + s.releaseLeftIn * c
        };
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
