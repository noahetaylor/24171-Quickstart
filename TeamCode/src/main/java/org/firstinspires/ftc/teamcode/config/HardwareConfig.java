package org.firstinspires.ftc.teamcode.config;

import com.pedropathing.ftc.localization.Encoder;
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

    /** Forward pod ("X-odometer" in the measurement doc): wheel rolls front-to-back. */
    public static final String FORWARD_POD = RIGHT_REAR;   // CH encoder port 3
    /** Strafe pod ("Y-odometer" in the measurement doc): wheel rolls side-to-side. */
    public static final String STRAFE_POD = LEFT_REAR;     // CH encoder port 0

    // Pod sign: after the pod is mounted, pushing the robot forward must
    // INCREASE the forward pod and pushing it left must INCREASE the strafe
    // pod. OdometryCheck shows which way each one counts.
    public static final double FORWARD_POD_DIR = Encoder.FORWARD;
    public static final double STRAFE_POD_DIR = Encoder.FORWARD;

    /**
     * goBILDA 4-Bar pod: 2000 counts/rev on a 32 mm wheel, which goBILDA
     * lists as 19.89436789 ticks/mm. 1 / (19.89436789 * 25.4) inches/tick.
     * Starting value only — Pedro's Forward/Lateral Tuners measure the real one.
     */
    public static final double POD_INCHES_PER_TICK = 1.0 / (19.89436789 * 25.4);

    /*
     * Pod offsets from the robot's center (inches), from
     * "24171 bot odometry measurements.pdf" (2026-10):
     *   forward pod: 7.5 behind center, 4.25 RIGHT of center
     *   strafe pod:  7.5 behind center, 5.25 LEFT of center
     *
     * Only one coordinate of each pod matters to Pedro's two-wheel math:
     * the forward pod's sideways offset and the strafe pod's fore/aft
     * offset. (A forward pod's fore/aft position doesn't change what it
     * reads during a turn, and likewise for the strafe pod sideways.)
     *
     * SIGN NOTE: Pedro's two-wheel localizer computes
     *   forward = ticks - forwardPodY * dHeading
     * For that to cancel a turn, forwardPodY must be positive to the RIGHT,
     * the opposite of Pedro's left-is-+Y convention (and of the measurement
     * doc's table, which also put left as negative). Hence +4.25 for a pod
     * 4.25 in to the right. Confirm on the robot with Tuning > Localization
     * > Offsets Tuner (see docs/odometry-setup.md) before trusting it.
     */
    public static final double FORWARD_POD_Y = 4.25;
    public static final double STRAFE_POD_X = -7.5;

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
