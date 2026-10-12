package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShotSpotsTest {

    private static final FieldPoint RED_UP = Cell.RED_AUDIENCE.openingWhenUp();

    private static void assertOnRingAndAllowed(FieldPoint spot, FieldPoint target, double r) {
        assertNotNull(spot);
        assertEquals(r, spot.distanceTo(target.x, target.y), 1e-6);
        assertTrue(ShotSpots.allowed(spot.x, spot.y));
    }

    @Test
    public void robotAlreadyOnTheRingStaysPut() {
        // Straight out toward the red wall from the red CELL: inside the field, outside the frame.
        double r = 40;
        FieldPoint spot = ShotSpots.nearest(RED_UP.x - r, RED_UP.y, RED_UP, r);
        assertOnRingAndAllowed(spot, RED_UP, r);
        assertEquals(RED_UP.x - r, spot.x, 1e-6);
        assertEquals(RED_UP.y, spot.y, 1e-6);
    }

    @Test
    public void farAwayRobotGetsTheRingPointTowardIt() {
        double r = 40;
        FieldPoint spot = ShotSpots.nearest(10, RED_UP.y, RED_UP, r);
        assertOnRingAndAllowed(spot, RED_UP, r);
        assertEquals(RED_UP.x - r, spot.x, 1e-6);
    }

    @Test
    public void spotsNeverLandUnderTheFrameOrInAWall() {
        double r = 60;
        for (int x = 0; x <= 144; x += 12) {
            for (int y = 0; y <= 144; y += 12) {
                FieldPoint spot = ShotSpots.nearest(x, y, RED_UP, r);
                assertOnRingAndAllowed(spot, RED_UP, r);
            }
        }
    }

    @Test
    public void frameAndWallsAreKeptOut() {
        assertFalse(ShotSpots.allowed(72, 72));   // under the HIVE
        assertFalse(ShotSpots.allowed(72, 3));    // in the audience wall
        assertTrue(ShotSpots.allowed(20, 20));
    }

    @Test
    public void blueIsRedRotated() {
        FieldPoint blueUp = Cell.BLUE_SCORING.openingWhenUp();
        FieldPoint redRobot = new FieldPoint(30, 20);
        FieldPoint blueRobot = Alliance.BLUE.fromRed(redRobot);
        FieldPoint red = ShotSpots.nearest(redRobot.x, redRobot.y, RED_UP, 60);
        FieldPoint blue = ShotSpots.nearest(blueRobot.x, blueRobot.y, blueUp, 60);
        FieldPoint expected = red.rotated180();
        assertEquals(expected.x, blue.x, 1e-6);
        assertEquals(expected.y, blue.y, 1e-6);
    }

    @Test
    public void impossibleRingGivesNull() {
        assertNull(ShotSpots.nearest(20, 20, RED_UP, 300));
    }
}
