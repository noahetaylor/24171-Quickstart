package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.tuning.VisionTuning;

/**
 * Turns a Limelight detection (angles in the image) into a spot on the
 * field: the camera ray is rotated by the tilt and pan servos and the
 * robot's heading, then intersected with the plane at the ball's center
 * height.
 *
 * Also checks the ball's apparent size against the size a real ball would
 * have at that range; a red tape edge or a blue bumper far away looks like
 * a "ball" at the wrong size and is rejected.
 *
 * Pure logic, unit-tested in BallLocatorTest.
 */
public final class BallLocator {
    private BallLocator() {}

    /**
     * @param txDeg, tyDeg    Limelight angles: + right, + up
     * @param angRadiusDeg    apparent radius of the blob
     * @param panDeg, tiltDeg camera pan (CCW +) and tilt (up +) relative to the robot
     * @return the sighting, or null if the ray doesn't hit the floor ahead or the size is wrong
     */
    public static BallSighting locate(BallSighting.Kind kind, double txDeg, double tyDeg, double angRadiusDeg,
                                      double panDeg, double tiltDeg,
                                      double robotX, double robotY, double robotHeadingRad) {
        // Ray in the camera frame: forward, left, up (pinhole: tan of the angles).
        double f = 1.0;
        double l = -Math.tan(Math.toRadians(txDeg));
        double u = Math.tan(Math.toRadians(tyDeg));

        // Tilt (pitch about the left axis).
        double t = Math.toRadians(tiltDeg);
        double f1 = f * Math.cos(t) - u * Math.sin(t);
        double u1 = f * Math.sin(t) + u * Math.cos(t);

        // Pan + robot heading (yaw about up).
        double yaw = Math.toRadians(panDeg) + robotHeadingRad;
        double dx = f1 * Math.cos(yaw) - l * Math.sin(yaw);
        double dy = f1 * Math.sin(yaw) + l * Math.cos(yaw);

        double drop = kind.radiusIn - LimelightMount.LENS_HEIGHT_IN;
        if (u1 >= 0 || drop >= 0) return null;   // looking at or above the horizon
        double s = drop / u1;

        // Camera position on the field.
        double c = Math.cos(robotHeadingRad), sn = Math.sin(robotHeadingRad);
        double camX = robotX + LimelightMount.LENS_FORWARD_IN * c - LimelightMount.LENS_LEFT_IN * sn;
        double camY = robotY + LimelightMount.LENS_FORWARD_IN * sn + LimelightMount.LENS_LEFT_IN * c;

        double bx = camX + s * dx;
        double by = camY + s * dy;
        double range = Math.hypot(s * dx, s * dy);
        if (range > VisionTuning.MAX_BALL_RANGE_IN) return null;

        // Size check: a real ball at this slant distance.
        double slant = Math.sqrt(range * range + drop * drop);
        double expected = Math.toDegrees(Math.atan(kind.radiusIn / slant));
        double ratio = angRadiusDeg / expected;
        if (ratio < 1 - VisionTuning.SIZE_TOLERANCE || ratio > 1 + VisionTuning.SIZE_TOLERANCE) return null;

        return new BallSighting(kind, bx, by, range);
    }
}
