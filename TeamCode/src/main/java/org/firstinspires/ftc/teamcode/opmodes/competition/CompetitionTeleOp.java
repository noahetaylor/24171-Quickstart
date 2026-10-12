package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.shooting.AutoAim;
import org.firstinspires.ftc.teamcode.shooting.ShotPlanner;
import org.firstinspires.ftc.teamcode.shooting.ShotSolution;
import org.firstinspires.ftc.teamcode.shooting.ShotSpots;
import org.firstinspires.ftc.teamcode.shooting.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.Feeder;
import org.firstinspires.ftc.teamcode.subsystems.Flywheels;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;
import org.firstinspires.ftc.teamcode.tuning.AimTuning;
import org.firstinspires.ftc.teamcode.tuning.MechanismTuning;
import org.firstinspires.ftc.teamcode.tuning.ShotTables;

/**
 * The match TeleOp. RedTeleOp / BlueTeleOp pick the alliance; everything
 * else is here.
 *
 * Gamepad 1:
 *   left stick      drive (robot-centric)      right stick X   turn
 *   RIGHT TRIGGER   hold: aim + fire from here (you still drive; it turns)
 *   LEFT TRIGGER    hold: PRE-ALIGN, robot drives itself to a shot spot,
 *                   aims and fires. Let go to take back control.
 *   RB / LB         hold: intake / eject
 *   B               hold: feed without aiming (unjam)
 *   X               flywheel idle spin on/off
 *   Y               our HIVE tipped (until the camera does it)
 *   back            reset HIVEs to the match-start state
 *   d-pad           ramp bar: up stow, down down, left block, right open
 *
 * Rumbles at 1:00 left (FLOWER scoring unlocked) and 0:20 left.
 */
public abstract class CompetitionTeleOp extends OpMode {

    private static final double TELEOP_LENGTH_S = 120;
    private static final double TRIGGER = 0.5;

    private final Alliance alliance;

    private Follower follower;
    private RobotHardware hw;
    private Flywheels flywheels;
    private Feeder feeder;
    private Intake intake;
    private final HiveState hive = MatchState.hive;

    private boolean idleSpin = true;
    private boolean preAligning = false;
    private Pose holdTarget = null;
    private boolean rumbled60, rumbled20;
    private final Edge xEdge = new Edge(), yEdge = new Edge(), backEdge = new Edge();

    protected CompetitionTeleOp(Alliance alliance) {
        this.alliance = alliance;
    }

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        hw = new RobotHardware();
        hw.initMechanisms(hardwareMap); // Pedro owns the drive motors
        flywheels = new Flywheels(hw);
        feeder = new Feeder(hw);
        intake = new Intake(hw);

