package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.pedropathing.geometry.Pose;

import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.HiveState;

/**
 * Carries the robot's pose and HIVE state from Auto into TeleOp (statics
 * survive between OpModes while the Robot Controller app keeps running).
 */
public final class MatchState {
    private MatchState() {}

    /** Set by Auto as it ends; null if no Auto ran since the app started. */
    public static Pose lastPose = null;
    public static final HiveState hive = new HiveState();

    /**
     * Where TeleOp assumes the robot is if no Auto ran (practice). Written
     * for RED; BLUE is rotated. PLACEHOLDER: robot center on tile A2 facing
     * the blue wall. Place the robot there before starting TeleOp alone.
     */
    public static final double RED_PRACTICE_X = 12, RED_PRACTICE_Y = 36, RED_PRACTICE_HEADING_DEG = 0;

    public static Pose practiceStart(Alliance a) {
        return new Pose(a.xFromRed(RED_PRACTICE_X), a.yFromRed(RED_PRACTICE_Y),
                a.headingFromRed(Math.toRadians(RED_PRACTICE_HEADING_DEG)));
    }
}
