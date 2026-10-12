package org.firstinspires.ftc.teamcode.field;

/**
 * Which alliance we are on, and how to turn a RED-authored position into
 * ours.
 *
 * The BIOBUZZ field is 180-degree rotationally symmetric, NOT mirrored: the
 * blue version of any red spot is the red one turned half a revolution about
 * field center (red loading zone A5 <-> blue F2, red garden A1 <-> blue F6,
 * red HIVE's AUDIENCE cell starts up <-> blue HIVE's SCORING cell starts up).
 * So routes are written once, for RED, and converted with the methods here.
 * A rotation keeps turn directions the same (a left turn stays a left turn),
 * which a mirror would not.
 */
public enum Alliance {
    RED,
    BLUE;

    public Alliance opponent() {
        return this == RED ? BLUE : RED;
    }

    /** A point written for RED, converted to this alliance. */
    public FieldPoint fromRed(FieldPoint redPoint) {
        return this == RED ? redPoint : redPoint.rotated180();
    }

    public double xFromRed(double redX) {
        return this == RED ? redX : Field.SIZE_IN - redX;
    }

    public double yFromRed(double redY) {
        return this == RED ? redY : Field.SIZE_IN - redY;
    }

    /** A heading written for RED, converted to this alliance, in (-pi, pi]. */
    public double headingFromRed(double redHeadingRadians) {
        double h = this == RED ? redHeadingRadians : redHeadingRadians + Math.PI;
        return Field.normalizeRadians(h);
    }
}
