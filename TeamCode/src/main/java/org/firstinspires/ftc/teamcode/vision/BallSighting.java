package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.field.Alliance;

/** One ball seen by the Limelight, located on the field (Pedro inches). */
public final class BallSighting {

    public enum Kind {
        POLLEN(1, 2.8 / 2),
        RED_NECTAR(2, 3.6 / 2),
        BLUE_NECTAR(3, 3.6 / 2);

        /** Class number in the Limelight pipeline's output. */
        public final int code;
        public final double radiusIn;

        Kind(int code, double radiusIn) {
            this.code = code;
            this.radiusIn = radiusIn;
        }

        public static Kind fromCode(int code) {
            for (Kind k : values()) if (k.code == code) return k;
            return null;
        }

        /** POLLEN is anyone's; NECTAR only of our own color. */
        public boolean wantedBy(Alliance a) {
            return this == POLLEN
                    || (this == RED_NECTAR && a == Alliance.RED)
                    || (this == BLUE_NECTAR && a == Alliance.BLUE);
        }
    }

    public final Kind kind;
    public final double x, y;
    /** Floor distance from the camera, inches. */
    public final double rangeIn;

    public BallSighting(Kind kind, double x, double y, double rangeIn) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.rangeIn = rangeIn;
    }

    @Override
    public String toString() {
        return String.format("%s at (%.1f, %.1f) %.0f in away", kind, x, y, rangeIn);
    }
}
