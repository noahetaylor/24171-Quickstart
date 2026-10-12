package org.firstinspires.ftc.teamcode.config;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Single source of truth for device names, directions, and physical
 * measurements. Both RobotHardware (TeleOp) and pedroPathing/Constants
 * (Pedro's Follower) read from here, so a rename or a direction flip is a
 * one-line change instead of two that can drift apart.
 *
 * Port layout lives in docs/port-map.md — keep the two in sync.
 *
 * Coordinate convention (Pedro's): +X is forward, +Y is left, heading is
 * counter-clockwise positive.
 */
public final class HardwareConfig {
    private HardwareConfig() {}

    // ---------------------------------------------------------------
    // Drive motors (Control Hub). Names must match the Driver Hub config.
    // ---------------------------------------------------------------
    public static final String LEFT_REAR = "leftRear";     // CH motor port 0
    public static final String LEFT_FRONT = "leftFront";   // CH motor port 1
    public static final String RIGHT_FRONT = "rightFront"; // CH motor port 2
    public static final String RIGHT_REAR = "rightRear";   // CH motor port 3

    // Mirror-mounted mecanum: the left side usually needs REVERSE so that
    // positive power rolls every wheel forward. Verify with OdometryCheck's
    // motor spin test and flip here if a wheel spins backward.
    public static final DcMotorSimple.Direction LEFT_FRONT_DIR = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction LEFT_REAR_DIR = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction RIGHT_FRONT_DIR = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction RIGHT_REAR_DIR = DcMotorSimple.Direction.FORWARD;

    // ---------------------------------------------------------------
    // Odometry: two goBILDA 4-Bar pods + the Control Hub IMU for heading.
    //
    // A pod has no hardwareMap entry of its own — it plugs into a motor
    // port's ENCODER connector and is read through that motor's name. Ports
    // 0 and 3 are used because REV hubs decode those two in hardware; 1 and
    // 2 can miss counts from a 2000-count pod at full speed.
    //
    // Consequence: the motors on ports 0 and 3 (leftRear, rightRear) report
    // POD ticks, not their own. Never run drive motors in RUN_USING_ENCODER
    // or RUN_TO_POSITION — they would servo on the pods.
    // ---------------------------------------------------------------

    /** Motor port names whose encoder connectors the two pods plug into. */
    public static final String POD_PORT0 = LEFT_REAR;      // CH encoder port 0
    public static final String POD_PORT3 = RIGHT_REAR;     // CH encoder port 3

    /*
     * Pod position, rolling axis and count direction are edited VISUALLY
     * with the Pod Placer (tools/pod-placer), which writes OdometryGeometry.
     * OdometryPods works out which pod is forward vs strafe and every sign
     * Pedro needs — don't hand-compute offsets here.
     */
    public static final OdometryPods.Layout PODS = OdometryPods.resolve(
            new OdometryPods.Placement(POD_PORT0,
                    OdometryGeometry.PORT0_POD_FORWARD_IN, OdometryGeometry.PORT0_POD_LEFT_IN,
                    OdometryGeometry.PORT0_POD_ARROW_DEG),
            new OdometryPods.Placement(POD_PORT3,
                    OdometryGeometry.PORT3_POD_FORWARD_IN, OdometryGeometry.PORT3_POD_LEFT_IN,
                    OdometryGeometry.PORT3_POD_ARROW_DEG));

    /** Pod whose wheel rolls front-to-back ("X-odometer" in the measurement doc). */
    public static final String FORWARD_POD = PODS.forwardPodName;
    /** Pod whose wheel rolls side-to-side ("Y-odometer" in the measurement doc). */
    public static final String STRAFE_POD = PODS.strafePodName;
    public static final double FORWARD_POD_DIR = PODS.forwardPodDir;
    public static final double STRAFE_POD_DIR = PODS.strafePodDir;

    /** Set true ONLY while running Tuning > Offsets Tuner (it needs both offsets at 0). */
    public static final boolean ZERO_OFFSETS_FOR_OFFSETS_TUNER = false;
    public static final double FORWARD_POD_Y = ZERO_OFFSETS_FOR_OFFSETS_TUNER ? 0 : PODS.forwardPodY;
    public static final double STRAFE_POD_X = ZERO_OFFSETS_FOR_OFFSETS_TUNER ? 0 : PODS.strafePodX;

    /**
     * goBILDA 4-Bar pod: 2000 counts/rev on a 32 mm wheel, which goBILDA
     * lists as 19.89436789 ticks/mm. 1 / (19.89436789 * 25.4) inches/tick.
     * Starting value only — Pedro's Forward/Lateral Tuners measure the real one.
     */
    public static final double POD_INCHES_PER_TICK = 1.0 / (19.89436789 * 25.4);

    // ---------------------------------------------------------------
    // IMU (built into the Control Hub, always named "imu").
    // MUST match how the hub is physically mounted, or heading will be
    // wrong (a logo-DOWN hub configured as logo-UP turns backwards).
    // PLACEHOLDER until the hub's mounting is confirmed.
    // ---------------------------------------------------------------
    public static final String IMU = "imu";
    public static final RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static final RevHubOrientationOnRobot.UsbFacingDirection HUB_USB =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;
}
