package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;
import com.pedropathing.paths.PathBuilder;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.auto.AutoBrain;
import org.firstinspires.ftc.teamcode.auto.CellLedger;
import org.firstinspires.ftc.teamcode.auto.Routes;
import org.firstinspires.ftc.teamcode.auto.StartSpot;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.shooting.AutoAim;
import org.firstinspires.ftc.teamcode.shooting.ShotSpots;
import org.firstinspires.ftc.teamcode.shooting.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.Feeder;
import org.firstinspires.ftc.teamcode.subsystems.Flywheels;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.tuning.AutoTuning;
import org.firstinspires.ftc.teamcode.tuning.MechanismTuning;
import org.firstinspires.ftc.teamcode.tuning.ShotTables;
import org.firstinspires.ftc.teamcode.vision.BallPicker;
import org.firstinspires.ftc.teamcode.vision.BallSighting;

import java.util.List;

/**
 * The match Auto. RedAuto / BlueAuto pick the alliance; everything is
 * written for RED and rotated for BLUE.
 *
 * INIT (gamepad 1):  d-pad left/right = start spot,  Y = garden on/off,
 *                    d-pad up/down = start delay (0.5 s steps).
 *
 * Flow (AutoBrain decides each next job):
 *   shoot the 4 preloads (3 tip the starting CELL; confirm with the tags,
 *   then the 4th goes into the new up CELL) -> GARDEN (optional) -> shoot
 *   -> Limelight pickups on our half -> shoot ... until time runs out.
 * No ball sensor: balls held are counted (pickups assumed to succeed),
 * and feeding runs for FEED_PER_BALL_S per ball. Pose and HIVE state carry
 * into TeleOp through MatchState.
 */
public abstract class CompetitionAuto extends OpMode {

    private static final double AUTO_LENGTH_S = 30;

    private enum Step { DELAY, PLAN, MOVE, AIM, FEED, CONFIRM_TIP, GARDEN_PUSH, GARDEN_BACK, SCAN, PICKUP, DONE }

    private final Alliance alliance;

    // INIT choices
    private StartSpot startSpot = StartSpot.AUDIENCE_WALL;
    private boolean useGarden = true;
    private double delayS = 0;
    private final boolean[] last = new boolean[6];

    private Follower follower;
    private RobotHardware hw;
    private Flywheels flywheels;
    private Feeder feeder;
    private Intake intake;
    private Vision vision;
    private final HiveState hive = MatchState.hive;
    private final CellLedger ledger = new CellLedger();

    private Step step = Step.DELAY;
    private Step afterMove = Step.PLAN;
    private AutoBrain.Job job = null;
    private double stepStart = 0;
    private Pose moveGoal = null;
    private int held = 4;
    private int burst = 0;
    private boolean gardenDone = false;
    private boolean collectGaveUp = false;
    private int scanTurns = 0;
    private Cell upBeforeTip = null;
    private String note = "";

    protected CompetitionAuto(Alliance alliance) {
        this.alliance = alliance;
    }

