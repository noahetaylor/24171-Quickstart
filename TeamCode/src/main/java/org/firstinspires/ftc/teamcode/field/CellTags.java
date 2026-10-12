package org.firstinspires.ftc.teamcode.field;

/**
 * Where each CELL AprilTag is on the field while its CELL is UP (the tags
 * move when the HIVE tips, but each stable state puts them in the same
 * place). Written for RED; BLUE is RED rotated 180 degrees.
 *
 * - Along X: the SDK 12.0 cluster layout, tags at -6.5, -2.75, +2.75,
 *   +6.5 in from the cluster center (3.25 in tags).
 * - Order: from the setup guide's tag figure (section 9.3). AUDIENCE cells
 *   count up toward +X; SCORING cells count up toward -X. VERIFY on a real
 *   field (Limelight Check, tag mode): if the order is wrong, the per-tag
 *   positions disagree and TagLocalizer rejects them, so corrections stop
 *   instead of going wrong.
 * - Y and height: ESTIMATES (row under the CELL opening, ~45 in up).
 *   Measure on a real HIVE with the CELL up.
 */
public final class CellTags {
    private CellTags() {}

    public static final double TAG_SIZE_IN = 3.25;
    static final double[] ALONG_ROW_IN = {-6.5, -2.75, 2.75, 6.5};

    /** ESTIMATE: height of the tag centers under an up CELL. */
    public static double TAG_HEIGHT_IN = 45.0;
    /** ESTIMATE: tag row's Y offset from the up CELL opening's Y (+ = toward the far wall, RED audience cell). */
    public static double TAG_ROW_Y_OFFSET_IN = 0.0;

    /** Field position of a CELL tag while its CELL is up, or null if not a CELL tag. */
    public static FieldPoint position(int tagId) {
        Cell c = Cell.forTagId(tagId);
        if (c == null) return null;
        int k = tagId - c.firstTagId;
        // Work on the RED cell with the same layout, then rotate for BLUE.
        Cell red = c.alliance == Alliance.RED ? c : redTwin(c);
        FieldPoint redPt = redPosition(red, k);
        return c.alliance == Alliance.RED ? redPt : redPt.rotated180();
    }

    /** The RED cell that BLUE's cell is the 180-degree rotation of. */
    static Cell redTwin(Cell blue) {
        return blue == Cell.BLUE_SCORING ? Cell.RED_AUDIENCE : Cell.RED_SCORING;
    }

    private static FieldPoint redPosition(Cell red, int k) {
        FieldPoint open = red.openingWhenUp();
        boolean audience = red.side == Cell.Side.AUDIENCE;
        double along = audience ? ALONG_ROW_IN[k] : -ALONG_ROW_IN[k];
        double yOff = audience ? TAG_ROW_Y_OFFSET_IN : -TAG_ROW_Y_OFFSET_IN;
        return new FieldPoint(open.x + along, open.y + yOff, TAG_HEIGHT_IN);
    }
}
