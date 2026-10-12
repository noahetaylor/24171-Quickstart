package org.firstinspires.ftc.teamcode.field;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FieldTest {

    private static final double EPS = 1e-9;

    private static void assertPoint(FieldPoint expected, FieldPoint actual) {
        assertEquals("x", expected.x, actual.x, EPS);
        assertEquals("y", expected.y, actual.y, EPS);
        assertEquals("z", expected.z, actual.z, EPS);
    }

    // ---------------------------------------------------------- alliance

    @Test
    public void redPositionsAreUnchanged() {
        FieldPoint p = new FieldPoint(10, 20, 5);
        assertPoint(p, Alliance.RED.fromRed(p));
        assertEquals(1.0, Alliance.RED.headingFromRed(1.0), EPS);
    }

    @Test
    public void blueIsRedRotatedAboutCenter() {
        FieldPoint blue = Alliance.BLUE.fromRed(new FieldPoint(10, 20, 5));
        assertPoint(new FieldPoint(134, 124, 5), blue);
        // Center maps to itself.
        assertPoint(new FieldPoint(72, 72), Alliance.BLUE.fromRed(new FieldPoint(72, 72)));
    }

    @Test
    public void blueHeadingIsTurnedHalfAround() {
        assertEquals(Math.PI, Alliance.BLUE.headingFromRed(0), EPS);
        assertEquals(-Math.PI / 2, Alliance.BLUE.headingFromRed(Math.PI / 2), EPS);
        assertEquals(0, Alliance.BLUE.headingFromRed(Math.PI), EPS);
    }

    @Test
    public void convertingTwiceGetsBackToRed() {
        FieldPoint p = new FieldPoint(33.3, 101.7, 2);
        assertPoint(p, Alliance.BLUE.fromRed(Alliance.BLUE.fromRed(p)));
    }

    // ---------------------------------------------- the field is symmetric

    @Test
    public void zonesAreRotationsOfEachOther() {
        FieldRect blueLz = Field.BLUE_LOADING_ZONE;
        assertEquals(133, blueLz.minX, EPS);
        assertEquals(144, blueLz.maxX, EPS);
        assertEquals(24, blueLz.minY, EPS);   // tile F2
        assertEquals(48, blueLz.maxY, EPS);

        FieldRect blueGarden = Field.BLUE_GARDEN;
        assertEquals(120, blueGarden.minX, EPS);  // tile F6, along the far wall
        assertEquals(142, blueGarden.minY, EPS);
    }

    @Test
    public void flowersComeInRotatedPairs() {
        assertPoint(Field.Flower.AUDIENCE.top, Field.Flower.FAR.top.rotated180());
        assertPoint(Field.Flower.BLUE_WALL.top, Field.Flower.RED_WALL.top.rotated180());
    }

    @Test
    public void cellsComeInRotatedPairs() {
        assertPoint(Cell.BLUE_SCORING.openingWhenUp(), Cell.RED_AUDIENCE.openingWhenUp().rotated180());
        assertPoint(Cell.BLUE_AUDIENCE.openingWhenUp(), Cell.RED_SCORING.openingWhenUp().rotated180());
    }

    @Test
    public void cellLayoutMatchesTheSetupGuide() {
        FieldPoint redAudience = Cell.RED_AUDIENCE.openingWhenUp();
        FieldPoint redScoring = Cell.RED_SCORING.openingWhenUp();
        assertTrue("RED HIVE is on the red-wall side", redAudience.x < Field.CENTER_IN);
        assertTrue("AUDIENCE cell is nearer the audience", redAudience.y < redScoring.y);
        assertEquals(Field.CELL_SPACING_IN, redScoring.y - redAudience.y, EPS);
    }

    // ------------------------------------------------------------- tags

    @Test
    public void tagIdsMapToTheirCells() {
        assertSame(Cell.RED_SCORING, Cell.forTagId(30));
        assertSame(Cell.RED_SCORING, Cell.forTagId(33));
        assertSame(Cell.RED_AUDIENCE, Cell.forTagId(34));
        assertSame(Cell.RED_AUDIENCE, Cell.forTagId(37));
        assertSame(Cell.BLUE_AUDIENCE, Cell.forTagId(38));
        assertSame(Cell.BLUE_AUDIENCE, Cell.forTagId(41));
        assertSame(Cell.BLUE_SCORING, Cell.forTagId(42));
        assertSame(Cell.BLUE_SCORING, Cell.forTagId(45));
        assertNull(Cell.forTagId(29));
        assertNull(Cell.forTagId(46));
    }

    @Test
    public void partnersAreOnTheSameHive() {
        for (Cell c : Cell.values()) {
            assertSame(c.alliance, c.partner().alliance);
            assertTrue(c.side != c.partner().side);
            assertSame(c, c.partner().partner());
        }
    }

    // ------------------------------------------------------- hive state

    @Test
    public void startsInTheMatchStartState() {
        HiveState s = new HiveState();
        assertSame(Cell.RED_AUDIENCE, s.upCell(Alliance.RED));
        assertSame(Cell.BLUE_SCORING, s.upCell(Alliance.BLUE));
    }

    @Test
    public void tippingFlipsOnlyThatHive() {
        HiveState s = new HiveState();
        s.tipped(Alliance.RED);
        assertSame(Cell.RED_SCORING, s.upCell(Alliance.RED));
        assertSame(Cell.BLUE_SCORING, s.upCell(Alliance.BLUE));
        s.tipped(Alliance.RED);
        assertSame(Cell.RED_AUDIENCE, s.upCell(Alliance.RED));
    }

    @Test
    public void seeingATagSetsThatCellUp() {
        HiveState s = new HiveState();
        assertTrue(s.observeTag(40));
        assertSame(Cell.BLUE_AUDIENCE, s.upCell(Alliance.BLUE));
        assertTrue(s.isUp(Cell.BLUE_AUDIENCE));
        assertFalse(s.isUp(Cell.BLUE_SCORING));
        // Seeing it again changes nothing; a non-CELL tag is ignored.
        assertTrue(s.observeTag(41));
        assertFalse(s.observeTag(7));
        assertSame(Cell.BLUE_AUDIENCE, s.upCell(Alliance.BLUE));
        assertSame(Cell.RED_AUDIENCE, s.upCell(Alliance.RED));
    }

    @Test
    public void targetFollowsTheUpCell() {
        HiveState s = new HiveState();
        assertPoint(Cell.RED_AUDIENCE.openingWhenUp(), s.target(Alliance.RED));
        s.tipped(Alliance.RED);
        assertPoint(Cell.RED_SCORING.openingWhenUp(), s.target(Alliance.RED));
    }

    @Test
    public void normalizeWrapsIntoHalfOpenRange() {
        assertEquals(Math.PI, Field.normalizeRadians(-Math.PI), EPS);
        assertEquals(0, Field.normalizeRadians(2 * Math.PI), EPS);
        assertEquals(-Math.PI / 2, Field.normalizeRadians(3 * Math.PI / 2), EPS);
    }
}
