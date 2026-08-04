package org.firstinspires.ftc.teamcode.subsystems;

/**
 * Drivetrain subsystem — TeleOp manual driving only. Autonomous continues
 * to use Pedro's Follower directly (see AutoTemplate), which needs
 * exclusive control of the drive motors for closed-loop path following.
 * Don't call driveRobotCentric() during an autonomous period.
 */
public class Drivetrain {

    private final RobotHardware hardware;

    public Drivetrain(RobotHardware hardware) {
        this.hardware = hardware;
    }

    /**
     * Standard mecanum drive math, robot-relative.
     *
     * @param forward  -1 (backward) to 1 (forward)
     * @param strafe   -1 (left) to 1 (right)
     * @param rotate   -1 (counter-clockwise) to 1 (clockwise)
     */
    public void driveRobotCentric(double forward, double strafe, double rotate) {
        double flPower = forward + strafe + rotate;
        double frPower = forward - strafe - rotate;
        double blPower = forward - strafe + rotate;
        double brPower = forward + strafe - rotate;

        // Scale all four powers down proportionally if any exceeds 1.0, so
        // the robot's actual direction of travel doesn't distort when you
        // push multiple sticks to their limits at once.
        double max = Math.max(1.0, Math.max(
                Math.abs(flPower),
                Math.max(Math.abs(frPower), Math.max(Math.abs(blPower), Math.abs(brPower)))
        ));

        hardware.frontLeft.setPower(flPower / max);
        hardware.frontRight.setPower(frPower / max);
        hardware.backLeft.setPower(blPower / max);
        hardware.backRight.setPower(brPower / max);
    }

    // TODO (later, by team decision): driveFieldCentric(forward, strafe,
    // rotate, robotHeadingRadians) — rotates the forward/strafe vector by
    // -robotHeadingRadians before feeding it into the same math above.
    // Deliberately left out for now; robot-centric first.

    public void stop() {
        driveRobotCentric(0, 0, 0);
    }
}
