package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AutoLogicTest {

    // ------------------------------------------------------------ start spots

    @Test
    public void everyStartSpotIsLegal() {
        for (StartSpot s : StartSpot.values()) {
            assertNull(s + ": " + s.whyIllegal(), s.whyIllegal());
        }
    }

    @Test
    public void illegalStartsAreCaught() {
        assertNotNull("in the loading zone", StartSpot.whyIllegal(8.6, 108, 0));
        assertNotNull("over the center line", StartSpot.whyIllegal(70, 8.6, 90));
        assertNotNull("not touching a wall", StartSpot.whyIllegal(40, 40, 90));
        assertNotNull("touching the red-wall FLOWER", StartSpot.whyIllegal(8.6, 50, 0));
        assertNotNull("on the garden", StartSpot.whyIllegal(12, 8.6, 90));
    }

    // ------------------------------------------------------------ ledger

    @Test
    public void startingCellTipsOnTheThirdPollen() {
        CellLedger l = new CellLedger();
        assertEquals(3, l.pollenToTip());
        l.shot(2, 0);
        assertFalse(l.tipExpected());
        l.shot(1, 0);
        assertTrue(l.tipExpected());
        assertEquals(1, l.pollenToTip());   // "should" have tipped: keep shooting one at a time
    }

    @Test
    public void emptyCellNeedsEightPollenOrThreeAndThree() {
        CellLedger l = new CellLedger();
        l.tipped();
        assertEquals(8, l.pollenToTip());
        l.shot(0, 3);
        assertEquals(3, l.pollenToTip());
    }

    // ------------------------------------------------------------ brain

    @Test
    public void aTypicalAutoGoesShootGardenShootCollectShoot() {
        // Preload: 4 held, 3 to tip.
        assertSame(AutoBrain.Job.SHOOT, AutoBrain.next(4, 29, true, false, 3));
        // After tip 1 + the 4th ball into the new CELL: empty, 7 to tip.
        assertSame(AutoBrain.Job.GARDEN, AutoBrain.next(0, 24, true, false, 7));
        // Garden gave 4: full, shoot.
        assertSame(AutoBrain.Job.SHOOT, AutoBrain.next(4, 17, true, true, 7));
        // 3 to tip: go find 3 more.
        assertSame(AutoBrain.Job.COLLECT, AutoBrain.next(0, 14, true, true, 3));
        assertSame(AutoBrain.Job.COLLECT, AutoBrain.next(2, 11, true, true, 3));
        // Enough for tip 2.
        assertSame(AutoBrain.Job.SHOOT, AutoBrain.next(3, 9, true, true, 3));
    }

    @Test
    public void partnerTakesTheGarden() {
        assertSame(AutoBrain.Job.COLLECT, AutoBrain.next(0, 24, false, false, 7));
    }

    @Test
    public void runningOutOfTime() {
        assertSame(AutoBrain.Job.SHOOT, AutoBrain.next(1, 4, true, true, 7));   // shoot what we have
        assertSame(AutoBrain.Job.PARK, AutoBrain.next(0, 4, true, true, 7));
        assertSame(AutoBrain.Job.SHOOT, AutoBrain.next(2, 7, true, true, 7));   // too late to collect, shoot
        assertSame(AutoBrain.Job.COLLECT, AutoBrain.next(0, 10, true, false, 7)); // too late for the garden
    }

    // ------------------------------------------------------------ routes

    @Test
    public void clearRoutesGoStraight() {
        List<FieldPoint> r = Routes.route(new FieldPoint(10, 10), new FieldPoint(10, 130));
        assertEquals(2, r.size());
    }

    @Test
    public void routesAroundTheFrameNeverCrossIt() {
        Random rnd = new Random(3);
        int detours = 0;
        for (int i = 0; i < 500; i++) {
            FieldPoint a = outside(rnd), b = outside(rnd);
            List<FieldPoint> r = Routes.route(a, b);
            if (r.size() > 2) detours++;
            for (int k = 1; k < r.size(); k++) {
                assertFalse("leg " + k + " of " + a + " -> " + b, Routes.crosses(r.get(k - 1), r.get(k)));
            }
        }
        assertTrue("some routes needed a detour", detours > 50);
    }

    @Test
    public void straightThroughTheMiddleNeedsADetour() {
        assertTrue(Routes.crosses(new FieldPoint(10, 72), new FieldPoint(134, 72)));
        assertTrue(Routes.route(new FieldPoint(10, 72), new FieldPoint(134, 72)).size() > 2);
    }

    private static FieldPoint outside(Random rnd) {
        while (true) {
            FieldPoint p = new FieldPoint(10 + 124 * rnd.nextDouble(), 10 + 124 * rnd.nextDouble());
            if (!Routes.KEEP_OUT.contains(p.x, p.y)) return p;
        }
    }
}
