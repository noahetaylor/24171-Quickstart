package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.shooting.ShotPlanner;
import org.firstinspires.ftc.teamcode.shooting.ShotSolution;
import org.firstinspires.ftc.teamcode.tuning.ShotTables;
import org.firstinspires.ftc.teamcode.shooting.Shooter;

/**
 * Practice tool for filling ShotTables: drive around and read off the
 * distance, aim and rpm the shot model wants for each shooter. Does not run
 * any mechanism.
 *
 * INIT: X = blue, B = red. D-pad up/down moves the start spot 6 in along the
 * field's Y. Place the robot's CENTER on the start spot shown, facing the
 * far wall, then press start.
 * RUN: drive with the sticks. Y = our HIVE tipped, A = reset HIVEs to the
 * match-start state.
 */
@TeleOp(name = "Shot Planner Check", group = "Testing")
public class ShotPlannerCheck extends OpMode {

    /** Start spot: robot center, Pedro coordinates. Default = middle of tile D1. */
    public static double START_X = 84;
    public static double START_Y = 12;
    public static double START_HEADING_DEG = 90; // facing the far wall

    private Follower follower;
    private final HiveState hive = new HiveState();
    private Alliance alliance = Alliance.RED;

    private boolean lastUp, lastDown, lastY, lastA;

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
    }

    @Override
    public void init_loop() {
        if (gamepad1.x) alliance = Alliance.BLUE;
        if (gamepad1.b) alliance = Alliance.RED;
        if (gamepad1.dpad_up && !lastUp) START_Y += 6;
        if (gamepad1.dpad_down && !lastDown) START_Y -= 6;
        lastUp = gamepad1.dpad_up;
        lastDown = gamepad1.dpad_down;

        telemetry.addData("Alliance (X blue / B red)", alliance);
        telemetry.addData("Start (robot center)", "x %.0f  y %.0f  facing far wall", START_X, START_Y);
        telemetry.addLine("D-pad up/down moves the start 6 in.");
        telemetry.update();
    }

    @Override
    public void start() {
        follower.setStartingPose(new Pose(START_X, START_Y, Math.toRadians(START_HEADING_DEG)));
        follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        follower.setTeleOpDrive(-gamepad1.left_stick_y, -gamepad1.left_stick_x, -gamepad1.right_stick_x, true);
        follower.update();

        if (gamepad1.y && !lastY) hive.tipped(alliance);
        if (gamepad1.a && !lastA) hive.reset();
        lastY = gamepad1.y;
        lastA = gamepad1.a;

        Pose pose = follower.getPose();
        Cell up = hive.upCell(alliance);
        telemetry.addData("Pose", "x %.1f  y %.1f  h %.1f deg",
                pose.getX(), pose.getY(), Math.toDegrees(pose.getHeading()));
        telemetry.addData("Target", "%s cell (Y = tipped, A = reset)", up);

        for (Shooter s : Shooter.values()) {
            ShotSolution sol = ShotPlanner.plan(pose.getX(), pose.getY(), pose.getHeading(),
                    hive.target(alliance), s, ShotTables.forShooter(s));
            telemetry.addLine();
            telemetry.addData(s.name(), sol.feasible ? sol.source.name() : "NO SHOT: " + sol.reason);
            telemetry.addData("  distance", "%.1f in", sol.distanceIn);
            telemetry.addData("  turn", "%.1f deg", Math.toDegrees(sol.turnRad));
            telemetry.addData("  hood / rpm", "%.0f deg / %.0f", sol.hoodDeg, sol.rpm);
            telemetry.addData("  ball", sol.descending ? "falling into the cell" : "STILL RISING at the cell");
        }
        telemetry.update();
    }
}
