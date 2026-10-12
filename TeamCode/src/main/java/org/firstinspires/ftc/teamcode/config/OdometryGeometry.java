// Written by the Pod Placer tool (tools/pod-placer). Prefer editing it there.
// Hand edits are fine as long as each value keeps its "NAME = value;" line.
package org.firstinspires.ftc.teamcode.config;

/**
 * Where the odometry pods physically are. Inches from robot center:
 * FORWARD is toward the front (negative = behind), LEFT is toward the
 * robot's left (negative = right). ARROW is the direction the ROBOT moves
 * to make that pod's raw count go UP: 0 front, 90 left, 180 back, 270 right.
 *
 * Physical facts only. OdometryPods converts these to Pedro's values.
 */
public final class OdometryGeometry {
    private OdometryGeometry() {}

    public static final double CHASSIS_WIDTH_IN = 14.0625;
    public static final double CHASSIS_LENGTH_IN = 17.0;

    // Pod plugged into Control Hub encoder port 0
    public static final double PORT0_POD_FORWARD_IN = -7.5;
    public static final double PORT0_POD_LEFT_IN = 5.25;
    public static final int PORT0_POD_ARROW_DEG = 90;

    // Pod plugged into Control Hub encoder port 3
    public static final double PORT3_POD_FORWARD_IN = -7.5;
    public static final double PORT3_POD_LEFT_IN = -4.25;
    public static final int PORT3_POD_ARROW_DEG = 0;
}
