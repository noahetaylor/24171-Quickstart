package org.firstinspires.ftc.teamcode.config;

/**
 * Where the Limelight sits and how its pan/tilt servos map to angles.
 *
 * Position: ROUGH, from the team CAD (front bar, pan axis 72 mm forward of
 * center, lens ~380 mm up). Measure on the robot: robot center to the lens,
 * inches. Robot frame: FORWARD toward the intake, LEFT, HEIGHT above tiles.
 *
 * Servo calibration: PLACEHOLDERS. In Limelight Check, find the servo
 * position that points the camera straight ahead (pan) and level (tilt),
 * then how far the position moves per degree.
 */
public final class LimelightMount {
    private LimelightMount() {}

    public static final double LENS_FORWARD_IN = 2.8;
    public static final double LENS_LEFT_IN = 0.0;
    public static final double LENS_HEIGHT_IN = 15.0;

    /** Pan: servo position when the camera looks straight forward; change per degree (CCW +). */
    public static final double PAN_CENTER_POS = 0.5;
    public static final double PAN_POS_PER_DEG = 1.0 / 270.0;
    public static final double PAN_MIN_DEG = -90, PAN_MAX_DEG = 90;

    /** Tilt: servo position when the camera is level; change per degree (up +). */
    public static final double TILT_LEVEL_POS = 0.5;
    public static final double TILT_POS_PER_DEG = 1.0 / 270.0;
    public static final double TILT_MIN_DEG = -60, TILT_MAX_DEG = 45;

    /** Limelight pipeline slots: 0 = ball finder (Python), 1 = AprilTags (36h11). */
    public static final int BALL_PIPELINE = 0;
    public static final int TAG_PIPELINE = 1;
}
