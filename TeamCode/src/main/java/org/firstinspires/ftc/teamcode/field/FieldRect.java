package org.firstinspires.ftc.teamcode.field;

/** An axis-aligned rectangle on the floor, Pedro coordinates (inches). */
public final class FieldRect {

    public final double minX;
    public final double minY;
    public final double maxX;
    public final double maxY;

    public FieldRect(double x1, double y1, double x2, double y2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public FieldPoint center() {
        return new FieldPoint((minX + maxX) / 2, (minY + maxY) / 2);
    }

    /** The matching rectangle on the other alliance's half. */
    public FieldRect rotated180() {
        return new FieldRect(Field.SIZE_IN - maxX, Field.SIZE_IN - maxY,
                Field.SIZE_IN - minX, Field.SIZE_IN - minY);
    }

    @Override
    public String toString() {
        return String.format("[%.1f..%.1f, %.1f..%.1f]", minX, maxX, minY, maxY);
    }
}
