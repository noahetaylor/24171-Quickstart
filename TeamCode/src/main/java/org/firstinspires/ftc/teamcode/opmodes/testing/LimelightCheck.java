package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.opmodes.competition.MatchState;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.vision.BallPicker;
import org.firstinspires.ftc.teamcode.vision.BallSighting;
import org.firstinspires.ftc.teamcode.vision.TagLocalizer;

import java.util.List;

/**
 * Limelight + gimbal check and calibration.
 *
 * INIT: X = blue, B = red. Place the robot on the practice start spot (MatchState).
 * RUN (gamepad 1):
 *   A          ball mode (camera down)      Y   tag mode (camera points at our HIVE)
 *   d-pad      nudge pan (left/right) and tilt (up/down) by 2 deg
 *   back       re-center (pan 0, ball tilt)
 *   X          (tag mode) apply tag position fixes to odometry on/off
 *   sticks     drive (robot-centric)
 *
 * Calibrating the servos (config/LimelightMount): watch the camera image on
 * the Limelight web page and nudge until it looks straight ahead and level.
 * If the pan/tilt shown then isn't 0, fix PAN_CENTER_POS / TILT_LEVEL_POS.
 * Then point at something 45 degrees off and fix *_POS_PER_DEG.
 */
@TeleOp(name = "Limelight Check", group = "Testing")
public class LimelightCheck extends OpMode {

    private Follower follower;
    private RobotHardware hw;
    private Vision vision;
    private final HiveState hive = new HiveState();
    private Alliance alliance = Alliance.RED;
    private double pan = 0, tilt = 0;
    private final boolean[] last = new boolean[8];
    private boolean applyFixes = false;

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        hw = new RobotHardware();
        hw.initMechanisms(hardwareMap);
        vision = new Vision(hw);
    }

    @Override
    public void init_loop() {
        if (gamepad1.x) alliance = Alliance.BLUE;
        if (gamepad1.b) alliance = Alliance.RED;
        telemetry.addData("Alliance (X blue / B red)", alliance);
        telemetry.addData("Limelight", vision.available() ? "found" : "NOT in config");
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config", hw.missing);
        telemetry.update();
    }

    @Override
    public void start() {
        follower.setStartingPose(MatchState.lastPose != null ? MatchState.lastPose : MatchState.practiceStart(alliance));
        follower.startTeleopDrive(true);
        vision.start();
        recenter();
    }

    private void recenter() {
        vision.lookForBalls(0);
        pan = vision.panDeg();
        tilt = vision.tiltDeg();
    }

    @Override
    public void loop() {
        follower.setTeleOpDrive(-gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x, true);
        follower.update();
        Pose pose = follower.getPose();

        if (pressed(0, gamepad1.a) || pressed(6, gamepad1.back)) recenter();
        if (pressed(1, gamepad1.y)) {
            vision.lookAtHive(alliance, pose.getX(), pose.getY(), pose.getHeading());
            pan = vision.panDeg();
            tilt = vision.tiltDeg();
        }
        if (pressed(7, gamepad1.x)) applyFixes = !applyFixes;
        if (pressed(2, gamepad1.dpad_left)) pan += 2;
        if (pressed(3, gamepad1.dpad_right)) pan -= 2;
        if (pressed(4, gamepad1.dpad_up)) tilt += 2;
        if (pressed(5, gamepad1.dpad_down)) tilt -= 2;
        vision.point(pan, tilt);

        telemetry.addData("Mode", "%s   (A balls / Y tags)", vision.mode());
        telemetry.addData("Gimbal", "pan %.0f  tilt %.0f deg", vision.panDeg(), vision.tiltDeg());
        telemetry.addData("Pose", "x %.1f  y %.1f  h %.0f", pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));

        if (vision.mode() == Vision.Mode.BALLS) {
            List<BallSighting> balls = vision.balls(pose.getX(), pose.getY(), pose.getHeading());
            BallSighting pick = BallPicker.pick(balls, alliance, true, pose.getX(), pose.getY(), pose.getHeading());
            telemetry.addData("Balls", balls.size());
            for (BallSighting b : balls) telemetry.addLine("  " + b + (b == pick ? "   <- would go for this" : ""));
        } else {
            List<Cell> seen = vision.observeHive(hive, pose.getHeading());
            telemetry.addData("CELL tags seen", seen.isEmpty() ? "none" : seen.toString());
            telemetry.addData("Our up CELL", hive.upCell(alliance));
            TagLocalizer.Fix fix = vision.lastFix();
            if (fix != null) {
                telemetry.addData("Tag fix", "x %.1f  y %.1f  (%d tags, %.0f in)  off by %.1f in",
                        fix.x, fix.y, fix.tags, fix.rangeIn, Math.hypot(fix.x - pose.getX(), fix.y - pose.getY()));
                if (applyFixes) {
                    double[] c = TagLocalizer.correct(pose.getX(), pose.getY(), fix, 0, 0);
                    if (c != null) follower.setPose(new Pose(c[0], c[1], pose.getHeading()));
                }
            } else {
                telemetry.addData("Tag fix", "none (no tags, or they disagree)");
            }
            telemetry.addData("Apply fixes (X)", applyFixes);
        }
        telemetry.addData("Pipelines", "balls = %d, tags = %d", LimelightMount.BALL_PIPELINE, LimelightMount.TAG_PIPELINE);
        telemetry.update();
    }

    private boolean pressed(int i, boolean now) {
        boolean r = now && !last[i];
        last[i] = now;
        return r;
    }

    @Override
    public void stop() {
        vision.stop();
    }
}
