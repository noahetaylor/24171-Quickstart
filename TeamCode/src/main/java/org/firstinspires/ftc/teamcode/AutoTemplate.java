package org.firstinspires.ftc.teamcode;

// ── If any of these show red in Android Studio, retype the class name
//    (e.g. "Pose") and use autocomplete (Alt+Enter) to pick the real
//    package for your installed Pedro 2.1.2 jar. Package names have
//    shifted between Pedro releases (localization/pathgen vs geometry/paths).
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

// TODO SWAP POINT: your Quickstart's generated Constants files.
// These were created when you first ran the Pedro Quickstart installer —
// check org.firstinspires.ftc.teamcode.pedroPathing.constants for the
// exact names/locations if these imports don't resolve.
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;

/**
 * Reusable autonomous skeleton.
 *
 * Per-autonomous, you only need to touch the two blocks marked
 * "SWAP POINT" below: the starting pose, and the Paths class body
 * (paste in whatever the Pedro Visualizer exports for that specific run).
 * Everything else (Follower setup, the update loop, telemetry) stays the same.
 */
@Autonomous(name = "Auto Template")
public class AutoTemplate extends OpMode {

    private Follower follower;
    private Paths paths;

    // ── SWAP POINT 1: starting pose ─────────────────────────────
    // Must match the first point of your Paths.MainChain below,
    // and must match wherever the robot is physically placed at
    // the start of the match for this particular autonomous.
    private final Pose startPose = new Pose(56.000, 8.000, Math.toRadians(90));

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startPose);

        paths = new Paths(follower);

        telemetry.addLine("Init complete. Ready to run.");
        telemetry.update();
    }

    @Override
    public void start() {
        follower.followPath(paths.MainChain);
    }

    @Override
    public void loop() {
        follower.update();

        // TODO (fall task): once actions/mechanisms exist, this is where
        // you'd check robot position/time and trigger events — e.g.
        // "if follower.getPose() is near the scoring pose, run the intake."
        // For now this just drives the path and reports status.

        telemetry.addData("Busy following path", follower.isBusy());
        telemetry.addData("Current Pose", follower.getPose());
        telemetry.update();
    }

    // ── SWAP POINT 2: paste your Pedro Visualizer export here ───
    // Replace the entire body of this class with whatever the
    // visualizer generates for a given autonomous. Keep the class
    // name, constructor signature, and MainChain field name the same
    // so nothing else in this file needs to change.
    public static class Paths {
        public PathChain MainChain;

        public Paths(Follower follower) {
            MainChain = follower.pathBuilder()
                .addPath(
                    new BezierCurve(
                        new Pose(56.000, 8.000),
                        new Pose(78.000, 48.000),
                        new Pose(68.058, 80.330)
                    )
                )
                .setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(120))
                .addPath(
                    new BezierLine(
                        new Pose(68.058, 80.330),
                        new Pose(30.000, 120.000)
                    )
                )
                .setLinearHeadingInterpolation(Math.toRadians(120), Math.toRadians(145))
                .addPath(
                    new BezierLine(
                        new Pose(30.000, 120.000),
                        new Pose(120.000, 96.000)
                    )
                )
                .setTangentHeadingInterpolation()
                .addPath(
                    new BezierCurve(
                        new Pose(120.000, 96.000),
                        new Pose(132.000, 12.000),
                        new Pose(78.000, 12.000)
                    )
                )
                .setLinearHeadingInterpolation(Math.toRadians(0), Math.toRadians(130))
                .build();
        }
    }
}
