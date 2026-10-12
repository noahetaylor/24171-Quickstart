package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.tuning.AimTuning;
import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ShotPlannerTest {

    private static final double EPS = 1e-6;

    private double yaw, hoodMin, hoodMax, eff, maxRpm;

    @Before
    public void saveTunables() {
        yaw = ShooterTuning.LAUNCH_YAW_DEG;
        hoodMin = ShooterTuning.HOOD_MIN_DEG;
        hoodMax = ShooterTuning.HOOD_MAX_DEG;
        eff = ShooterTuning.EXIT_EFFICIENCY;
        maxRpm = ShooterTuning.MAX_RPM;
    }

    @After
    public void restoreTunables() {
        ShooterTuning.LAUNCH_YAW_DEG = yaw;
        ShooterTuning.HOOD_MIN_DEG = hoodMin;
        ShooterTuning.HOOD_MAX_DEG = hoodMax;
        ShooterTuning.EXIT_EFFICIENCY = eff;
        ShooterTuning.MAX_RPM = maxRpm;
    }

    // ------------------------------------------------------------ physics

    @Test
    public void physicsSpeedLandsOnTheTarget() {
        double d = 80, h = 38, a = Math.toRadians(60);
        double v = ShotPhysics.speedFor(d, h, a);
        double t = ShotPhysics.timeOfFlight(d, v, a);
        double heightAtTarget = v * Math.sin(a) * t - 0.5 * ShotPhysics.G_IN_PER_S2 * t * t;
        assertEquals(h, heightAtTarget, 1e-6);
    }

    @Test
    public void physicsRefusesTargetsAboveTheLaunchLine() {
        // tan(60) * 20 = 34.6 < 38: can't get there at 60 degrees.
        assertTrue(Double.isNaN(ShotPhysics.speedFor(20, 38, Math.toRadians(60))));
    }

    @Test
    public void minSpeedAngleReallyIsTheMinimum() {
        double d = 70, h = 38;
        double best = ShotPhysics.minSpeedAngle(d, h);
        double vBest = ShotPhysics.speedFor(d, h, best);
        assertTrue(vBest < ShotPhysics.speedFor(d, h, best + 0.05));
        assertTrue(vBest < ShotPhysics.speedFor(d, h, best - 0.05));
    }

    @Test
    public void rpmAndSpeedAreInverses() {
        double v = 300;
        assertEquals(v, Shooter.POLLEN.speedForRpm(Shooter.POLLEN.rpmForSpeed(v)), 1e-9);
    }

    // -------------------------------------------------------------- table

    @Test
    public void tableInterpolatesBetweenRowsInAnyOrder() {
        ShotTable t = new ShotTable().add(72, 60, 3600).add(48, 60, 3000);
        ShotTable.Row r = t.lookup(60);
        assertNotNull(r);
        assertEquals(3300, r.rpm, EPS);
        assertEquals(3000, t.lookup(48).rpm, EPS);
        assertEquals(3600, t.lookup(72).rpm, EPS);
    }

    @Test
    public void tableCoversOnlyBetweenItsEnds() {
        ShotTable t = new ShotTable().add(48, 60, 3000).add(72, 60, 3600);
        assertNull(t.lookup(47.9));
        assertNull(t.lookup(72.1));
        assertNull(new ShotTable().add(48, 60, 3000).lookup(48)); // one row is not enough
    }

    // ------------------------------------------------------------ planner

    /** Distance from the target to the launch ray: ~0 when aimed right. */
    private static double missDistance(double robotX, double robotY, ShotSolution s,
                                       Shooter sh, FieldPoint target) {
        double[] p = ShotPlanner.releasePoint(robotX, robotY, s.aimHeadingRad, sh);
        double dir = s.aimHeadingRad + Math.toRadians(ShooterTuning.LAUNCH_YAW_DEG);
        double dx = target.x - p[0], dy = target.y - p[1];
        return Math.abs(Math.cos(dir) * dy - Math.sin(dir) * dx);
    }

    @Test
    public void aimLinesTheOffCenterShooterUpWithTheTarget() {
        FieldPoint target = new HiveState().target(Alliance.RED);
        for (Shooter sh : Shooter.values()) {
            ShotSolution s = ShotPlanner.plan(30, 20, 0, target, sh, null);
            assertEquals(sh + " miss", 0, missDistance(30, 20, s, sh, target), 1e-6);
        }
    }

    @Test
    public void aimingWorksWhenShootingOutTheBack() {
        ShooterTuning.LAUNCH_YAW_DEG = 180;
        FieldPoint target = new HiveState().target(Alliance.BLUE);
        ShotSolution s = ShotPlanner.plan(110, 30, 0, target, Shooter.NECTAR, null);
        assertEquals(0, missDistance(110, 30, s, Shooter.NECTAR, target), 1e-6);
        // Robot faces away from the target.
        double toTarget = Math.atan2(target.y - 30, target.x - 110);
        assertTrue(Math.cos(s.aimHeadingRad - toTarget) < 0);
    }

    @Test
    public void turnIsFromTheCurrentHeading() {
        FieldPoint target = new FieldPoint(72, 120, 53.5);
        ShotSolution a = ShotPlanner.plan(72, 40, 0, target, Shooter.POLLEN, null);
        ShotSolution b = ShotPlanner.plan(72, 40, a.aimHeadingRad, target, Shooter.POLLEN, null);
        assertEquals(0, b.turnRad, 1e-9);
        assertEquals(a.aimHeadingRad, a.turnRad, 1e-9);
    }

    @Test
    public void physicsShotFromMidFieldIsFeasible() {
        FieldPoint target = new HiveState().target(Alliance.RED);
        ShotSolution s = ShotPlanner.plan(30, 20, 0, target, Shooter.POLLEN, null);
        assertSame(ShotSolution.Source.PHYSICS, s.source);
        assertTrue(s.toString(), s.feasible);
        assertEquals(60, s.hoodDeg, EPS);
        assertTrue(s.rpm > 1000 && s.rpm < ShooterTuning.MAX_RPM);
        assertEquals(53.5 - Shooter.POLLEN.releaseHeightIn, s.riseIn, EPS);
    }

    @Test
    public void tooCloseIsNotFeasible() {
        FieldPoint target = new HiveState().target(Alliance.RED);
        // Release point right under the opening.
        ShotSolution s = ShotPlanner.plan(target.x + 7.28, target.y + 3.82, 0, target, Shooter.POLLEN, null);
        assertFalse(s.feasible);
    }

    @Test
    public void overMaxRpmIsNotFeasible() {
        ShooterTuning.MAX_RPM = 100;
        ShotSolution s = ShotPlanner.plan(10, 10, 0, new FieldPoint(130, 130, 53.5), Shooter.POLLEN, null);
        assertFalse(s.feasible);
    }

    @Test
    public void tableWinsWhereItCoversTheDistance() {
        FieldPoint target = new FieldPoint(72, 120, 53.5);
        ShotSolution physics = ShotPlanner.plan(72, 40, Math.PI / 2, target, Shooter.POLLEN, null);
        double d = physics.distanceIn;
        ShotTable t = new ShotTable().add(d - 10, 60, 2000).add(d + 10, 60, 4000);
        ShotSolution tabled = ShotPlanner.plan(72, 40, Math.PI / 2, target, Shooter.POLLEN, t);
        assertSame(ShotSolution.Source.TABLE, tabled.source);
        assertEquals(3000, tabled.rpm, 1e-6);
        assertEquals(physics.aimHeadingRad, tabled.aimHeadingRad, 1e-12);
    }

    @Test
    public void adjustableHoodPicksTheLowSpeedAngleWithinRange() {
        ShooterTuning.HOOD_MIN_DEG = 50;
        ShooterTuning.HOOD_MAX_DEG = 70;
        FieldPoint target = new FieldPoint(72, 120, 53.5);
        ShotSolution s = ShotPlanner.plan(72, 40, 0, target, Shooter.POLLEN, null);
        assertTrue(s.hoodDeg >= 50 && s.hoodDeg <= 70);
        double best = Math.toDegrees(ShotPhysics.minSpeedAngle(s.distanceIn, s.riseIn));
        assertEquals(Math.max(50, Math.min(70, best)), s.hoodDeg, 1e-9);
    }
}
