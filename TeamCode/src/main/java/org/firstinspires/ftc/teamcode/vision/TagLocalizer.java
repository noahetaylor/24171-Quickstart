package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.field.CellTags;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.tuning.VisionTuning;

import java.util.List;

/**
 * Robot position from CELL tags. The Limelight reports each tag's position
 * in camera space (x right, y down, z out of the lens); with the gimbal
 * angles and the robot heading (gyro, trusted) that's the vector from the
 * lens to the tag on the field. The tag's field position is known while
 * its CELL is up (CellTags), so the lens position, and from it the robot
 * position, follows.
 *
 * Several tags: each gives an estimate; if they disagree by more than
 * MAX_TAG_SPREAD_IN (wrong tag order, a misread, the CELL not where we
 * think) the frame is rejected. The tags' measured height is checked too.
 *
 * Pure logic, unit-tested in TagLocalizerTest.
 */
public final class TagLocalizer {
    private TagLocalizer() {}

    /** One tag in camera space (inches, Limelight axes). */
    public static final class TagSeen {
        public final int id;
        public final double camX, camY, camZ;

        public TagSeen(int id, double camX, double camY, double camZ) {
            this.id = id;
            this.camX = camX;
            this.camY = camY;
            this.camZ = camZ;
        }
    }

    public static final class Fix {
        public final double x, y;
        public final int tags;
        /** Floor distance to the nearest tag used. */
        public final double rangeIn;

        Fix(double x, double y, int tags, double rangeIn) {
            this.x = x;
            this.y = y;
            this.tags = tags;
            this.rangeIn = rangeIn;
        }
    }

    /** Robot position from one tag, or null if it's not a CELL tag or the height is way off. */
    public static double[] fromTag(TagSeen t, double panDeg, double tiltDeg, double headingRad) {
        FieldPoint tag = CellTags.position(t.id);
        if (tag == null) return null;
        double f = t.camZ, l = -t.camX, u = -t.camY;
        double tr = Math.toRadians(tiltDeg);
        double f1 = f * Math.cos(tr) - u * Math.sin(tr);
        double u1 = f * Math.sin(tr) + u * Math.cos(tr);
        if (Math.abs(LimelightMount.LENS_HEIGHT_IN + u1 - tag.z) > VisionTuning.MAX_TAG_HEIGHT_ERR_IN) return null;
        double yaw = Math.toRadians(panDeg) + headingRad;
        double dx = f1 * Math.cos(yaw) - l * Math.sin(yaw);
        double dy = f1 * Math.sin(yaw) + l * Math.cos(yaw);
        double lensX = tag.x - dx, lensY = tag.y - dy;
        double c = Math.cos(headingRad), s = Math.sin(headingRad);
        return new double[]{
                lensX - (LimelightMount.LENS_FORWARD_IN * c - LimelightMount.LENS_LEFT_IN * s),
                lensY - (LimelightMount.LENS_FORWARD_IN * s + LimelightMount.LENS_LEFT_IN * c),
                Math.hypot(dx, dy)};
    }

    /** Combined fix from all tags in a frame, or null if none usable or they disagree. */
    public static Fix locate(List<TagSeen> tags, double panDeg, double tiltDeg, double headingRad) {
        double sx = 0, sy = 0, minRange = Double.MAX_VALUE;
        int n = 0;
        double[][] each = new double[tags.size()][];
        for (TagSeen t : tags) {
            double[] p = fromTag(t, panDeg, tiltDeg, headingRad);
            if (p == null) continue;
            each[n++] = p;
            sx += p[0];
            sy += p[1];
            minRange = Math.min(minRange, p[2]);
        }
        if (n == 0) return null;
        double mx = sx / n, my = sy / n;
        for (int i = 0; i < n; i++) {
            if (Math.hypot(each[i][0] - mx, each[i][1] - my) > VisionTuning.MAX_TAG_SPREAD_IN) return null;
        }
        return new Fix(mx, my, n, minRange);
    }

    /**
     * Blend a fix into the odometry position, or return null to leave it:
     * only when nearly still, close enough, and not a wild jump.
     */
    public static double[] correct(double odoX, double odoY, Fix fix, double speedInPerS, double turnDegPerS) {
        if (fix == null) return null;
        if (speedInPerS > VisionTuning.TAG_MAX_SPEED_IN_S || Math.abs(turnDegPerS) > VisionTuning.TAG_MAX_TURN_DEG_S) return null;
        if (fix.rangeIn > VisionTuning.TAG_MAX_RANGE_IN) return null;
        double jump = Math.hypot(fix.x - odoX, fix.y - odoY);
        if (jump > VisionTuning.TAG_MAX_JUMP_IN) return null;
        double g = VisionTuning.TAG_GAIN;
        return new double[]{odoX + g * (fix.x - odoX), odoY + g * (fix.y - odoY)};
    }
}
