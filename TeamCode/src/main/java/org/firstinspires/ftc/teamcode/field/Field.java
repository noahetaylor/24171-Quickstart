package org.firstinspires.ftc.teamcode.field;

/**
 * Where things are on the BIOBUZZ field, in Pedro coordinates (inches).
 *
 * Frame (our convention; keep every OpMode on it):
 *   - Seen from the AUDIENCE, origin is the near-left corner, where the
 *     audience wall meets the RED wall.
 *   - +X runs toward the BLUE wall (tile columns A..F),
 *     +Y runs toward the far wall ("Scoring" side; tile rows 1..6).
 *   - Heading 0 faces +X; counter-clockwise is positive.
 *   - Field center is (72, 72). Tile seams every 24 in.
 *
 * Sources: Competition Manual section 9 and the Event Field Setup Guide V1.0.
 * Numbers marked ESTIMATE were read off figures, not given in the text. Check
 * them against the Onshape field CAD (or a tape measure on a real field)
 * before trusting a shot that depends on them.
 */
public final class Field {
    private Field() {}

    public static final double SIZE_IN = 144.0;
    public static final double TILE_IN = 24.0;
    public static final double CENTER_IN = SIZE_IN / 2;

    /** Tile seam n (0..6) from the origin, in either axis. */
    public static double seam(int n) {
        return n * TILE_IN;
    }

    // ---------------------------------------------------------------- HIVE
    // One frame at center holding two HIVEs side by side along X: RED on the
    // red-wall side (column C), BLUE on the blue-wall side (column D). Each
    // HIVE pivots about an X-direction axis and tips between its AUDIENCE
    // cell (low Y) and SCORING cell (high Y).

    public static final double HIVE_PIVOT_HEIGHT_IN = 43.95;   // manual 9.6.1
    public static final double CELL_SPACING_IN = 18.8;         // manual 9.6.2, between the two CELLs of a HIVE
    public static final double CELL_OPENING_WIDTH_IN = 20.0;   // manual 9.6.2
    public static final double CELL_OPENING_HEIGHT_IN = 14.0;
    public static final double CELL_DEPTH_IN = 12.0;

    /** ESTIMATE: each HIVE's center is this far from field center in X (setup guide figures). */
    public static final double HIVE_X_OFFSET_IN = 12.0;

    /**
     * ESTIMATE: height of an UP cell's opening center. The pivot height is a
     * placeholder until it is measured in the field CAD.
     */
    public static final double UP_CELL_OPENING_HEIGHT_IN = HIVE_PIVOT_HEIGHT_IN;

    public static final double RED_HIVE_X = CENTER_IN - HIVE_X_OFFSET_IN;
    public static final double BLUE_HIVE_X = CENTER_IN + HIVE_X_OFFSET_IN;

    // -------------------------------------------------------------- FLOWERS
    // Four, on the perimeter walls at tile seams (setup guide 10.4).

    /** ESTIMATE: wall to FLOWER center. */
    public static final double FLOWER_WALL_INSET_IN = 3.0;
    public static final double FLOWER_TOP_HEIGHT_IN = 21.5;     // manual 9.7, top opening
    public static final double FLOWER_TOP_DIAMETER_IN = 4.0;

    public enum Flower {
        /** On the far wall, left of center. */
        FAR(seam(2), SIZE_IN - FLOWER_WALL_INSET_IN),
        /** On the audience wall, right of center. */
        AUDIENCE(seam(4), FLOWER_WALL_INSET_IN),
        /** On the red alliance wall, audience side of center. */
        RED_WALL(FLOWER_WALL_INSET_IN, seam(2)),
        /** On the blue alliance wall, far side of center. */
        BLUE_WALL(SIZE_IN - FLOWER_WALL_INSET_IN, seam(4));

        public final FieldPoint top;

        Flower(double x, double y) {
            this.top = new FieldPoint(x, y, FLOWER_TOP_HEIGHT_IN);
        }
    }

    // ---------------------------------------------------------------- ZONES

    public static final double LOADING_ZONE_DEPTH_IN = 11.0;   // out from the wall
    public static final double GARDEN_DEPTH_IN = 2.0;

    /** RED loading zone: tile A5, against the red wall. */
    public static final FieldRect RED_LOADING_ZONE =
            new FieldRect(0, seam(4), LOADING_ZONE_DEPTH_IN, seam(5));
    /** BLUE loading zone: tile F2, against the blue wall. */
    public static final FieldRect BLUE_LOADING_ZONE = RED_LOADING_ZONE.rotated180();

    /** RED garden: tile A1, along the audience wall. */
    public static final FieldRect RED_GARDEN = new FieldRect(0, 0, seam(1), GARDEN_DEPTH_IN);
    /** BLUE garden: tile F6, along the far wall. */
    public static final FieldRect BLUE_GARDEN = RED_GARDEN.rotated180();

    public static FieldRect loadingZone(Alliance a) {
        return a == Alliance.RED ? RED_LOADING_ZONE : BLUE_LOADING_ZONE;
    }

    public static FieldRect garden(Alliance a) {
        return a == Alliance.RED ? RED_GARDEN : BLUE_GARDEN;
    }

    // --------------------------------------------------------------- helpers

    /** Wraps an angle into (-pi, pi]. */
    public static double normalizeRadians(double a) {
        double r = a % (2 * Math.PI);
        if (r <= -Math.PI) r += 2 * Math.PI;
        if (r > Math.PI) r -= 2 * Math.PI;
        return r;
    }
}
