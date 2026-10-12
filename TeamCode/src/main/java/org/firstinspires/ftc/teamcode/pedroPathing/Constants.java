package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.TwoWheelConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.config.HardwareConfig;

/**
 * Pedro configuration. Names, directions, and measurements come from
 * HardwareConfig; values that Pedro's tuners produce get recorded here.
 */
public class Constants {
    public static FollowerConstants followerConstants = new FollowerConstants();

    // xVelocity / yVelocity are Pedro's defaults until the Forward/Lateral
    // Velocity Tuners are run on our drivetrain.
    public static MecanumConstants driveConstants = new MecanumConstants()
            .maxPower(1)
            .leftFrontMotorName(HardwareConfig.LEFT_FRONT)
            .leftRearMotorName(HardwareConfig.LEFT_REAR)
            .rightFrontMotorName(HardwareConfig.RIGHT_FRONT)
            .rightRearMotorName(HardwareConfig.RIGHT_REAR)
            .leftFrontMotorDirection(HardwareConfig.LEFT_FRONT_DIR)
            .leftRearMotorDirection(HardwareConfig.LEFT_REAR_DIR)
            .rightFrontMotorDirection(HardwareConfig.RIGHT_FRONT_DIR)
            .rightRearMotorDirection(HardwareConfig.RIGHT_REAR_DIR);

    public static TwoWheelConstants localizerConstants = new TwoWheelConstants()
            .forwardEncoder_HardwareMapName(HardwareConfig.FORWARD_POD)
            .strafeEncoder_HardwareMapName(HardwareConfig.STRAFE_POD)
            .forwardEncoderDirection(HardwareConfig.FORWARD_POD_DIR)
            .strafeEncoderDirection(HardwareConfig.STRAFE_POD_DIR)
            .forwardTicksToInches(HardwareConfig.POD_INCHES_PER_TICK)
            .strafeTicksToInches(HardwareConfig.POD_INCHES_PER_TICK)
            .forwardPodY(HardwareConfig.FORWARD_POD_Y)
            .strafePodX(HardwareConfig.STRAFE_POD_X)
            .IMU_HardwareMapName(HardwareConfig.IMU)
            .IMU_Orientation(new RevHubOrientationOnRobot(HardwareConfig.HUB_LOGO, HardwareConfig.HUB_USB));

    public static PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .pathConstraints(pathConstraints)
                .mecanumDrivetrain(driveConstants)
                .twoWheelLocalizer(localizerConstants)
                .build();
    }
}
