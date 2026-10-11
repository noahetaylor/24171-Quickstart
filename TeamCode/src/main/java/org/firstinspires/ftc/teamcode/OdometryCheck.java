package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.config.HardwareConfig;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;

/**
 * Pre-tuning wiring check — run this BEFORE Pedro's Tuning OpModes. It
 * reads the pods and IMU directly (no Pedro), so a wrong port, pod
 * direction, hub orientation, or motor direction shows up here as a plain
 * number instead of as a mysteriously drifting pose.
 *
 * Push the robot BY HAND (motors are off unless a button is held):
 *   push forward  -> forward pod inches go UP
 *   push left     -> strafe pod inches go UP
 *   turn left/CCW -> heading goes UP
 * If any go the wrong way, flip the matching value in HardwareConfig.
 *
 * Motor spin test (hold to run at low power; every wheel should roll the
 * robot FORWARD — put it on a block first):
 *   X = leftFront   Y = rightFront   A = leftRear   B = rightRear
 *
 * Back resets the pod and heading readouts to zero.
 */
@TeleOp(name = "Odometry Check", group = "Setup")
public class OdometryCheck extends OpMode {
    private static final double SPIN_TEST_POWER = 0.3;

    private RobotHardware hardware;
    private DcMotor forwardPod, strafePod;
    private IMU imu;
    private int forwardZero, strafeZero;

    @Override
    public void init() {
        hardware = new RobotHardware();
        hardware.init(hardwareMap);

        forwardPod = hardwareMap.get(DcMotor.class, HardwareConfig.FORWARD_POD);
        strafePod = hardwareMap.get(DcMotor.class, HardwareConfig.STRAFE_POD);

        imu = hardwareMap.get(IMU.class, HardwareConfig.IMU);
        imu.initialize(new IMU.Parameters(
                new RevHubOrientationOnRobot(HardwareConfig.HUB_LOGO, HardwareConfig.HUB_USB)));

        zero();
    }

    @Override
    public void loop() {
        if (gamepad1.back) zero();

        hardware.frontLeft.setPower(gamepad1.x ? SPIN_TEST_POWER : 0);
        hardware.frontRight.setPower(gamepad1.y ? SPIN_TEST_POWER : 0);
        hardware.backLeft.setPower(gamepad1.a ? SPIN_TEST_POWER : 0);
        hardware.backRight.setPower(gamepad1.b ? SPIN_TEST_POWER : 0);

        // getCurrentPosition() ignores the motor's setDirection(), so the pod
        // sign is applied here exactly the way Pedro's Encoder applies it.
        int forwardTicks = (int) ((forwardPod.getCurrentPosition() - forwardZero) * HardwareConfig.FORWARD_POD_DIR);
        int strafeTicks = (int) ((strafePod.getCurrentPosition() - strafeZero) * HardwareConfig.STRAFE_POD_DIR);

        telemetry.addLine("Push forward / left / turn CCW: all should go UP");
        telemetry.addData("Forward pod (" + HardwareConfig.FORWARD_POD + ")", "%d ticks  %.2f in",
                forwardTicks, forwardTicks * HardwareConfig.POD_INCHES_PER_TICK);
        telemetry.addData("Strafe pod (" + HardwareConfig.STRAFE_POD + ")", "%d ticks  %.2f in",
                strafeTicks, strafeTicks * HardwareConfig.POD_INCHES_PER_TICK);
        telemetry.addData("Heading", "%.1f deg", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
        telemetry.addLine();
        telemetry.addLine("Hold X/Y/A/B: LF / RF / LR / RR forward  |  Back: zero");
        telemetry.update();
    }

    private void zero() {
        forwardZero = forwardPod.getCurrentPosition();
        strafeZero = strafePod.getCurrentPosition();
        imu.resetYaw();
    }
}
