package org.firstinspires.ftc.teamcode.replay;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathSegmentPlannerTest {

    @Test
    public void emptyWaypointListProducesNoSegments() {
        List<PoseSample> waypoints = new ArrayList<>();
        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(0, segments.size());
    }

    @Test
    public void singleWaypointProducesNoSegments() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 0, 0, 0));
        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(0, segments.size());
    }

    @Test
    public void nWaypointsProduceNMinusOneSegments() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 0, 0, 0));
        waypoints.add(new PoseSample(100, 10, 0, 0));
        waypoints.add(new PoseSample(200, 10, 10, 0));
        waypoints.add(new PoseSample(300, 20, 10, 0));

        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(3, segments.size());
    }

    @Test
    public void classifiesNormalDriveAsNotRotation() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 0, 0, 0));
        waypoints.add(new PoseSample(100, 20, 0, 0)); // moved 20 inches

        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(1, segments.size());
        assertFalse(segments.get(0).isRotationInPlace);
    }

    @Test
    public void classifiesInPlaceRotationCorrectly() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 5, 5, 0));
        waypoints.add(new PoseSample(500, 5, 5, Math.PI / 2)); // same position, heading changed

        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(1, segments.size());
        assertTrue(segments.get(0).isRotationInPlace);
        assertEquals(0, segments.get(0).fromHeadingRadians, 0.0001);
        assertEquals(Math.PI / 2, segments.get(0).toHeadingRadians, 0.0001);
    }

    @Test
    public void driveRotateDriveSequenceClassifiesEachSegmentCorrectly() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 0, 0, 0));            // start
        waypoints.add(new PoseSample(100, 10, 0, 0));         // after drive
        waypoints.add(new PoseSample(200, 10, 0, Math.PI / 2)); // after rotation, same position
        waypoints.add(new PoseSample(300, 10, 10, Math.PI / 2)); // after second drive

        List<PlannedSegment> segments = PathSegmentPlanner.planSegments(waypoints);
        assertEquals(3, segments.size());

        assertFalse("segment 1 should be a drive", segments.get(0).isRotationInPlace);
        assertTrue("segment 2 should be a rotation", segments.get(1).isRotationInPlace);
        assertFalse("segment 3 should be a drive", segments.get(2).isRotationInPlace);
    }

    @Test
    public void customThresholdChangesClassification() {
        List<PoseSample> waypoints = new ArrayList<>();
        waypoints.add(new PoseSample(0, 0, 0, 0));
        waypoints.add(new PoseSample(100, 3, 0, 0)); // moved 3 inches

        // Default threshold (1.0) would call this a drive.
        List<PlannedSegment> withDefault = PathSegmentPlanner.planSegments(waypoints);
        assertFalse(withDefault.get(0).isRotationInPlace);

        // A looser threshold (5.0) calls the same segment a rotation instead.
        List<PlannedSegment> withLooseThreshold = PathSegmentPlanner.planSegments(waypoints, 5.0);
        assertTrue(withLooseThreshold.get(0).isRotationInPlace);
    }
}
