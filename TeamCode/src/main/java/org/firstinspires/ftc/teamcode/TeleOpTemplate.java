package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;

/**
 * Minimal TeleOp skeleton showing the subsystem pattern in use. As real
 * mechanisms get added, they'll follow this same shape: declare the
 * subsystem, initialize it off RobotHardware in init(), call its methods
 * from gamepad input in loop().
 */
@TeleOp(name = "TeleOp Template")
public class TeleOpTemplate extends OpMode {

    private RobotHardware hardware;
    private Drivetrain drivetrain;

    @Override
    public void init() {
        hardware = new RobotHardware();
        hardware.init(hardwareMap);

        drivetrain = new Drivetrain(hardware);

        telemetry.addLine("TeleOp init complete.");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Gamepad's left_stick_y is inverted by convention (pushing the
        // stick forward/up returns a negative value), so it's negated here.
        double forward = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_stick_x;
        double rotate = gamepad1.right_stick_x;

        drivetrain.driveRobotCentric(forward, strafe, rotate);

        telemetry.addData("forward", forward);
        telemetry.addData("strafe", strafe);
        telemetry.addData("rotate", rotate);
        telemetry.update();
    }
}
