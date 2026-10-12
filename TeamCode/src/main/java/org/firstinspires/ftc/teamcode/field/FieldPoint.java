package org.firstinspires.ftc.teamcode.field;

/**
 * A point on the field in Pedro coordinates (inches). Z is height above the
 * tiles. Plain data holder, no Android/FTC/Pedro dependencies, so it can be
 * tested as ordinary Java.
 */
public final class FieldPoint {

    public final double x;
    public final double y;
    public final double z;

    public FieldPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public FieldPoint(double x, double y) {
        this(x, y, 0);
    }

    /** The same spot on the other alliance's half: rotated 180 degrees about field center. */
    public FieldPoint rotated180() {
        return new FieldPoint(Field.SIZE_IN - x, Field.SIZE_IN - y, z);
    }

    /** Floor distance (ignores height). */
    public double distanceTo(double otherX, double otherY) {
        return Math.hypot(otherX - x, otherY - y);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}
