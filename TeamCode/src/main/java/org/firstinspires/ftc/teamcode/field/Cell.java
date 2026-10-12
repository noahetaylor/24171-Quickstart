package org.firstinspires.ftc.teamcode.field;

/**
 * The four CELLs. Each has a cluster of four AprilTags (36h11) on its
 * underside; the SDK 12.0 tag library knows each cluster, with its origin at
 * the center of the CELL opening.
 */
public enum Cell {
    RED_AUDIENCE(Alliance.RED, Side.AUDIENCE, 34),
    RED_SCORING(Alliance.RED, Side.SCORING, 30),
    BLUE_AUDIENCE(Alliance.BLUE, Side.AUDIENCE, 38),
    BLUE_SCORING(Alliance.BLUE, Side.SCORING, 42);

    /** Which end of the HIVE: AUDIENCE is the low-Y end, SCORING the high-Y end. */
    public enum Side { AUDIENCE, SCORING }

    public static final int TAGS_PER_CELL = 4;

    public final Alliance alliance;
    public final Side side;
    /** Tag IDs are firstTagId .. firstTagId + 3. */
    public final int firstTagId;

    Cell(Alliance alliance, Side side, int firstTagId) {
        this.alliance = alliance;
        this.side = side;
        this.firstTagId = firstTagId;
    }

    /** The other CELL on the same HIVE. */
    public Cell partner() {
        switch (this) {
            case RED_AUDIENCE: return RED_SCORING;
            case RED_SCORING: return RED_AUDIENCE;
            case BLUE_AUDIENCE: return BLUE_SCORING;
            default: return BLUE_AUDIENCE;
        }
    }

    public boolean hasTag(int id) {
        return id >= firstTagId && id < firstTagId + TAGS_PER_CELL;
    }

    /** The CELL carrying this tag, or null if the ID is not a BIOBUZZ CELL tag. */
    public static Cell forTagId(int id) {
        for (Cell c : values()) {
            if (c.hasTag(id)) return c;
        }
        return null;
    }

    /**
     * Center of this CELL's opening while it is the UP cell: the shooting
     * target. X and height are measured; Y is ESTIMATED as half the 18.8 in
     * CELL spacing either side of center, since a tipped-up CELL likely
     * sits somewhat further out.
     */
    public FieldPoint openingWhenUp() {
        double x = alliance == Alliance.RED ? Field.RED_HIVE_X : Field.BLUE_HIVE_X;
        double halfSpacing = Field.CELL_SPACING_IN / 2;
        double y = side == Side.AUDIENCE ? Field.CENTER_IN - halfSpacing : Field.CENTER_IN + halfSpacing;
        return new FieldPoint(x, y, Field.UP_CELL_OPENING_HEIGHT_IN);
    }
}
