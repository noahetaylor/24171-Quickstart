package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Shared hardware class. Owns references to physical devices and does the
 * one-time hardwareMap lookups. Subsystems (like Drivetrain) take this in
 * their constructor instead of touching hardwareMap directly — keeps device
 * names/config in exactly one place.
 *
 * As mechanisms get added this fall, their motors/servos get declared and
 * initialized here too, following the same pattern as the drive motors below.
 */
public class RobotHardware {

    public DcMotor frontLeft, frontRight, backLeft, backRight;

    public void init(HardwareMap hardwareMap) {
        // PLACEHOLDER NAMES — these strings must exactly match whatever
        // you name the motors in the Driver Station's robot configuration
        // file once there's a real Control Hub to configure. Rename here
        // (and only here) once that's set, or send the parts inventory /
        // planned wiring and I'll pre-fill real names.
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        // PLACEHOLDER DIRECTIONS — on a typical mecanum chassis, motors are
        // mirror-mounted left-to-right, so one side usually needs REVERSE
        // for "forward" to actually mean the same physical direction on
        // both sides. This is a common starting guess, NOT a guarantee —
        // verify once wired by testing each wheel spins the correct way.
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);

        // BRAKE means motors resist being pushed when at zero power —
        // generally what you want for a drivetrain so the robot doesn't
        // coast/drift after releasing the sticks.
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }
}
