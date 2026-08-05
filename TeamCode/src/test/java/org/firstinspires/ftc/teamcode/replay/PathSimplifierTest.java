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

        List<PoseSample> result = PathSimplifier.simplify(points, 1.0, Math.toRadians(10));
        assertEquals(2, result.size());
    }

    @Test
    public void removesCollinearPoints() {
        // A perfectly straight line from (0,0) to (10,0), densely sampled,
        // heading constant throughout. Every intermediate point should be
        // removed, leaving just the two endpoints.
        List<PoseSample> points = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            points.add(new PoseSample(i * 100L, i, 0, 0));
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 0.5, Math.toRadians(10));

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

        List<PoseSample> result = PathSimplifier.simplify(points, 0.5, Math.toRadians(10));

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

        // huge epsilons, simplifies as aggressively as possible
        List<PoseSample> result = PathSimplifier.simplify(points, 100.0, Math.PI);

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

        List<PoseSample> loose = PathSimplifier.simplify(points, 5.0, Math.toRadians(10));
        List<PoseSample> tight = PathSimplifier.simplify(points, 0.01, Math.toRadians(10));

        assertTrue(tight.size() >= loose.size());
    }

    @Test
    public void preservesInPlaceRotation() {
        // Robot sits at a fixed (5,5) — position never changes at all —
        // and pauses before rotating: heading holds at 0 for the first
        // half of the samples, then sweeps to 90 degrees in the second
        // half. This is deliberately NOT a constant-rate rotation — a
        // perfectly linear heading ramp is actually fine to collapse to
        // just 2 points, since setLinearHeadingInterpolation reproduces
        // it exactly. This pause-then-spin shape has a genuine kink a
        // realistic driver would produce (hold a position, then spin),
        // which a straight line between start/end heading would NOT
        // predict correctly at the midpoint — that's what should get
        // caught and preserved.
        List<PoseSample> points = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            points.add(new PoseSample(i * 50L, 5.0, 5.0, 0)); // paused, heading flat
        }
        for (int i = 1; i <= 5; i++) {
            double heading = (Math.PI / 2) * (i / 5.0);
            points.add(new PoseSample((5 + i) * 50L, 5.0, 5.0, heading)); // now rotating
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 1.0, Math.toRadians(5));

        assertTrue("expected more than just start/end to survive a pause-then-rotate sequence",
                result.size() > 2);
        assertEquals(Math.PI / 2, result.get(result.size() - 1).headingRadians, 0.001);
    }

    @Test
    public void driveRotateDriveSequenceKeepsAllThreePhases() {
        // The exact "amateur driver" scenario: drive straight, stop and
        // rotate in place, then drive straight again in the new direction.
        List<PoseSample> points = new ArrayList<>();

        // Phase 1: drive from (0,0) to (10,0), heading fixed at 0.
        for (int i = 0; i <= 5; i++) {
            points.add(new PoseSample(i * 50L, i * 2.0, 0, 0));
        }
        // Phase 2: rotate in place at (10,0) from heading 0 to heading PI/2.
        for (int i = 1; i <= 5; i++) {
            double heading = (Math.PI / 2) * (i / 5.0);
            points.add(new PoseSample((5 + i) * 50L, 10.0, 0.0, heading));
        }
        // Phase 3: drive from (10,0) to (10,10), heading fixed at PI/2.
        for (int i = 1; i <= 5; i++) {
            points.add(new PoseSample((10 + i) * 50L, 10.0, i * 2.0, Math.PI / 2));
        }

        List<PoseSample> result = PathSimplifier.simplify(points, 0.5, Math.toRadians(5));

        // Expect at least: start, corner before rotation, corner after
        // rotation, end — i.e. the rotation phase must leave a trace.
        assertTrue("expected the rotation phase to be preserved as distinct waypoints",
                result.size() >= 4);
    }
}