    // =============================================================== INIT

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        hw = new RobotHardware();
        hw.initMechanisms(hardwareMap);
        flywheels = new Flywheels(hw);
        feeder = new Feeder(hw);
        intake = new Intake(hw);
        vision = new Vision(hw);
        intake.setRamp(Intake.Ramp.STOW);
        hive.reset();
        ledger.reset();
        MatchState.lastPose = null;
    }

    @Override
    public void init_loop() {
        StartSpot[] spots = StartSpot.values();
        if (pressed(0, gamepad1.dpad_right)) startSpot = spots[(startSpot.ordinal() + 1) % spots.length];
        if (pressed(1, gamepad1.dpad_left)) startSpot = spots[(startSpot.ordinal() + spots.length - 1) % spots.length];
        if (pressed(2, gamepad1.y)) useGarden = !useGarden;
        if (pressed(3, gamepad1.dpad_up)) delayS = Math.min(10, delayS + 0.5);
        if (pressed(4, gamepad1.dpad_down)) delayS = Math.max(0, delayS - 0.5);

        follower.setStartingPose(startPose());
        String illegal = startSpot.whyIllegal();
        telemetry.addData("Alliance", alliance);
        telemetry.addData("Start spot (d-pad L/R)", "%s  (x %.1f, y %.1f)", startSpot,
                startSpot.x(alliance), startSpot.y(alliance));
        if (illegal != null) telemetry.addData("!! START SPOT PROBLEM", illegal);
        telemetry.addData("Garden (Y)", useGarden ? "ON" : "OFF (partner takes it)");
        telemetry.addData("Delay (d-pad up/down)", "%.1f s", delayS);
        telemetry.addLine("Load 4 POLLEN.");
        if (!hw.missing.isEmpty()) telemetry.addData("Not in config", hw.missing);
        telemetry.update();
    }

    private Pose startPose() {
        return new Pose(startSpot.x(alliance), startSpot.y(alliance), startSpot.headingRad(alliance));
    }

    @Override
    public void start() {
        follower.setStartingPose(startPose());
        vision.start();
        vision.lookForBalls(0);
        resetRuntime();
        enter(Step.DELAY);
    }

    // =============================================================== LOOP

    @Override
    public void loop() {
        double now = getRuntime();
        double left = AUTO_LENGTH_S - now;
        follower.update();
        Pose pose = follower.getPose();
        Vector vel = follower.getVelocity();
        MatchState.lastPose = pose;

        FieldPoint target = hive.target(alliance);
        AutoAim.Command aim = AutoAim.update(pose.getX(), pose.getY(), pose.getHeading(),
                vel.getXComponent(), vel.getYComponent(), follower.getAngularVelocity(),
                flywheels.rpm(Shooter.POLLEN), flywheels.rpm(Shooter.NECTAR),
                target, ShotTables.POLLEN, ShotTables.NECTAR);

        // Flywheels: spin to the shot while shooting is next, idle otherwise.
        if (job == AutoBrain.Job.SHOOT) flywheels.setRpm(aim.rpmFor(Shooter.POLLEN), aim.rpmFor(Shooter.NECTAR));
        else flywheels.idle();

        double inStep = now - stepStart;
        if (left <= 0.2 && step != Step.DONE) enter(Step.DONE);

        switch (step) {
            case DELAY:
                if (inStep >= delayS) enter(Step.PLAN);
                break;

            case PLAN:
                plan(pose, target, left);
                break;

            case MOVE:
                if (arrived(pose) || inStep > AutoTuning.MOVE_TIMEOUT_S) enter(afterMove);
                break;

            case AIM: {
                Pose want = new Pose(moveGoal.getX(), moveGoal.getY(),
                        ShotSpots.aimHeading(moveGoal.getX(), moveGoal.getY(), target));
                holdIfMoved(want);
                boolean timedOut = inStep > AutoTuning.AIM_TIMEOUT_S && aim.status != AutoAim.Status.NO_SHOT;
                if (aim.fire || timedOut) {
                    burst = Math.min(held, ledger.pollenToTip());
                    note = timedOut ? "fired on timeout (" + aim.status + ")" : "aimed";
                    enter(Step.FEED);
                }
                break;
            }

            case FEED:
                feeder.run(MechanismTuning.FEED_POWER, MechanismTuning.CROSS_WHEEL_POWER);
                if (inStep >= burst * AutoTuning.FEED_PER_BALL_S + AutoTuning.FEED_EXTRA_S) {
                    feeder.stop();
                    held -= burst;
                    ledger.shot(burst, 0);
                    if (ledger.tipExpected()) {
                        upBeforeTip = hive.upCell(alliance);
                        vision.lookAtHive(alliance, pose.getX(), pose.getY(), pose.getHeading());
                        enter(Step.CONFIRM_TIP);
                    } else {
                        enter(Step.PLAN);
                    }
                }
                break;

            case CONFIRM_TIP: {
                vision.lookAtHive(alliance, pose.getX(), pose.getY(), pose.getHeading());
                vision.observeHive(hive);
                boolean seenFlip = hive.upCell(alliance) != upBeforeTip;
                if (seenFlip || inStep > AutoTuning.TIP_CONFIRM_S) {
                    if (!seenFlip) hive.tipped(alliance);  // assume it went
                    ledger.tipped();
                    note = seenFlip ? "tip CONFIRMED by tags" : "tip ASSUMED (tags not seen)";
                    vision.lookForBalls(0);
                    enter(Step.PLAN);
                }
                break;
            }

            case GARDEN_PUSH:
                intake.in();
                if (inStep > AutoTuning.GARDEN_PUSH_S) {
                    moveTo(gardenPose(AutoTuning.GARDEN_APPROACH_Y), Step.PLAN);
                    held = Math.min(AutoBrain.CAPACITY, held + AutoTuning.GARDEN_BALLS);
                    gardenDone = true;
                    step = Step.GARDEN_BACK;
                }
                break;

            case GARDEN_BACK:
                intake.in();
                if (arrived(pose) || inStep > AutoTuning.MOVE_TIMEOUT_S) {
                    intake.stop();
                    enter(Step.PLAN);
                }
                break;

            case SCAN: {
                double pan = AutoTuning.SCAN_PAN_DEG * Math.sin(2 * Math.PI * inStep / AutoTuning.SCAN_S);
                vision.lookForBalls(pan);
                List<BallSighting> balls = vision.balls(pose.getX(), pose.getY(), pose.getHeading());
                BallSighting pick = BallPicker.pick(balls, alliance, true, pose.getX(), pose.getY(), pose.getHeading());
                if (pick != null) {
                    double dir = Math.atan2(pick.y - pose.getY(), pick.x - pose.getX());
                    Pose grab = new Pose(pick.x - AutoTuning.PICKUP_STANDOFF_IN * Math.cos(dir),
                            pick.y - AutoTuning.PICKUP_STANDOFF_IN * Math.sin(dir), dir);
                    note = "going for " + pick;
                    intake.in();
                    moveTo(grab, Step.PLAN);
                    step = Step.PICKUP;
                } else if (inStep > AutoTuning.SCAN_S) {
                    if (++scanTurns >= 3) {
                        collectGaveUp = true;
                        note = "no balls found";
                        enter(Step.PLAN);
                    } else {
                        // Nothing in view: turn 60 deg and look again (up to 3 tries).
                        double h = pose.getHeading() + Math.toRadians(60);
                        moveTo(new Pose(pose.getX(), pose.getY(), h), Step.SCAN);
                    }
                }
                break;
            }

            case PICKUP:
                intake.in();
                if (arrived(pose) || inStep > AutoTuning.PICKUP_TIMEOUT_S) {
                    held = Math.min(AutoBrain.CAPACITY, held + 1);
                    scanTurns = 0;
                    enter(Step.PLAN);
                }
                break;

            case DONE:
                feeder.stop();
                intake.stop();
                break;
        }

        telemetry.addData("Time left", "%.1f s", left);
        telemetry.addData("Step", "%s  (job %s)", step, job);
        telemetry.addData("Held (estimate)", held);
        telemetry.addData("Our up CELL", "%s, fill ~%.1f, %d more to tip", hive.upCell(alliance), ledger.fill(), ledger.pollenToTip());
        telemetry.addData("Shot", aim.status);
        telemetry.addData("Note", note);
        telemetry.addData("Pose", "x %.1f  y %.1f  h %.0f", pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        telemetry.update();
    }

    // =============================================================== jobs

    private void plan(Pose pose, FieldPoint target, double left) {
        job = AutoBrain.next(held, left, useGarden, gardenDone, ledger.pollenToTip());
        if (job == AutoBrain.Job.COLLECT && collectGaveUp) job = held > 0 ? AutoBrain.Job.SHOOT : AutoBrain.Job.PARK;
        intake.stop();
        switch (job) {
            case SHOOT: {
                FieldPoint spot = ShotSpots.nearest(pose.getX(), pose.getY(), target);
                if (spot == null) { job = AutoBrain.Job.PARK; enter(Step.DONE); return; }
                moveTo(new Pose(spot.x, spot.y, ShotSpots.aimHeading(spot.x, spot.y, target)), Step.AIM);
                break;
            }
            case GARDEN:
                moveTo(gardenPose(AutoTuning.GARDEN_APPROACH_Y), Step.GARDEN_PUSH);
                // GARDEN_PUSH then drives in; set its goal when it starts (see enter()).
                break;
            case COLLECT:
                vision.lookForBalls(0);
                enter(Step.SCAN);
                break;
            default:
                enter(Step.DONE);
        }
    }

    private Pose gardenPose(double redY) {
        return new Pose(alliance.xFromRed(AutoTuning.GARDEN_X), alliance.yFromRed(redY),
                alliance.headingFromRed(Math.toRadians(AutoTuning.GARDEN_HEADING_DEG)));
    }

    // =============================================================== motion

    private void moveTo(Pose goal, Step then) {
        Pose from = follower.getPose();
        List<FieldPoint> pts = Routes.route(new FieldPoint(from.getX(), from.getY()), new FieldPoint(goal.getX(), goal.getY()));
        PathBuilder b = follower.pathBuilder();
        double h0 = from.getHeading();
        double turn = Field.normalizeRadians(goal.getHeading() - h0);
        for (int i = 1; i < pts.size(); i++) {
            double ha = h0 + turn * (i - 1) / (pts.size() - 1);
            double hb = h0 + turn * i / (pts.size() - 1);
            b.addPath(new BezierLine(
                    new Pose(pts.get(i - 1).x, pts.get(i - 1).y),
                    new Pose(pts.get(i).x, pts.get(i).y)))
                    .setLinearHeadingInterpolation(ha, hb);
        }
        follower.followPath(b.build(), true);
        moveGoal = goal;
        afterMove = then;
        step = Step.MOVE;
        stepStart = getRuntime();
    }

    private void holdIfMoved(Pose want) {
        if (moveGoal == null
                || Math.abs(Field.normalizeRadians(moveGoal.getHeading() - want.getHeading())) > Math.toRadians(2)) {
            follower.holdPoint(want);
            moveGoal = want;
        }
    }

    private boolean arrived(Pose pose) {
        if (moveGoal == null) return true;
        return !follower.isBusy()
                || (Math.hypot(pose.getX() - moveGoal.getX(), pose.getY() - moveGoal.getY()) < AutoTuning.ARRIVE_TOL_IN
                && Math.abs(Field.normalizeRadians(pose.getHeading() - moveGoal.getHeading()))
                < Math.toRadians(AutoTuning.ARRIVE_TOL_DEG));
    }

    private void enter(Step s) {
        step = s;
        stepStart = getRuntime();
        if (s == Step.GARDEN_PUSH) {
            follower.holdPoint(gardenPose(AutoTuning.GARDEN_PUSH_Y));
            moveGoal = gardenPose(AutoTuning.GARDEN_PUSH_Y);
        }
        if (s == Step.SCAN) follower.holdPoint(follower.getPose());
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
        intake.stop();
        vision.stop();
    }
}
