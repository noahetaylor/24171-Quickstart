package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;

/**
 * Auto-aim: each loop, turns the robot toward the shot, sets the flywheel
 * target, and says when to pull the trigger.
 *
 * - Fires EARLY: the ball leaves FIRE_LATENCY_S after the trigger, so the
 *   check is made on where the robot will be pointing then, not now. A robot
 *   still swinging onto target can fire and the ball leaves as it settles.
 * - Leads moving shots: the ball keeps the robot's velocity, so it aims at a
 *   target shifted back along that velocity by the flight time.
 *
 * Inputs are Pedro field values: pose (in, rad), velocity (in/s, field
 * frame), turn rate (rad/s, CCW positive). turnPower is for Pedro's
 * setTeleOpDrive rotation input: positive turns CCW.
 *
 * Pure logic, no hardware: unit-tested in AutoAimTest.
 */
public final class AutoAim {
    private AutoAim() {}

    // --------------------------------------------------------- tunables
    public static double TURN_KP = 1.2;               // turn power per radian of error
    public static double TURN_KD = 0.08;              // turn power per rad/s (damping)
    public static double TURN_MAX = 0.8;
    public static double TURN_MIN = 0.06;             // enough to actually move the robot
    public static double HEADING_TOL_DEG = 2.0;       // allowed aim error at release
    public static double MAX_TURN_RATE_DEG_S = 30.0;  // must be nearly settled
    public static double RPM_TOL = 0.03;              // fraction of target rpm
    public static double MAX_DRIVE_SPEED_IN_S = 40.0; // faster than this: don't fire
    public static boolean LEAD_MOVING_SHOTS = true;

    public enum Status {
        /** No shot from here (too close / needs too much rpm). */
        NO_SHOT,
        /** Driving too fast to trust the shot. */
        MOVING_TOO_FAST,
        /** Heading not on target (or still turning fast). */
        TURNING,
        /** Aimed, waiting on the flywheel. */
        SPINNING_UP,
        /** Fire now. */
        READY
    }

    public static final class Command {
        public final Status status;
        public final boolean fire;
        public final double turnPower;
        public final double targetRpm;
        public final double hoodDeg;
        /** The shot as planned from the predicted release pose. */
        public final ShotSolution shot;

        Command(Status status, double turnPower, ShotSolution shot) {
            this.status = status;
            this.fire = status == Status.READY;
            this.turnPower = turnPower;
            this.targetRpm = shot.feasible ? shot.rpm : 0;
            this.hoodDeg = shot.hoodDeg;
            this.shot = shot;
        }
    }

    public static Command update(double x, double y, double heading,
                                 double vx, double vy, double omega,
                                 double flywheelRpm,
                                 FieldPoint target, Shooter shooter, ShotTable table) {
        // Where the robot will be when the ball actually leaves.
        double lat = Shooter.FIRE_LATENCY_S;
        double px = x + vx * lat;
        double py = y + vy * lat;
        double ph = heading + omega * lat;

        FieldPoint aimAt = target;
        ShotSolution shot = ShotPlanner.plan(px, py, ph, aimAt, shooter, table);
        if (LEAD_MOVING_SHOTS && shot.feasible) {
            for (int i = 0; i < 3; i++) {
                double t = shot.flightTimeS;
                aimAt = new FieldPoint(target.x - vx * t, target.y - vy * t, target.z);
                shot = ShotPlanner.plan(px, py, ph, aimAt, shooter, table);
                if (!shot.feasible) break;
            }
        }

        double errNow = Field.normalizeRadians(shot.aimHeadingRad - heading);
        double turn = TURN_KP * errNow - TURN_KD * omega;
        if (Math.abs(errNow) > Math.toRadians(HEADING_TOL_DEG) / 2 && Math.abs(turn) < TURN_MIN) {
            turn = Math.copySign(TURN_MIN, errNow);
        }
        turn = Math.max(-TURN_MAX, Math.min(TURN_MAX, turn));

        Status status;
        if (!shot.feasible) {
            status = Status.NO_SHOT;
            turn = 0;
        } else if (Math.hypot(vx, vy) > MAX_DRIVE_SPEED_IN_S) {
            status = Status.MOVING_TOO_FAST;
        } else if (Math.abs(shot.turnRad) > Math.toRadians(HEADING_TOL_DEG)
                || Math.abs(omega) > Math.toRadians(MAX_TURN_RATE_DEG_S)) {
            status = Status.TURNING;
        } else if (Math.abs(flywheelRpm - shot.rpm) > RPM_TOL * shot.rpm) {
            status = Status.SPINNING_UP;
        } else {
            status = Status.READY;
        }
        return new Command(status, turn, shot);
    }
}
