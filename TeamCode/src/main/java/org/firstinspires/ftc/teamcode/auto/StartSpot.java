package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.config.RobotSize;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldRect;

/**
 * Where Auto can start, written for RED (robot center, heading). Chosen on
 * the Driver Hub in INIT. Each is checked against G304 by isLegal() (unit
 * tested): on our side, touching a wall, not in the LOADING ZONE, not
 * touching a FLOWER, and not on the GARDEN's POLLEN.
 *
 * PLACEHOLDERS until the team picks real spots.
 */
public enum StartSpot {
    /** Back against the audience wall, tile B1, facing the far wall. */
    AUDIENCE_WALL(36, RobotSize.HALF_LENGTH_IN, 90),
    /** Back against our alliance wall, on the center line, facing the blue wall. */
    ALLIANCE_WALL(RobotSize.HALF_LENGTH_IN, 72, 0),
    /** Back against the far wall, tile B6, facing the audience. */
    FAR_WALL(30, Field.SIZE_IN - RobotSize.HALF_LENGTH_IN, -90);

    /** FLOWER base ring radius plus a hair: the robot must stay this far from a FLOWER's center. */
    public static final double FLOWER_CLEAR_IN = 4.0;

    public final double redX, redY, redHeadingDeg;

    StartSpot(double redX, double redY, double redHeadingDeg) {
        this.redX = redX;
        this.redY = redY;
        this.redHeadingDeg = redHeadingDeg;
    }

    public double x(Alliance a) {
        return a.xFromRed(redX);
    }

    public double y(Alliance a) {
        return a.yFromRed(redY);
    }

    public double headingRad(Alliance a) {
        return a.headingFromRed(Math.toRadians(redHeadingDeg));
    }

    /** Robot footprint (axis-aligned; start headings are multiples of 90). */
    public static FieldRect footprint(double x, double y, double headingDeg) {
        boolean alongX = Math.abs(Math.cos(Math.toRadians(headingDeg))) > 0.5;
        double hx = alongX ? RobotSize.HALF_LENGTH_IN : RobotSize.HALF_WIDTH_IN;
        double hy = alongX ? RobotSize.HALF_WIDTH_IN : RobotSize.HALF_LENGTH_IN;
        return new FieldRect(x - hx, y - hy, x + hx, y + hy);
    }

    /** G304 checks for a RED start pose. Returns null if legal, else the reason. */
    public static String whyIllegal(double x, double y, double headingDeg) {
        FieldRect r = footprint(x, y, headingDeg);
        double eps = 0.25;
        if (r.minX < -eps || r.minY < -eps || r.maxY > Field.SIZE_IN + eps) return "outside the field";
        if (r.maxX > Field.CENTER_IN + eps) return "not fully on our side (columns A-C)";
        boolean touching = r.minX <= eps || r.minY <= eps || r.maxY >= Field.SIZE_IN - eps;
        if (!touching) return "not touching a wall";
        if (overlaps(r, Field.RED_LOADING_ZONE)) return "in the LOADING ZONE";
        FieldRect gardenBalls = new FieldRect(0, 0, Field.TILE_IN, 2.8);
        if (overlaps(r, gardenBalls)) return "on the GARDEN's POLLEN";
        for (Field.Flower f : Field.Flower.values()) {
            double cx = Math.max(r.minX, Math.min(f.top.x, r.maxX));
            double cy = Math.max(r.minY, Math.min(f.top.y, r.maxY));
            if (Math.hypot(f.top.x - cx, f.top.y - cy) < FLOWER_CLEAR_IN) return "touching the " + f + " FLOWER";
        }
        return null;
    }

    public String whyIllegal() {
        return whyIllegal(redX, redY, redHeadingDeg);
    }

    private static boolean overlaps(FieldRect a, FieldRect b) {
        return a.minX < b.maxX && a.maxX > b.minX && a.minY < b.maxY && a.maxY > b.minY;
    }
}
