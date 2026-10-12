package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VisionTest {

    private static final double EPS = 1e-6;

    /** What the camera would report for a ball at (bx, by): {tx, ty, angRadius}. The inverse of BallLocator. */
    private static double[] observe(BallSighting.Kind kind, double bx, double by,
                                    double pan, double tilt, double rx, double ry, double rh) {
        double camX = rx + LimelightMount.LENS_FORWARD_IN * Math.cos(rh) - LimelightMount.LENS_LEFT_IN * Math.sin(rh);
        double camY = ry + LimelightMount.LENS_FORWARD_IN * Math.sin(rh) + LimelightMount.LENS_LEFT_IN * Math.cos(rh);
        double vx = bx - camX, vy = by - camY, vz = kind.radiusIn - LimelightMount.LENS_HEIGHT_IN;
        // Undo yaw (pan + heading).
        double yaw = Math.toRadians(pan) + rh;
        double f1 = vx * Math.cos(yaw) + vy * Math.sin(yaw);
        double l = -vx * Math.sin(yaw) + vy * Math.cos(yaw);
        // Undo tilt.
        double t = Math.toRadians(tilt);
        double f = f1 * Math.cos(t) + vz * Math.sin(t);
        double u = -f1 * Math.sin(t) + vz * Math.cos(t);
        double tx = Math.toDegrees(Math.atan(-l / f));
        double ty = Math.toDegrees(Math.atan(u / f));
        double slant = Math.sqrt(vx * vx + vy * vy + vz * vz);
        return new double[]{tx, ty, Math.toDegrees(Math.atan(kind.radiusIn / slant))};
    }

    // ------------------------------------------------------------ locator

    @Test
    public void straightAheadOnTheFloor() {
        double tilt = -30;
        double drop = LimelightMount.LENS_HEIGHT_IN - BallSighting.Kind.POLLEN.radiusIn;
        double ahead = drop / Math.tan(Math.toRadians(30));
        double slant = Math.hypot(ahead, drop);
        double angR = Math.toDegrees(Math.atan(BallSighting.Kind.POLLEN.radiusIn / slant));
        BallSighting s = BallLocator.locate(BallSighting.Kind.POLLEN, 0, 0, angR, 0, tilt, 50, 50, 0);
        assertNotNull(s);
        assertEquals(50 + LimelightMount.LENS_FORWARD_IN + ahead, s.x, EPS);
        assertEquals(50, s.y, EPS);
    }

    @Test
    public void locatesWhatTheCameraSeesFromAnyPose() {
        Random r = new Random(7);
        for (int i = 0; i < 200; i++) {
            BallSighting.Kind kind = BallSighting.Kind.values()[r.nextInt(3)];
            double rx = 20 + 100 * r.nextDouble(), ry = 20 + 100 * r.nextDouble();
            double rh = (r.nextDouble() * 2 - 1) * Math.PI;
            double pan = -60 + 120 * r.nextDouble();
            double tilt = -50 + 30 * r.nextDouble();
            // A ball somewhere in front of the camera, 15..70 in away.
            double yaw = Math.toRadians(pan) + rh + Math.toRadians(-20 + 40 * r.nextDouble());
            double dist = 15 + 55 * r.nextDouble();
            double bx = rx + dist * Math.cos(yaw), by = ry + dist * Math.sin(yaw);
            double[] o = observe(kind, bx, by, pan, tilt, rx, ry, rh);
            if (Math.abs(o[0]) > 27 || Math.abs(o[1]) > 21) continue; // outside the field of view
            BallSighting s = BallLocator.locate(kind, o[0], o[1], o[2], pan, tilt, rx, ry, rh);
            assertNotNull("case " + i, s);
            assertEquals("x case " + i, bx, s.x, 1e-6);
            assertEquals("y case " + i, by, s.y, 1e-6);
        }
    }

    @Test
    public void panAndRightAreTheRightWayRound() {
        // Panned left 90 deg: a ball straight left of the CAMERA (which is ahead of center) is dead center.
        double[] left = observe(BallSighting.Kind.POLLEN, 50 + LimelightMount.LENS_FORWARD_IN, 80, 90, -30, 50, 50, 0);
        assertEquals(0, left[0], 1e-6);
        double[] right = observe(BallSighting.Kind.POLLEN, 75, 45, 0, -30, 50, 50, 0);
        assertTrue("ball to the right has tx > 0", right[0] > 0);
    }

    @Test
    public void rejectsRaysAboveTheHorizonAndWrongSizes() {
        assertNull(BallLocator.locate(BallSighting.Kind.POLLEN, 0, 5, 2, 0, 0, 50, 50, 0));
        double[] o = observe(BallSighting.Kind.POLLEN, 80, 50, 0, -30, 50, 50, 0);
        assertNull("3x too big", BallLocator.locate(BallSighting.Kind.POLLEN, o[0], o[1], o[2] * 3, 0, -30, 50, 50, 0));
        assertNull("far too small", BallLocator.locate(BallSighting.Kind.POLLEN, o[0], o[1], o[2] * 0.3, 0, -30, 50, 50, 0));
    }

    // ------------------------------------------------------------ parser

    @Test
    public void parsesPipelineOutput() {
        double[] out = {2, -3.5, -10.2, 1, 2.9, 4.0, -12.0, 3, 3.1};
        List<LimelightBalls.Detection> d = LimelightBalls.parse(out);
        assertEquals(2, d.size());
        assertSame(BallSighting.Kind.POLLEN, d.get(0).kind);
        assertSame(BallSighting.Kind.BLUE_NECTAR, d.get(1).kind);
        assertEquals(-12.0, d.get(1).tyDeg, 0);
    }

    @Test
    public void toleratesTruncatedOrOddOutput() {
        assertEquals(0, LimelightBalls.parse(null).size());
        assertEquals(0, LimelightBalls.parse(new double[0]).size());
        assertEquals(1, LimelightBalls.parse(new double[]{3, 1, 2, 1, 3, 5, 6}).size()); // says 3, has 1.5
        assertEquals(0, LimelightBalls.parse(new double[]{1, 1, 2, 9, 3}).size());       // unknown class
    }

    // ------------------------------------------------------------ picker

    private static BallSighting ball(BallSighting.Kind k, double x, double y) {
        return new BallSighting(k, x, y, 0);
    }

    @Test
    public void skipsOpposingNectarAndPicksTheNearest() {
        List<BallSighting> balls = Arrays.asList(
                ball(BallSighting.Kind.BLUE_NECTAR, 32, 30),   // nearest, but the opponent's
                ball(BallSighting.Kind.POLLEN, 40, 40),
                ball(BallSighting.Kind.RED_NECTAR, 55, 55));
        BallSighting b = BallPicker.pick(balls, Alliance.RED, true, 30, 30, Math.PI / 4);
        assertSame(BallSighting.Kind.POLLEN, b.kind);
        BallSighting blue = BallPicker.pick(balls, Alliance.BLUE, false, 30, 30, Math.PI / 4);
        assertSame(BallSighting.Kind.BLUE_NECTAR, blue.kind);
    }

    @Test
    public void staysOnOurSideInAuto() {
        List<BallSighting> balls = new ArrayList<>();
        balls.add(ball(BallSighting.Kind.POLLEN, 90, 30));     // blue's side
        assertNull(BallPicker.pick(balls, Alliance.RED, true, 40, 30, 0));
        assertNotNull(BallPicker.pick(balls, Alliance.RED, false, 40, 30, 0));   // TeleOp: fine
        assertNotNull(BallPicker.pick(balls, Alliance.BLUE, true, 110, 30, Math.PI));
    }

    @Test
    public void skipsBallsUnderTheFrameOrJammedInACorner() {
        List<BallSighting> balls = Arrays.asList(
                ball(BallSighting.Kind.POLLEN, 60, 72),     // under the HIVE frame
                ball(BallSighting.Kind.POLLEN, 1.4, 1.4));  // jammed in the corner
        assertNull(BallPicker.pick(balls, Alliance.RED, false, 30, 30, 0));
    }

    @Test
    public void preferBallsAheadOverOnesBehind() {
        List<BallSighting> balls = Arrays.asList(
                ball(BallSighting.Kind.POLLEN, 25, 40),   // 15 in behind
                ball(BallSighting.Kind.POLLEN, 60, 40));  // 20 in ahead (still our side)
        BallSighting b = BallPicker.pick(balls, Alliance.RED, true, 40, 40, 0);
        assertEquals(60, b.x, 0);
    }
}
