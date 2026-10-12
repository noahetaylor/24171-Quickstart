package org.firstinspires.ftc.teamcode.opmodes.testing;

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
 *   1. RAW counts: whichever way you push the robot to make a pod's raw
 *      count go UP is the arrow to set for it in the Pod Placer.
 *   2. After saving the Pod Placer and redeploying, the forward/strafe
 *      readouts should go UP when pushed forward / left.
 *   3. Turn left (counter-clockwise) -> heading goes UP. If not, fix
 *      HUB_LOGO / HUB_USB in HardwareConfig.
 *
 * Motor spin test (hold to run at low power; every wheel should roll the
 * robot FORWARD — put it on a block first):
 *   X = leftFront   Y = rightFront   A = leftRear   B = rightRear
 *
 * Back resets the pod and heading readouts to zero.
 */
@TeleOp(name = "Odometry Check", group = "Testing")
public class OdometryCheck extends OpMode {
    private static final double SPIN_TEST_POWER = 0.3;

    private RobotHardware hardware;
    private DcMotor pod0, pod3;
    private IMU imu;
    private int zero0, zero3;

    @Override
    public void init() {
        hardware = new RobotHardware();
        hardware.init(hardwareMap);

        pod0 = hardwareMap.get(DcMotor.class, HardwareConfig.POD_PORT0);
        pod3 = hardwareMap.get(DcMotor.class, HardwareConfig.POD_PORT3);

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

        // getCurrentPosition() ignores the motor's setDirection(), so these
        // are the pods' true raw counts.
        int raw0 = pod0.getCurrentPosition() - zero0;
        int raw3 = pod3.getCurrentPosition() - zero3;

        // Same direction flip Pedro's Encoder applies.
        boolean port3IsForward = HardwareConfig.FORWARD_POD.equals(HardwareConfig.POD_PORT3);
        double forwardTicks = (port3IsForward ? raw3 : raw0) * HardwareConfig.FORWARD_POD_DIR;
        double strafeTicks = (port3IsForward ? raw0 : raw3) * HardwareConfig.STRAFE_POD_DIR;

        telemetry.addLine("RAW (set Pod Placer arrows from these)");
        telemetry.addData("  port 0 pod (" + HardwareConfig.POD_PORT0 + ")", raw0);
        telemetry.addData("  port 3 pod (" + HardwareConfig.POD_PORT3 + ")", raw3);
        telemetry.addLine();
        telemetry.addLine("AS PEDRO SEES IT (push fwd / left / turn CCW: all UP)");
        telemetry.addData("  forward pod", "%.2f in", forwardTicks * HardwareConfig.POD_INCHES_PER_TICK);
        telemetry.addData("  strafe pod", "%.2f in", strafeTicks * HardwareConfig.POD_INCHES_PER_TICK);
        telemetry.addData("  heading", "%.1f deg", imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES));
        telemetry.addLine();
        telemetry.addLine("Hold X/Y/A/B: LF / RF / LR / RR forward  |  Back: zero");
        telemetry.update();
    }

    private void zero() {
        zero0 = pod0.getCurrentPosition();
        zero3 = pod3.getCurrentPosition();
        imu.resetYaw();
    }
}
