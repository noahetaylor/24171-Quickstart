package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Blocker;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.shooting.ShotSpots;
import org.firstinspires.ftc.teamcode.vision.BallPicker;
import org.firstinspires.ftc.teamcode.vision.BallSighting;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PartnerPlanTest {

    // col 0..2 = A..C (x), row 0..5 = 1..6 (y), RED frame
    private static PartnerPlan plan(int col, int row, PartnerPlan.Window w) {
        PartnerPlan p = new PartnerPlan();
        p.set(col, row, w);
        return p;
    }

    @Test
    public void windowsBlockOnlyTheirTime() {
        PartnerPlan p = plan(1, 1, PartnerPlan.Window.EARLY);   // B2: x 24..48, y 24..48
        assertTrue(p.at(Alliance.RED, 5).pointBlocked(36, 36));
        assertFalse(p.at(Alliance.RED, 15).pointBlocked(36, 36));
        PartnerPlan all = plan(1, 1, PartnerPlan.Window.ALL);
        assertTrue(all.at(Alliance.RED, 0).pointBlocked(36, 36));
        assertTrue(all.at(Alliance.RED, 29.9).pointBlocked(36, 36));
        assertTrue(plan(1, 1, PartnerPlan.Window.MID).between(Alliance.RED, 8, 12).pointBlocked(36, 36));
        assertFalse(plan(1, 1, PartnerPlan.Window.LATE).between(Alliance.RED, 8, 12).pointBlocked(36, 36));
    }

    @Test
    public void marginKeepsTheRobotBodyOut() {
        Blocker b = plan(1, 1, PartnerPlan.Window.ALL).at(Alliance.RED, 0);
        assertTrue("3 in outside the tile", b.pointBlocked(51, 36));
        assertFalse("10 in outside the tile", b.pointBlocked(58, 36));
    }

    @Test
    public void blueSeesTheSamePlanRotated() {
        PartnerPlan p = plan(0, 0, PartnerPlan.Window.ALL);    // red A1 = blue F6
        assertTrue(p.at(Alliance.BLUE, 0).pointBlocked(132, 132));
        assertFalse(p.at(Alliance.BLUE, 0).pointBlocked(132, 12));
        assertEquals("A1", PartnerPlan.tileName(0, 0, Alliance.RED));
        assertEquals("F6", PartnerPlan.tileName(0, 0, Alliance.BLUE));
        assertEquals("C6", PartnerPlan.tileName(2, 5, Alliance.RED));
        assertEquals("D1", PartnerPlan.tileName(2, 5, Alliance.BLUE));
    }

    @Test
    public void segmentsThroughATileAreBlocked() {
        Blocker b = plan(1, 2, PartnerPlan.Window.ALL).at(Alliance.RED, 0);   // B3: x 24..48, y 48..72
        assertTrue(b.segmentBlocked(10, 60, 62, 60));
        assertFalse(b.segmentBlocked(10, 20, 62, 20));
        // Already inside B3: driving out is fine; driving out and into another blocked tile is not.
        PartnerPlan two = plan(1, 2, PartnerPlan.Window.ALL);
        two.set(1, 4, PartnerPlan.Window.ALL);                     // B5: y 96..120
        Blocker b2 = two.at(Alliance.RED, 0);
        assertFalse("leave B3, stop short of B5", b2.segmentBlocked(36, 60, 36, 84));
        assertTrue("leave B3, run into B5", b2.segmentBlocked(36, 60, 36, 130));
        assertTrue("never get out of B3", b2.segmentBlocked(36, 60, 40, 64));
    }

    @Test
    public void textRoundTripsAndSkipsJunk() {
        PartnerPlan p = new PartnerPlan();
        p.setName("14250 left push");
        p.set(0, 0, PartnerPlan.Window.ALL);
        p.add(new PartnerPlan.Zone(30.5, 40, 50, 80.25, 3, 12.5));
        PartnerPlan q = PartnerPlan.fromText(p.toText() + "zone=1,2,3\n# comment\nzone=a,b,c,d,e,f\nzone=0,0,10,10,9,4\n");
        assertEquals("14250 left push", q.name());
        assertEquals(2, q.zoneCount());   // the three bad zone lines are skipped
        assertEquals(p.toText(), q.toText());
        assertSame(PartnerPlan.Window.ALL, q.get(0, 0));
        assertEquals(0, PartnerPlan.fromText(null).zoneCount());
    }

    @Test
    public void freeFormZonesBlockWithTheirOwnTimes() {
        PartnerPlan p = new PartnerPlan();
        p.add(new PartnerPlan.Zone(30, 40, 50, 80, 3, 12.5));   // a laptop-drawn strip
        assertTrue(p.at(Alliance.RED, 5).pointBlocked(40, 60));
        assertFalse(p.at(Alliance.RED, 13).pointBlocked(40, 60));
        assertFalse(p.at(Alliance.RED, 5).pointBlocked(65, 60));     // 15 in outside
        assertTrue(p.at(Alliance.BLUE, 5).pointBlocked(104, 84));    // same zone, rotated for blue
    }

    @Test
    public void tileEditsOnlyTouchTheirOwnTile() {
        PartnerPlan p = new PartnerPlan();
        p.add(new PartnerPlan.Zone(30, 40, 50, 80, 0, 30));
        p.cycle(1, 2);                                   // B3 -> whole Auto
        assertSame(PartnerPlan.Window.ALL, p.get(1, 2));
        p.set(1, 2, PartnerPlan.Window.FREE);
        assertEquals(1, p.zoneCount());                  // the laptop zone is still there
        assertSame(PartnerPlan.Window.FREE, p.get(1, 2));
        PartnerPlan copy = new PartnerPlan();
        copy.copyFrom(p);
        assertEquals(p.toText(), copy.toText());
    }

    @Test
    public void rendersFromTheDriveTeamsWall() {
        PartnerPlan partial = new PartnerPlan();
        partial.add(new PartnerPlan.Zone(30, 40, 40, 45, 0, 30));   // part of B2
        assertTrue(partial.render(-1, 0, Alliance.RED)[2].contains("+"));
        PartnerPlan p = plan(0, 5, PartnerPlan.Window.ALL);   // A6: nearest column, far-wall end
        String[] red = p.render(0, 5, Alliance.RED);
        assertTrue(red[0].startsWith("far wall"));
        // Bottom grid line is column A, and A6 is leftmost (red's left is the far wall).
        assertTrue(red[3].startsWith("A [#]"));
        String[] blue = p.render(2, 0, Alliance.BLUE);
        assertTrue(blue[0].startsWith("audience"));
        assertTrue(blue[3].startsWith("F  # "));   // F1 for blue, on blue's left (audience side)
    }

    @Test
    public void ballsInOrBehindBlockedTilesAreSkipped() {
        Blocker b = plan(1, 1, PartnerPlan.Window.ALL).at(Alliance.RED, 0);   // B2
        List<BallSighting> balls = Arrays.asList(
                new BallSighting(BallSighting.Kind.POLLEN, 36, 36, 0),    // in B2
                new BallSighting(BallSighting.Kind.POLLEN, 60, 36, 0),    // behind B2 from the robot
                new BallSighting(BallSighting.Kind.POLLEN, 12, 60, 0));   // clear
        BallSighting pick = BallPicker.pick(balls, Alliance.RED, true, 10, 36, 0, b);
        assertNotNull(pick);
        assertEquals(12, pick.x, 0);
        assertEquals(36, BallPicker.pick(balls, Alliance.RED, true, 10, 36, 0).x, 0);   // no plan: nearest
    }

    @Test
    public void shotSpotsAvoidBlockedTiles() {
        FieldPoint target = Cell.RED_AUDIENCE.openingWhenUp();
        FieldPoint free = ShotSpots.nearest(20, 30, target, 50);
        // Block whatever tile the free spot landed in, for the whole Auto.
        PartnerPlan p = plan((int) (free.x / 24), (int) (free.y / 24), PartnerPlan.Window.ALL);
        Blocker b = p.at(Alliance.RED, 0);
        FieldPoint spot = ShotSpots.nearest(20, 30, target, 50, b);
        assertNotNull(spot);
        assertFalse(b.pointBlocked(spot.x, spot.y));
        assertFalse(b.segmentBlocked(20, 30, spot.x, spot.y));
    }

    @Test
    public void emptyPlanBlocksNothing() {
        Blocker b = new PartnerPlan().between(Alliance.RED, 0, 30);
        assertFalse(b.pointBlocked(36, 36));
        assertFalse(b.segmentBlocked(0, 0, 72, 144));
    }
}