        Pose start = MatchState.lastPose != null ? MatchState.lastPose : MatchState.practiceStart(alliance);
        follower.setStartingPose(start);
    }

    @Override
    public void init_loop() {
        telemetry.addData("Alliance", alliance);
        telemetry.addData("Start pose", MatchState.lastPose != null
                ? "from Auto" : "PRACTICE spot (no Auto ran): see MatchState");
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config (skipped)", hw.missing);
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive(true);
        resetRuntime();
    }

    @Override
    public void loop() {
        double now = getRuntime();
        Pose pose = follower.getPose();
        Vector vel = follower.getVelocity();
        double omega = follower.getAngularVelocity();

        if (xEdge.rose(gamepad1.x)) idleSpin = !idleSpin;
        if (yEdge.rose(gamepad1.y)) hive.tipped(alliance);
        if (backEdge.rose(gamepad1.back)) hive.reset();

        FieldPoint target = hive.target(alliance);
        AutoAim.Command aim = AutoAim.update(pose.getX(), pose.getY(), pose.getHeading(),
                vel.getXComponent(), vel.getYComponent(), omega,
                flywheels.rpm(Shooter.POLLEN), flywheels.rpm(Shooter.NECTAR),
                target, ShotTables.POLLEN, ShotTables.NECTAR);

        boolean aiming = gamepad1.right_trigger > TRIGGER;
        boolean preAlign = gamepad1.left_trigger > TRIGGER;

        // ---- drive
        String mode;
        if (preAlign) {
            mode = driveToShotSpot(pose, target, aim);
        } else {
            if (preAligning) {          // just let go: hand control back
                follower.startTeleopDrive(true);
                preAligning = false;
                holdTarget = null;
            }
            double turn = aiming ? aim.turnPower : -gamepad1.right_stick_x;
            follower.setTeleOpDrive(-gamepad1.left_stick_y, -gamepad1.left_stick_x, turn, true);
            mode = aiming ? "AIM + FIRE" : "DRIVER";
        }
        follower.update();

        // ---- flywheels + feed
        boolean shooting = aiming || preAlign;
        if (shooting) {
            flywheels.setRpm(aim.rpmFor(Shooter.POLLEN), aim.rpmFor(Shooter.NECTAR));
        } else if (idleSpin) {
            flywheels.idle();
        } else {
            flywheels.stop();
        }
        boolean feeding;
        if (gamepad1.b) {
            feeder.run(MechanismTuning.FEED_POWER, MechanismTuning.CROSS_WHEEL_POWER);
            feeding = true;
        } else {
            feeding = feeder.update(shooting && aim.fire, now);
        }

        // ---- intake + ramp
        if (gamepad1.right_bumper) intake.in();
        else if (gamepad1.left_bumper) intake.eject();
        else intake.stop();
        if (gamepad1.dpad_up) intake.setRamp(Intake.Ramp.STOW);
        if (gamepad1.dpad_down) intake.setRamp(Intake.Ramp.DOWN);
        if (gamepad1.dpad_left) intake.setRamp(Intake.Ramp.BLOCK);
        if (gamepad1.dpad_right) intake.setRamp(Intake.Ramp.OPEN);

        // ---- match clock
        double left = TELEOP_LENGTH_S - now;
        if (!rumbled60 && left <= 60) { gamepad1.rumble(600); rumbled60 = true; }
        if (!rumbled20 && left <= 20) { gamepad1.rumbleBlips(3); rumbled20 = true; }

        MatchState.lastPose = pose;

        // ---- telemetry
        telemetry.addData("Mode", mode);
        telemetry.addData("Shot", "%s%s", aim.status, feeding ? "  FEEDING" : "");
        telemetry.addData("Target", "%s cell up", hive.upCell(alliance));
        telemetry.addData("Distance", "%.0f in", aim.pollen.distanceIn);
        telemetry.addData("Flywheels", "P %.0f/%.0f  N %.0f/%.0f rpm",
                flywheels.rpm(Shooter.POLLEN), aim.rpmFor(Shooter.POLLEN),
                flywheels.rpm(Shooter.NECTAR), aim.rpmFor(Shooter.NECTAR));
        telemetry.addData("Pose", "x %.1f  y %.1f  h %.1f",
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        telemetry.addData("Time left", "%.0f s%s", Math.max(0, left), left <= 60 ? "  FLOWER OPEN" : "");
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config", hw.missing);
        telemetry.update();
    }

    /** Pre-align: hold the nearest shot spot with the aimed heading. */
    private String driveToShotSpot(Pose pose, FieldPoint target, AutoAim.Command aim) {
        FieldPoint spot = ShotSpots.nearest(pose.getX(), pose.getY(), target);
        if (spot == null) {
            follower.setTeleOpDrive(0, 0, 0, true);
            return "PRE-ALIGN: no spot";
        }
        // Heading that aims both shooters from the spot (mean of the two).
        ShotSolution p = ShotPlanner.plan(spot.x, spot.y, pose.getHeading(), target, Shooter.POLLEN, ShotTables.POLLEN);
        ShotSolution n = ShotPlanner.plan(spot.x, spot.y, pose.getHeading(), target, Shooter.NECTAR, ShotTables.NECTAR);
        double heading = Math.atan2(Math.sin(p.aimHeadingRad) + Math.sin(n.aimHeadingRad),
                Math.cos(p.aimHeadingRad) + Math.cos(n.aimHeadingRad));

        Pose want = new Pose(spot.x, spot.y, heading);
        if (holdTarget == null || moved(holdTarget, want)) {
            follower.holdPoint(want);
            holdTarget = want;
        }
        preAligning = true;
        return String.format("PRE-ALIGN to (%.0f, %.0f)", spot.x, spot.y);
    }

    private static boolean moved(Pose a, Pose b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY()) > AimTuning.PREALIGN_RETARGET_IN
                || Math.abs(Field.normalizeRadians(a.getHeading() - b.getHeading()))
                > Math.toRadians(AimTuning.PREALIGN_RETARGET_DEG);
    }

    @Override
    public void stop() {
        flywheels.stop();
        feeder.stop();
        intake.stop();
    }

    /** Rising-edge detector for buttons. */
    private static final class Edge {
        private boolean last;

        boolean rose(boolean now) {
            boolean r = now && !last;
            last = now;
            return r;
        }
    }
}
