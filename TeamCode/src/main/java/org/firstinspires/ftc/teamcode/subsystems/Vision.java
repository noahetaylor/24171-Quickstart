package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.field.Alliance;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.CellTags;
import org.firstinspires.ftc.teamcode.field.Field;
import org.firstinspires.ftc.teamcode.field.HiveState;
import org.firstinspires.ftc.teamcode.tuning.VisionTuning;
import org.firstinspires.ftc.teamcode.vision.BallSighting;
import org.firstinspires.ftc.teamcode.vision.LimelightBalls;
import org.firstinspires.ftc.teamcode.vision.TagLocalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Limelight on its pan/tilt gimbal. Two jobs, one at a time:
 *  - BALLS: tilt down, Python pipeline, located balls on the field.
 *  - TAGS: point at our HIVE, AprilTag pipeline, update HiveState from the
 *    CELL tags it sees (the visible cluster is the UP cell), and compute a
 *    robot position fix from them (TagLocalizer).
 * Results from the wrong pipeline or older than MAX_AGE_MS are ignored, so
 * a switch never mixes the two. Missing hardware: everything returns empty.
 */
public class Vision {

    public enum Mode { OFF, BALLS, TAGS }

    private static final long MAX_AGE_MS = 150;

    private final RobotHardware hw;
    private Mode mode = Mode.OFF;
    private double panDeg = 0, tiltDeg = 0;
    private TagLocalizer.Fix lastFix = null;

    public Vision(RobotHardware hw) {
        this.hw = hw;
    }

    public boolean available() {
        return hw.limelight != null;
    }

    public void start() {
        if (hw.limelight == null) return;
        hw.limelight.setPollRateHz(50);
        hw.limelight.start();
    }

    public void stop() {
        if (hw.limelight != null) hw.limelight.stop();
    }

    public Mode mode() {
        return mode;
    }

    // ------------------------------------------------------------ gimbal

    /** Point the camera: pan CCW +, tilt up +, degrees from straight ahead / level. */
    public void point(double pan, double tilt) {
        panDeg = clamp(pan, LimelightMount.PAN_MIN_DEG, LimelightMount.PAN_MAX_DEG);
        tiltDeg = clamp(tilt, LimelightMount.TILT_MIN_DEG, LimelightMount.TILT_MAX_DEG);
        if (hw.limelightPan != null) {
            hw.limelightPan.setPosition(clamp(LimelightMount.PAN_CENTER_POS + panDeg * LimelightMount.PAN_POS_PER_DEG, 0, 1));
        }
        if (hw.limelightTilt != null) {
            hw.limelightTilt.setPosition(clamp(LimelightMount.TILT_LEVEL_POS + tiltDeg * LimelightMount.TILT_POS_PER_DEG, 0, 1));
        }
    }

    public double panDeg() {
        return panDeg;
    }

    public double tiltDeg() {
        return tiltDeg;
    }

    // ------------------------------------------------------------ balls

    /** Switch to ball finding, camera down at the given pan. */
    public void lookForBalls(double pan) {
        setMode(Mode.BALLS);
        point(pan, VisionTuning.BALL_TILT_DEG);
    }

    /** Balls seen in the latest fresh ball-pipeline frame. */
    public List<BallSighting> balls(double robotX, double robotY, double robotHeadingRad) {
        LLResult r = fresh(LimelightMount.BALL_PIPELINE);
        if (r == null) return Collections.emptyList();
        return LimelightBalls.locate(r.getPythonOutput(), panDeg, tiltDeg, robotX, robotY, robotHeadingRad);
    }

    // ------------------------------------------------------------ tags

    /** Switch to tags and point at our HIVE (as far as the pan range allows). */
    public void lookAtHive(Alliance a, double robotX, double robotY, double robotHeadingRad) {
        setMode(Mode.TAGS);
        double hx = a == Alliance.RED ? Field.RED_HIVE_X : Field.BLUE_HIVE_X;
        double hy = Field.CENTER_IN;
        double dist = Math.hypot(hx - robotX, hy - robotY);
        double pan = Math.toDegrees(Field.normalizeRadians(Math.atan2(hy - robotY, hx - robotX) - robotHeadingRad));
        double tilt = Math.toDegrees(Math.atan2(CellTags.TAG_HEIGHT_IN - LimelightMount.LENS_HEIGHT_IN, dist));
        point(pan, tilt);
    }

    /**
     * CELL tag IDs in the latest fresh tag frame; each is also fed to the
     * HIVE state. Returns the cells seen (empty if none). Also computes a
     * position fix from the same frame (lastFix()).
     */
    public List<Cell> observeHive(HiveState hive, double robotHeadingRad) {
        List<Cell> seen = new ArrayList<>();
        lastFix = null;
        LLResult r = fresh(LimelightMount.TAG_PIPELINE);
        if (r == null) return seen;
        List<TagLocalizer.TagSeen> tags = new ArrayList<>();
        for (LLResultTypes.FiducialResult f : r.getFiducialResults()) {
            Cell c = Cell.forTagId(f.getFiducialId());
            if (c == null) continue;
            if (!seen.contains(c)) {
                seen.add(c);
                hive.observeUp(c);
            }
            Pose3D cam = f.getTargetPoseCameraSpace();
            if (cam != null) {
                Position p = cam.getPosition().toUnit(DistanceUnit.INCH);
                tags.add(new TagLocalizer.TagSeen(f.getFiducialId(), p.x, p.y, p.z));
            }
        }
        lastFix = TagLocalizer.locate(tags, panDeg, tiltDeg, robotHeadingRad);
        return seen;
    }

    /** Position fix from the last observeHive() frame, or null. */
    public TagLocalizer.Fix lastFix() {
        return lastFix;
    }

    // ------------------------------------------------------------ internals

    private void setMode(Mode m) {
        if (m == mode || hw.limelight == null) {
            mode = m;
            return;
        }
        mode = m;
        hw.limelight.pipelineSwitch(m == Mode.TAGS ? LimelightMount.TAG_PIPELINE : LimelightMount.BALL_PIPELINE);
    }

    private LLResult fresh(int pipeline) {
        if (hw.limelight == null) return null;
        LLResult r = hw.limelight.getLatestResult();
        if (r == null || !r.isValid() || r.getPipelineIndex() != pipeline || r.getStaleness() > MAX_AGE_MS) return null;
        return r;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
