package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.FieldPoint;
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
        latency = Shooter.FIRE_LATENCY_S;
        lead = AutoAim.LEAD_MOVING_SHOTS;
    }

    @After
    public void restore() {
        Shooter.FIRE_LATENCY_S = latency;
        AutoAim.LEAD_MOVING_SHOTS = lead;
    }

    /** The aim heading and rpm for a still robot at (X, Y). */
    private static ShotSolution still() {
        return ShotPlanner.plan(X, Y, 0, TARGET, Shooter.POLLEN, null);
    }

    private static AutoAim.Command at(double heading, double omega, double rpm) {
        return AutoAim.update(X, Y, heading, 0, 0, omega, rpm, TARGET, Shooter.POLLEN, null);
    }

    @Test
    public void stillAimedAndUpToSpeedFires() {
        ShotSolution s = still();
        AutoAim.Command c = at(s.aimHeadingRad, 0, s.rpm);
        assertSame(AutoAim.Status.READY, c.status);
        assertTrue(c.fire);
        assertEquals(s.rpm, c.targetRpm, 1e-6);
    }

    @Test
    public void offTargetTurnsTheRightWay() {
        ShotSolution s = still();
        AutoAim.Command ccw = at(s.aimHeadingRad - Math.toRadians(20), 0, s.rpm);
        assertSame(AutoAim.Status.TURNING, ccw.status);
        assertTrue("needs CCW", ccw.turnPower > 0);
        AutoAim.Command cw = at(s.aimHeadingRad + Math.toRadians(20), 0, s.rpm);
        assertTrue("needs CW", cw.turnPower < 0);
        assertFalse(cw.fire);
    }

    @Test
    public void turnPowerIsClampedAndHasAFloor() {
        ShotSolution s = still();
        assertEquals(AutoAim.TURN_MAX, at(s.aimHeadingRad - Math.PI / 2, 0, s.rpm).turnPower, 1e-9);
        double small = at(s.aimHeadingRad - Math.toRadians(1.5), 0, s.rpm).turnPower;
        assertTrue(small >= AutoAim.TURN_MIN);
    }

    @Test
    public void waitsForTheFlywheel() {
        ShotSolution s = still();
        AutoAim.Command c = at(s.aimHeadingRad, 0, s.rpm * 0.9);
        assertSame(AutoAim.Status.SPINNING_UP, c.status);
        assertFalse(c.fire);
    }

    @Test
    public void firesEarlyWhenTheSwingWillLandOnTarget() {
        Shooter.FIRE_LATENCY_S = 0.15;
        ShotSolution s = still();
        // 3 deg short (outside the 2 deg tolerance) but turning 20 deg/s toward it:
        // in 0.15 s it will be on target.
        double omega = Math.toRadians(20);
        AutoAim.Command c = at(s.aimHeadingRad - Math.toRadians(3), omega, s.rpm);
        assertSame(AutoAim.Status.READY, c.status);
        // Same error, turning AWAY: no.
        AutoAim.Command away = at(s.aimHeadingRad - Math.toRadians(3), -omega, s.rpm);
        assertSame(AutoAim.Status.TURNING, away.status);
    }

    @Test
    public void doesNotFireWhileSpinningFastThroughTheTarget() {
        ShotSolution s = still();
        double omega = Math.toRadians(90);
        double heading = s.aimHeadingRad - omega * Shooter.FIRE_LATENCY_S;  // on target at release
        AutoAim.Command c = at(heading, omega, s.rpm);
        assertSame(AutoAim.Status.TURNING, c.status);
    }

    @Test
    public void noShotMeansNoTurnAndNoFire() {
        AutoAim.Command c = AutoAim.update(TARGET.x, TARGET.y - 2, Math.PI / 2, 0, 0, 0, 3000,
                TARGET, Shooter.POLLEN, null);
        assertSame(AutoAim.Status.NO_SHOT, c.status);
        assertEquals(0, c.turnPower, 0);
        assertEquals(0, c.targetRpm, 0);
    }

    @Test
    public void tooFastToShoot() {
        ShotSolution s = still();
        AutoAim.Command c = AutoAim.update(X, Y, s.aimHeadingRad, 60, 0, 0, s.rpm,
                TARGET, Shooter.POLLEN, null);
        assertSame(AutoAim.Status.MOVING_TOO_FAST, c.status);
    }

    @Test
    public void movingShotIsLeadSoTheBallStillArrives() {
        double vx = 30; // sliding toward the blue wall
        AutoAim.LEAD_MOVING_SHOTS = true;
        AutoAim.Command c = AutoAim.update(X, Y, Math.PI / 2, vx, 0, 0, 3000,
                TARGET, Shooter.POLLEN, null);
        AutoAim.LEAD_MOVING_SHOTS = false;
        AutoAim.Command plain = AutoAim.update(X, Y, Math.PI / 2, vx, 0, 0, 3000,
                TARGET, Shooter.POLLEN, null);
        // Leading aims back against the slide: further CCW (left) than not leading.
        assertTrue(c.shot.aimHeadingRad > plain.shot.aimHeadingRad);

        // Fly the led shot: release point + (launch velocity + robot velocity) * t.
        double lat = Shooter.FIRE_LATENCY_S;
        double[] p = ShotPlanner.releasePoint(X + vx * lat, Y, c.shot.aimHeadingRad, Shooter.POLLEN);
        double a = Math.toRadians(c.shot.hoodDeg);
        double v = Shooter.POLLEN.speedForRpm(c.shot.rpm);
        double t = c.shot.flightTimeS;
        double dir = c.shot.aimHeadingRad;
        double bx = p[0] + (v * Math.cos(a) * Math.cos(dir) + vx) * t;
        double by = p[1] + (v * Math.cos(a) * Math.sin(dir)) * t;
        assertEquals(TARGET.x, bx, 0.5);
        assertEquals(TARGET.y, by, 0.5);
    }
}
