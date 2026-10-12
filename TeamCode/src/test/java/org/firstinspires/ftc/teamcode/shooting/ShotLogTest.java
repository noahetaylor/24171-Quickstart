package org.firstinspires.ftc.teamcode.shooting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ShotLogTest {

    @Test
    public void roundTripsThroughCsv() {
        ShotLog a = new ShotLog("2026-10-20T17:03:12", Shooter.NECTAR, ShotLog.Result.HIT,
                61.25, 60, 3150, 3122.4, ShotLog.DistanceSource.TAPE, 40.04, 22.5, 87.3);
        ShotLog b = ShotLog.parse(a.toCsv());
        assertSame(Shooter.NECTAR, b.shooter);
        assertSame(ShotLog.Result.HIT, b.result);
        assertSame(ShotLog.DistanceSource.TAPE, b.source);
        assertEquals(61.3, b.distanceIn, 1e-9);   // logged to 0.1 in
        assertEquals(3122, b.rpmActual, 1e-9);    // logged to 1 rpm
        assertEquals("2026-10-20T17:03:12", b.time);
    }

    @Test
    public void headerMatchesFieldCount() {
        String row = new ShotLog("t", Shooter.POLLEN, ShotLog.Result.MISS, 1, 2, 3, 4,
                ShotLog.DistanceSource.POSE, 5, 6, 7).toCsv();
        assertEquals(ShotLog.HEADER.split(",").length, row.split(",").length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsGarbage() {
        ShotLog.parse("not,a,shot");
    }
}
