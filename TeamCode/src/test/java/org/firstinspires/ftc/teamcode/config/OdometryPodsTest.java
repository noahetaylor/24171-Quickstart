package org.firstinspires.ftc.teamcode.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class OdometryPodsTest {
    private static final double EPS = 1e-9;

    /** Our measured layout: forward pod 7.5 behind / 4.25 right, strafe pod 7.5 behind / 5.25 left. */
    @Test
    public void measuredLayoutGivesExpectedPedroValues() {
        OdometryPods.Layout l = OdometryPods.resolve(
                new OdometryPods.Placement("leftRear", -7.5, 5.25, OdometryPods.LEFT),
                new OdometryPods.Placement("rightRear", -7.5, -4.25, OdometryPods.FRONT));

        assertEquals("rightRear", l.forwardPodName);
        assertEquals("leftRear", l.strafePodName);
        assertEquals(4.25, l.forwardPodY, EPS);   // 4.25 RIGHT -> positive for Pedro two-wheel
        assertEquals(-7.5, l.strafePodX, EPS);
        assertEquals(1, l.forwardPodDir, EPS);
        assertEquals(1, l.strafePodDir, EPS);
    }

    @Test
    public void argumentOrderDoesNotMatter() {
        OdometryPods.Placement fwd = new OdometryPods.Placement("p3", 1, 2, OdometryPods.FRONT);
        OdometryPods.Placement str = new OdometryPods.Placement("p0", 3, 4, OdometryPods.LEFT);
        OdometryPods.Layout ab = OdometryPods.resolve(fwd, str);
        OdometryPods.Layout ba = OdometryPods.resolve(str, fwd);

        assertEquals(ab.forwardPodName, ba.forwardPodName);
        assertEquals(ab.forwardPodY, ba.forwardPodY, EPS);
        assertEquals(ab.strafePodX, ba.strafePodX, EPS);
    }

    @Test
    public void podsCountingUpBackwardOrRightAreReversed() {
        OdometryPods.Layout l = OdometryPods.resolve(
                new OdometryPods.Placement("f", 0, 0, OdometryPods.BACK),
                new OdometryPods.Placement("s", 0, 0, OdometryPods.RIGHT));

        assertEquals(-1, l.forwardPodDir, EPS);
        assertEquals(-1, l.strafePodDir, EPS);
    }

    @Test
    public void leftSideForwardPodIsNegative() {
        OdometryPods.Layout l = OdometryPods.resolve(
                new OdometryPods.Placement("f", 0, 3, OdometryPods.FRONT),
                new OdometryPods.Placement("s", 2, 0, OdometryPods.LEFT));

        assertEquals(-3, l.forwardPodY, EPS);
        assertEquals(2, l.strafePodX, EPS);
    }

    @Test
    public void twoPodsOnTheSameAxisAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> OdometryPods.resolve(
                new OdometryPods.Placement("a", 0, 0, OdometryPods.FRONT),
                new OdometryPods.Placement("b", 0, 0, OdometryPods.BACK)));
    }

    @Test
    public void diagonalArrowIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new OdometryPods.Placement("a", 0, 0, 45));
    }
}
