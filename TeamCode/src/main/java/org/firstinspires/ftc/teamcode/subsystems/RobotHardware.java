package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.config.HardwareConfig;

/**
 * Shared hardware class. Owns references to physical devices and does the
 * one-time hardwareMap lookups. Subsystems (like Drivetrain) take this in
 * their constructor instead of touching hardwareMap directly.
 *
 * Device names and directions come from HardwareConfig, which Pedro's
 * Constants also reads — so TeleOp and autonomous always agree.
 *
 * As mechanisms get added this fall, their motors/servos get declared and
 * initialized here too, following the same pattern as the drive motors below.
 */
public class RobotHardware {

    public DcMotor frontLeft, frontRight, backLeft, backRight;

    public void init(HardwareMap hardwareMap) {
        frontLeft = hardwareMap.get(DcMotor.class, HardwareConfig.LEFT_FRONT);
        frontRight = hardwareMap.get(DcMotor.class, HardwareConfig.RIGHT_FRONT);
        backLeft = hardwareMap.get(DcMotor.class, HardwareConfig.LEFT_REAR);
        backRight = hardwareMap.get(DcMotor.class, HardwareConfig.RIGHT_REAR);

        frontLeft.setDirection(HardwareConfig.LEFT_FRONT_DIR);
        frontRight.setDirection(HardwareConfig.RIGHT_FRONT_DIR);
        backLeft.setDirection(HardwareConfig.LEFT_REAR_DIR);
        backRight.setDirection(HardwareConfig.RIGHT_REAR_DIR);

        for (DcMotor motor : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            // The odometry pods share encoder ports with backLeft/backRight,
            // so the drive must never close a loop on "its" encoder — it
            // would be reading a pod. Plain power control only.
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            // BRAKE means motors resist being pushed when at zero power —
            // generally what you want for a drivetrain so the robot doesn't
            // coast/drift after releasing the sticks.
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
    }
}
