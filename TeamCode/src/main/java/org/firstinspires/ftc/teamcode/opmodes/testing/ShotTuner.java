package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.opmodes.competition.MatchState;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.shooting.AutoAim;
import org.firstinspires.ftc.teamcode.shooting.ShotLog;
import org.firstinspires.ftc.teamcode.shooting.ShotPlanner;
import org.firstinspires.ftc.teamcode.shooting.ShotSolution;
import org.firstinspires.ftc.teamcode.shooting.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.Feeder;
import org.firstinspires.ftc.teamcode.subsystems.Flywheels;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;
import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;
import org.firstinspires.ftc.teamcode.tuning.ShotTables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Data gathering for the shot tables. Shoot, mark each ball hit or miss;
 * every ball becomes a row in /sdcard/FIRST/data/shots/shots-<time>.csv,
 * which the Shot Table Editor (tools/shot-table-editor) pulls and plots.
 *
 * INIT: X = blue, B = red (decides which CELL is "ours").
 * Gamepad 1 while running:
 *   sticks          drive (robot-centric) to reposition
 *   right trigger   hold: auto-turn onto the CELL (no firing)
 *   X               switch shooter under test (POLLEN / NECTAR)
 *   d-pad up/down   rpm +/- 50          Y   rpm = what the model suggests
 *   A               fire (runs the transfer briefly)
 *   RB / LB         log one ball HIT / MISS (press once per ball)
 *   B               undo the last logged ball
 *   back            distance from odometry <-> TAPE (typed in)
 *   d-pad left/right  TAPE distance -/+ 1 in
 * Load only the ball type for the shooter under test.
 */
@TeleOp(name = "Shot Tuner", group = "Testing")
public class ShotTuner extends OpMode {

    private static final double FIRE_PULSE_S = 0.3;

    private Follower follower;
    private Flywheels flywheels;
    private Feeder feeder;
    private RobotHardware hw;
    private final HiveState hive = new HiveState();
    private Alliance alliance = Alliance.RED;

    private Shooter shooter = Shooter.POLLEN;
    private double rpm = 3000;
    private boolean tape = false;
    private double tapeDistance = 60;
    private double fireUntil = -1;

    private final List<ShotLog> session = new ArrayList<>();
    private File file;
    private String fileError = null;

