package org.firstinspires.ftc.teamcode.field;

/**
 * Which CELL is up on each HIVE.
 *
 * Partners' and opponents' shots tip HIVEs too, so this must be kept up to
 * date from what the camera sees (observeUp), not from counting our own
 * shots. tipped() is for when we are confident a tip happened but have not
 * seen the tags yet.
 *
 * Match start (setup guide 11.1): RED's AUDIENCE cell up, BLUE's SCORING cell
 * up, each holding 3 NECTAR. Each CELL is calibrated to tip at 3 NECTAR +
 * 3 POLLEN or at 8 POLLEN (setup guide 12), so a starting CELL tips on its
 * 3rd POLLEN.
 */
public final class HiveState {

    private Cell redUp;
    private Cell blueUp;

    /** Starts in the match-start state. */
    public HiveState() {
        reset();
    }

    public void reset() {
        redUp = Cell.RED_AUDIENCE;
        blueUp = Cell.BLUE_SCORING;
    }

    public Cell upCell(Alliance a) {
        return a == Alliance.RED ? redUp : blueUp;
    }

    public boolean isUp(Cell c) {
        return upCell(c.alliance) == c;
    }

    /** The camera saw this CELL's tags facing down, so it is the up CELL. */
    public void observeUp(Cell c) {
        if (c.alliance == Alliance.RED) redUp = c;
        else blueUp = c;
    }

    /**
     * Same as observeUp for a single tag ID. Returns false (and changes
     * nothing) if the ID is not a CELL tag.
     */
    public boolean observeTag(int tagId) {
        Cell c = Cell.forTagId(tagId);
        if (c == null) return false;
        observeUp(c);
        return true;
    }

    /** That alliance's HIVE flipped. */
    public void tipped(Alliance a) {
        observeUp(upCell(a).partner());
    }

    /** Where to shoot for this alliance's HIVE right now. */
    public FieldPoint target(Alliance a) {
        return upCell(a).openingWhenUp();
    }
}
