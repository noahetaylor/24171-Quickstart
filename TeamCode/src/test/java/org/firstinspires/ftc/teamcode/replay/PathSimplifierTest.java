package org.firstinspires.ftc.teamcode.replay;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathSimplifierTest {

    @Test
    public void keepsFewerThanThreePointsUnchanged() {
        List<PoseSample> points = new ArrayList<>();
        points.add(new PoseSample(0, 0, 0, 0));
        points.add(new PoseSample(100, 10, 10, 0));

        List<PoseSample> result = PathSimplifier.simplify(points, 1.0);
        assertEquals(2, result.size());
    }

    @Test
    public void removesCollinearPoints() {
        // A perfectly straight line from (0,0) to (10,0), densely sampled.
        // Every intermediate point lies exactly on the line, so all should
        // be removed, leaving just the two endpoints.
        List<PoseSample> points = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            points.add(new PoseSample(i * 100L, i, 0, 0));
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 0.5);

        assertEquals(2, result.size());
        assertEquals(0.0, result.get(0).x, 0.0001);
        assertEquals(10.0, result.get(1).x, 0.0001);
    }

    @Test
    public void keepsSignificantCornerPoint() {
        // An "L" shaped path: straight along x, then straight along y.
        // The corner at (10, 0) is a real direction change and must survive.
        List<PoseSample> points = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            points.add(new PoseSample(i * 100L, i, 0, 0)); // (0,0) -> (10,0)
        }
        for (int i = 1; i <= 10; i++) {
            points.add(new PoseSample((10 + i) * 100L, 10, i, 0)); // (10,0) -> (10,10)
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 0.5);

        assertEquals(3, result.size()); // start, corner, end
        assertEquals(0.0, result.get(0).x, 0.0001);
        assertEquals(0.0, result.get(0).y, 0.0001);
        assertEquals(10.0, result.get(1).x, 0.0001);
        assertEquals(0.0, result.get(1).y, 0.0001);
        assertEquals(10.0, result.get(2).x, 0.0001);
        assertEquals(10.0, result.get(2).y, 0.0001);
    }

    @Test
    public void alwaysKeepsFirstAndLastPoint() {
        List<PoseSample> points = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            points.add(new PoseSample(i * 50L, i, 0, 0));
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 100.0); // huge epsilon, simplifies aggressively

        assertEquals(2, result.size());
        assertEquals(points.get(0).x, result.get(0).x, 0.0001);
        assertEquals(points.get(points.size() - 1).x, result.get(result.size() - 1).x, 0.0001);
    }

    @Test
    public void smallerEpsilonKeepsMorePoints() {
        List<PoseSample> points = new ArrayList<>();
        // A gentle curve approximated by small steps, not a perfect line.
        for (int i = 0; i <= 20; i++) {
            double x = i;
            double y = 0.05 * i * i; // slight upward curve
            points.add(new PoseSample(i * 50L, x, y, 0));
        }

        List<PoseSample> loose = PathSimplifier.simplify(points, 5.0);
        List<PoseSample> tight = PathSimplifier.simplify(points, 0.01);

        assertTrue(tight.size() >= loose.size());
    }
}
