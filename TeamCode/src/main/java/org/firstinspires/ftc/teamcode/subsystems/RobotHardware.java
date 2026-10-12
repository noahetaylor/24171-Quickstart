package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.config.HardwareConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared hardware class. Owns references to physical devices and does the
 * one-time hardwareMap lookups. Subsystems take this in their constructor
 * instead of touching hardwareMap directly.
 *
 * Device names and directions come from HardwareConfig, which Pedro's
 * Constants also reads, so TeleOp and autonomous always agree.
 *
 * Drive motors are required. Mechanisms are OPTIONAL: if one isn't in the
 * Driver Hub configuration it stays null, its subsystem does nothing, and
 * its name is listed in {@link #missing} for telemetry. That lets a
 * half-built robot run.
 */
public class RobotHardware {

    public DcMotor frontLeft, frontRight, backLeft, backRight;

    public DcMotorEx pollenFlywheel, nectarFlywheel, intake, transfer;
    public CRServo crossWheels;
    public Servo rampBar;

    /** Mechanism names not found in the robot configuration. */
    public final List<String> missing = new ArrayList<>();

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

        initMechanisms(hardwareMap);
    }

    /**
     * Mechanisms only, for code that leaves the drive motors to Pedro's
     * Follower (competition OpModes).
     */
    public void initMechanisms(HardwareMap hardwareMap) {
        missing.clear();

        pollenFlywheel = motor(hardwareMap, HardwareConfig.POLLEN_FLYWHEEL);
        nectarFlywheel = motor(hardwareMap, HardwareConfig.NECTAR_FLYWHEEL);
        intake = motor(hardwareMap, HardwareConfig.INTAKE);
        transfer = motor(hardwareMap, HardwareConfig.TRANSFER);

        if (pollenFlywheel != null) setUpFlywheel(pollenFlywheel, HardwareConfig.POLLEN_FLYWHEEL_DIR);
        if (nectarFlywheel != null) setUpFlywheel(nectarFlywheel, HardwareConfig.NECTAR_FLYWHEEL_DIR);
        if (intake != null) {
            intake.setDirection(HardwareConfig.INTAKE_DIR);
            intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        }
        if (transfer != null) {
            transfer.setDirection(HardwareConfig.TRANSFER_DIR);
            transfer.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            // BRAKE: a stopped transfer is what holds the balls in the trough.
            transfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        crossWheels = hardwareMap.tryGet(CRServo.class, HardwareConfig.CROSS_WHEELS);
        if (crossWheels == null) missing.add(HardwareConfig.CROSS_WHEELS);
        else crossWheels.setDirection(HardwareConfig.CROSS_WHEELS_DIR);

        rampBar = hardwareMap.tryGet(Servo.class, HardwareConfig.RAMP_BAR);
        if (rampBar == null) missing.add(HardwareConfig.RAMP_BAR);
    }

    private DcMotorEx motor(HardwareMap hardwareMap, String name) {
        DcMotorEx m = hardwareMap.tryGet(DcMotorEx.class, name);
        if (m == null) missing.add(name);
        return m;
    }

    private static void setUpFlywheel(DcMotorEx m, DcMotorSimple.Direction dir) {
        m.setDirection(dir);
        m.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        // FLOAT: let a spinning flywheel coast down instead of braking hard.
        m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }
}