    private final boolean[] last = new boolean[12];

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        hw = new RobotHardware();
        hw.initMechanisms(hardwareMap);
        flywheels = new Flywheels(hw);
        feeder = new Feeder(hw);
        Pose start = MatchState.lastPose != null ? MatchState.lastPose : MatchState.practiceStart(alliance);
        follower.setStartingPose(start);

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File dir = new File(AppUtil.ROOT_FOLDER + "/data/shots");
        if (!dir.isDirectory() && !dir.mkdirs()) fileError = "can't create " + dir;
        file = new File(dir, "shots-" + stamp + ".csv");
    }

    @Override
    public void init_loop() {
        if (gamepad1.x) alliance = Alliance.BLUE;
        if (gamepad1.b) alliance = Alliance.RED;
        if (MatchState.lastPose == null) follower.setStartingPose(MatchState.practiceStart(alliance));
        telemetry.addData("Alliance (X blue / B red)", alliance);
        telemetry.addData("Start", MatchState.lastPose != null ? "pose from last OpMode"
                : "practice spot (see MatchState): place the robot there");
        telemetry.addData("Log file", file.getName());
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config", hw.missing);
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive(true);
    }

    @Override
    public void loop() {
        double now = getRuntime();
        Pose pose = follower.getPose();
        FieldPoint target = hive.target(alliance);
        ShotSolution plan = ShotPlanner.plan(pose.getX(), pose.getY(), pose.getHeading(),
                target, shooter, ShotTables.forShooter(shooter));
        double distance = tape ? tapeDistance : plan.distanceIn;

        // ---- buttons
        if (pressed(0, gamepad1.x)) shooter = shooter == Shooter.POLLEN ? Shooter.NECTAR : Shooter.POLLEN;
        if (pressed(1, gamepad1.dpad_up)) rpm += 50;
        if (pressed(2, gamepad1.dpad_down)) rpm = Math.max(0, rpm - 50);
        if (pressed(3, gamepad1.y) && plan.feasible) rpm = Math.round(plan.rpm / 10.0) * 10;
        if (pressed(4, gamepad1.a)) fireUntil = now + FIRE_PULSE_S;
        if (pressed(5, gamepad1.right_bumper)) log(ShotLog.Result.HIT, distance, pose);
        if (pressed(6, gamepad1.left_bumper)) log(ShotLog.Result.MISS, distance, pose);
        if (pressed(7, gamepad1.b) && !session.isEmpty()) {
            session.remove(session.size() - 1);
            save();
        }
        if (pressed(8, gamepad1.back)) tape = !tape;
        if (pressed(9, gamepad1.dpad_left)) tapeDistance -= 1;
        if (pressed(10, gamepad1.dpad_right)) tapeDistance += 1;

        // ---- drive (optionally auto-turn)
        double turn = -gamepad1.right_stick_x;
        if (gamepad1.right_trigger > 0.5) {
            AutoAim.Command aim = AutoAim.update(pose.getX(), pose.getY(), pose.getHeading(),
                    0, 0, follower.getAngularVelocity(), 0, 0, target, ShotTables.POLLEN, ShotTables.NECTAR);
            turn = aim.turnPower;
        }
        follower.setTeleOpDrive(-gamepad1.left_stick_y, -gamepad1.left_stick_x, turn, true);
        follower.update();

        // ---- shooter under test only
        flywheels.setRpm(shooter == Shooter.POLLEN ? rpm : 0, shooter == Shooter.NECTAR ? rpm : 0);
        feeder.update(now < fireUntil, now);

        int hits = 0, n = 0;
        for (ShotLog s : session) {
            if (s.shooter == shooter) {
                n++;
                if (s.result == ShotLog.Result.HIT) hits++;
            }
        }
        telemetry.addData("Shooter", "%s   (X to switch)", shooter);
        telemetry.addData("rpm", "target %.0f   actual %.0f   model says %s",
                rpm, flywheels.rpm(shooter), plan.feasible ? String.format(Locale.US, "%.0f (%s)", plan.rpm, plan.source) : "no shot");
        telemetry.addData("Distance", "%.1f in  (%s, back to switch)", distance, tape ? "TAPE" : "odometry");
        telemetry.addData("Turn to aim", "%.1f deg  (hold RT to auto-turn)", Math.toDegrees(plan.turnRad));
        telemetry.addData("Logged", "%d balls this session, %s %d/%d hit", session.size(), shooter, hits, n);
        telemetry.addData("File", fileError != null ? fileError : file.getName());
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config", hw.missing);
        telemetry.update();
    }

    private void log(ShotLog.Result result, double distance, Pose pose) {
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
        session.add(new ShotLog(time, shooter, result, distance, ShooterTuning.HOOD_MIN_DEG,
                rpm, flywheels.rpm(shooter), tape ? ShotLog.DistanceSource.TAPE : ShotLog.DistanceSource.POSE,
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading())));
        save();
    }

    /** Rewrites this session's file (so undo works). */
    private void save() {
        try (Writer w = new FileWriter(file, false)) {
            w.write(ShotLog.HEADER + "\n");
            for (ShotLog s : session) w.write(s.toCsv() + "\n");
            fileError = null;
        } catch (IOException e) {
            fileError = "SAVE FAILED: " + e.getMessage();
        }
    }

    private boolean pressed(int i, boolean now) {
        boolean r = now && !last[i];
        last[i] = now;
        return r;
    }

    @Override
    public void stop() {
        flywheels.stop();
        feeder.stop();
    }
}
