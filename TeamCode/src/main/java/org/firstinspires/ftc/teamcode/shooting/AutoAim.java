package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.tuning.AimTuning;
import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;

/**
 * Auto-aim: each loop, turns the robot toward the shot, sets the flywheel
 * targets, and says when to feed.
 *
 * - Both shooters at once: the transfer feeds both lanes, so the robot aims
 *   between the two shooters' aim headings. Shooters are parallel and only
 *   7.6 in apart, so each ball lands ~3.8 in either side of the CELL
 *   center; firing waits until each will pass within MAX_MISS_IN of it.
 * - Fires EARLY: the ball leaves FIRE_LATENCY_S after the trigger, so the
 *   check is made on where the robot will be pointing then, not now.
 * - Leads moving shots: the ball keeps the robot's velocity, so it aims at a
 *   target shifted back along that velocity by the flight time.
 *
 * Inputs are Pedro field values: pose (in, rad), velocity (in/s, field
 * frame), turn rate (rad/s, CCW positive). turnPower is for Pedro's
 * setTeleOpDrive rotation input: positive turns CCW.
 *
 * Gains and limits: tuning/AimTuning. Pure logic, no hardware:
 * unit-tested in AutoAimTest.
 */
public final class AutoAim {
    private AutoAim() {}

    /** In order of priority: the first one that applies is reported. */
    public enum Status {
        /** No shot from here (too close / needs too much rpm). */
        NO_SHOT,
        /** Driving too fast to trust the shot. */
        MOVING_TOO_FAST,
        /** Heading not on target (or still turning fast). */
        TURNING,
        /** Aimed, waiting on a flywheel. */
        SPINNING_UP,
        /** Feed now. */
        READY
    }

    public static final class Command {
        public final Status status;
        public final boolean fire;
        public final double turnPower;
        /** Heading the robot is being turned to. */
        public final double aimHeadingRad;
        public final ShotSolution pollen;
        public final ShotSolution nectar;

        Command(Status status, double turnPower, double aimHeadingRad,
                ShotSolution pollen, ShotSolution nectar) {
            this.status = status;
            this.fire = status == Status.READY;
            this.turnPower = turnPower;
            this.aimHeadingRad = aimHeadingRad;
            this.pollen = pollen;
            this.nectar = nectar;
        }

        public double rpmFor(Shooter s) {
            ShotSolution sol = s == Shooter.POLLEN ? pollen : nectar;
            return sol.feasible ? sol.rpm : 0;
        }
    }

    /**
     * @param pollenRpm / nectarRpm  measured flywheel speeds
     */
    public static Command update(double x, double y, double heading,
                                 double vx, double vy, double omega,
                                 double pollenRpm, double nectarRpm,
                                 FieldPoint target, ShotTable pollenTable, ShotTable nectarTable) {
        // Where the robot will be when the balls actually leave.
        double lat = ShooterTuning.FIRE_LATENCY_S;
        double px = x + vx * lat;
        double py = y + vy * lat;
        double ph = heading + omega * lat;

        ShotSolution pollen = solve(px, py, ph, vx, vy, target, Shooter.POLLEN, pollenTable);
        ShotSolution nectar = solve(px, py, ph, vx, vy, target, Shooter.NECTAR, nectarTable);

        // Aim between the two (circular mean).
        double aim = Math.atan2(Math.sin(pollen.aimHeadingRad) + Math.sin(nectar.aimHeadingRad),
                Math.cos(pollen.aimHeadingRad) + Math.cos(nectar.aimHeadingRad));

        // Sideways miss of each ball if released at the predicted heading.
        double worstMiss = Math.max(miss(pollen, ph), miss(nectar, ph));

        double errNow = Field.normalizeRadians(aim - heading);
        double turn = AimTuning.TURN_KP * errNow - AimTuning.TURN_KD * omega;
        double meanDistance = (pollen.distanceIn + nectar.distanceIn) / 2;
        double aimErrorIn = Math.abs(meanDistance * Math.sin(errNow));
        if (aimErrorIn < AimTuning.AIM_DEADBAND_IN && Math.abs(omega) < Math.toRadians(5)) {
            turn = 0;
        } else if (Math.abs(turn) < AimTuning.TURN_MIN) {
            turn = Math.copySign(AimTuning.TURN_MIN, errNow);
        }
        turn = Math.max(-AimTuning.TURN_MAX, Math.min(AimTuning.TURN_MAX, turn));

        Status status;
        if (!pollen.feasible || !nectar.feasible) {
            status = Status.NO_SHOT;
            turn = 0;
        } else if (Math.hypot(vx, vy) > AimTuning.MAX_DRIVE_SPEED_IN_S) {
            status = Status.MOVING_TOO_FAST;
        } else if (worstMiss > AimTuning.MAX_MISS_IN
                || Math.abs(omega) > Math.toRadians(AimTuning.MAX_TURN_RATE_DEG_S)) {
            status = Status.TURNING;
        } else if (!atSpeed(pollenRpm, pollen.rpm) || !atSpeed(nectarRpm, nectar.rpm)) {
            status = Status.SPINNING_UP;
        } else {
            status = Status.READY;
        }
        return new Command(status, turn, Field.normalizeRadians(aim), pollen, nectar);
    }

    /** One shooter's shot from the predicted release pose, led for robot motion. */
    static ShotSolution solve(double px, double py, double ph, double vx, double vy,
                              FieldPoint target, Shooter shooter, ShotTable table) {
        ShotSolution shot = ShotPlanner.plan(px, py, ph, target, shooter, table);
        if (AimTuning.LEAD_MOVING_SHOTS && shot.feasible) {
            for (int i = 0; i < 3; i++) {
                double t = shot.flightTimeS;
                FieldPoint led = new FieldPoint(target.x - vx * t, target.y - vy * t, target.z);
                ShotSolution next = ShotPlanner.plan(px, py, ph, led, shooter, table);
                if (!next.feasible) break;
                shot = next;
            }
        }
        return shot;
    }

    /** Sideways distance the ball misses by if launched at heading ph. */
    static double miss(ShotSolution s, double ph) {
        return Math.abs(s.distanceIn * Math.sin(Field.normalizeRadians(s.aimHeadingRad - ph)));
    }

    private static boolean atSpeed(double measured, double target) {
        return Math.abs(measured - target) <= AimTuning.RPM_TOL * target;
    }
}
