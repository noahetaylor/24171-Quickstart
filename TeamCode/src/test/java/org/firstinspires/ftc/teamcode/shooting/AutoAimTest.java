package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.tuning.AimTuning;
import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AutoAimTest {

    private static final FieldPoint TARGET = new FieldPoint(72, 120, 53.5);
    private static final double X = 72, Y = 40;

    private double latency;
    private boolean lead;

    @Before
    public void save() {
        latency = ShooterTuning.FIRE_LATENCY_S;
        lead = AimTuning.LEAD_MOVING_SHOTS;
    }

    @After
    public void restore() {
        ShooterTuning.FIRE_LATENCY_S = latency;
        AimTuning.LEAD_MOVING_SHOTS = lead;
    }

    /** The aim for a still robot at (X, Y); its rpms are "at speed". */
    private static AutoAim.Command ideal() {
        return AutoAim.update(X, Y, 0, 0, 0, 0, 0, 0, TARGET, null, null);
    }

    private static AutoAim.Command at(double heading, double omega, double pollenRpm, double nectarRpm) {
        return AutoAim.update(X, Y, heading, 0, 0, omega, pollenRpm, nectarRpm, TARGET, null, null);
    }

    private static AutoAim.Command atSpeed(double heading, double omega) {
        AutoAim.Command i = ideal();
        return at(heading, omega, i.pollen.rpm, i.nectar.rpm);
    }

    @Test
    public void stillAimedAndUpToSpeedFires() {
        AutoAim.Command c = atSpeed(ideal().aimHeadingRad, 0);
        assertSame(AutoAim.Status.READY, c.status);
        assertTrue(c.fire);
        assertEquals(0, c.turnPower, 0); // inside the dead-band: no twitching
    }

    @Test
    public void aimingBetweenTheShootersPutsBothBallsInTheOpening() {
        AutoAim.Command c = ideal();
        double h = c.aimHeadingRad;
        // Parallel shooters 7.6 in apart: each ball ~3.8-4.2 in off center
        // (a bit more than half the spacing: the release points are behind
        // the center the robot turns about).
        assertEquals(4.0, AutoAim.miss(c.pollen, h), 0.5);
        assertEquals(4.0, AutoAim.miss(c.nectar, h), 0.5);
        assertTrue(AutoAim.miss(c.pollen, h) < AimTuning.MAX_MISS_IN);
    }

    @Test
    public void offTargetTurnsTheRightWay() {
        double aim = ideal().aimHeadingRad;
        AutoAim.Command ccw = atSpeed(aim - Math.toRadians(20), 0);
        assertSame(AutoAim.Status.TURNING, ccw.status);
        assertTrue("needs CCW", ccw.turnPower > 0);
        AutoAim.Command cw = atSpeed(aim + Math.toRadians(20), 0);
        assertTrue("needs CW", cw.turnPower < 0);
        assertFalse(cw.fire);
    }

    @Test
    public void turnPowerIsClampedAndHasAFloor() {
        double aim = ideal().aimHeadingRad;
        assertEquals(AimTuning.TURN_MAX, atSpeed(aim - Math.PI / 2, 0).turnPower, 1e-9);
        // 2 deg at ~80 in is ~2.8 in: outside the dead-band, so at least TURN_MIN.
        double small = atSpeed(aim - Math.toRadians(2), 0).turnPower;
        assertTrue(small >= AimTuning.TURN_MIN);
    }

    @Test
    public void waitsForEitherFlywheel() {
        AutoAim.Command i = ideal();
        assertSame(AutoAim.Status.SPINNING_UP, at(i.aimHeadingRad, 0, i.pollen.rpm * 0.9, i.nectar.rpm).status);
        assertSame(AutoAim.Status.SPINNING_UP, at(i.aimHeadingRad, 0, i.pollen.rpm, i.nectar.rpm * 0.9).status);
    }

    @Test
    public void firesEarlyWhenTheSwingWillLandOnTarget() {
        ShooterTuning.FIRE_LATENCY_S = 0.15;
        double aim = ideal().aimHeadingRad;
        double omega = Math.toRadians(20);
        // 3 deg short (~4.2 in more miss: too much) but turning 20 deg/s toward
        // it: in 0.15 s it will be on target.
        assertSame(AutoAim.Status.TURNING, atSpeed(aim - Math.toRadians(3), 0).status);
        assertSame(AutoAim.Status.READY, atSpeed(aim - Math.toRadians(3), omega).status);
        // Same error, turning AWAY: no.
        assertSame(AutoAim.Status.TURNING, atSpeed(aim - Math.toRadians(3), -omega).status);
    }

    @Test
    public void doesNotFireWhileSpinningFastThroughTheTarget() {
        double omega = Math.toRadians(90);
        double heading = ideal().aimHeadingRad - omega * ShooterTuning.FIRE_LATENCY_S; // on target at release
        assertSame(AutoAim.Status.TURNING, atSpeed(heading, omega).status);
    }

    @Test
    public void noShotMeansNoTurnAndNoFire() {
        AutoAim.Command c = AutoAim.update(TARGET.x, TARGET.y - 2, Math.PI / 2, 0, 0, 0, 3000, 3000,
                TARGET, null, null);
        assertSame(AutoAim.Status.NO_SHOT, c.status);
        assertEquals(0, c.turnPower, 0);
        assertEquals(0, c.rpmFor(Shooter.POLLEN), 0);
    }

    @Test
    public void tooFastToShoot() {
        AutoAim.Command i = ideal();
        AutoAim.Command c = AutoAim.update(X, Y, i.aimHeadingRad, 60, 0, 0, i.pollen.rpm, i.nectar.rpm,
                TARGET, null, null);
        assertSame(AutoAim.Status.MOVING_TOO_FAST, c.status);
    }

    /** Fly one shooter's ball from the command's aim; return its sideways miss at the target. */
    private static double flownMiss(AutoAim.Command c, Shooter s, ShotSolution sol, double vx) {
        double lat = ShooterTuning.FIRE_LATENCY_S;
        double h = c.aimHeadingRad;
        double[] p = ShotPlanner.releasePoint(X + vx * lat, Y, h, s);
        double a = Math.toRadians(sol.hoodDeg);
        double v = s.speedForRpm(sol.rpm);
        // Time for the ball to reach the target's Y (the target is straight ahead).
        double vyBall = v * Math.cos(a) * Math.sin(h);
        double t = (TARGET.y - p[1]) / vyBall;
        double bx = p[0] + (v * Math.cos(a) * Math.cos(h) + vx) * t;
        return Math.abs(bx - TARGET.x);
    }

    @Test
    public void movingShotIsLeadSoTheBallsStillArrive() {
        double vx = 30; // sliding toward the blue wall
        AimTuning.LEAD_MOVING_SHOTS = true;
        AutoAim.Command led = AutoAim.update(X, Y, Math.PI / 2, vx, 0, 0, 3000, 3000, TARGET, null, null);
        AimTuning.LEAD_MOVING_SHOTS = false;
        AutoAim.Command plain = AutoAim.update(X, Y, Math.PI / 2, vx, 0, 0, 3000, 3000, TARGET, null, null);

        // Leading aims back against the slide: further CCW (left).
        assertTrue(led.aimHeadingRad > plain.aimHeadingRad);
        // Led balls land in the opening; unled ones drift off with the robot.
        for (Shooter s : Shooter.values()) {
            ShotSolution sol = s == Shooter.POLLEN ? led.pollen : led.nectar;
            assertTrue(s + " led", flownMiss(led, s, sol, vx) < AimTuning.MAX_MISS_IN);
        }
        double ledWorst = Math.max(flownMiss(led, Shooter.POLLEN, led.pollen, vx),
                flownMiss(led, Shooter.NECTAR, led.nectar, vx));
        double plainWorst = Math.max(flownMiss(plain, Shooter.POLLEN, plain.pollen, vx),
                flownMiss(plain, Shooter.NECTAR, plain.nectar, vx));
        assertTrue(ledWorst < plainWorst);
    }
}
