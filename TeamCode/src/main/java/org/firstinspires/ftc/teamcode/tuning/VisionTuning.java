package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/** Limelight aiming and ball-picking rules. Editable live from Panels in the pit. */
@Configurable
public final class VisionTuning {
    private VisionTuning() {}

    /** Camera tilt while looking for balls (negative = down). */
    public static double BALL_TILT_DEG = -30;

    /** Ignore balls farther than this (inches along the floor). */
    public static double MAX_BALL_RANGE_IN = 90;
    /** Apparent size may differ from a real ball's by this fraction. */
    public static double SIZE_TOLERANCE = 0.45;

    /** In Auto, stay this far inside our half (center line = 72 in). */
    public static double OWN_SIDE_MARGIN_IN = 10;
    /** Turning costs this many inches of driving per 90 degrees when choosing a ball. */
    public static double TURN_COST_IN_PER_90 = 12;

    // Settling: frames are ignored for a moment after the pipeline switches or the gimbal
    // swings, so a ball or tag is never located with the wrong camera angle.
    public static double SWITCH_SETTLE_S = 0.25;
    public static double GIMBAL_SETTLE_S = 0.2;
    /** A gimbal move bigger than this (deg) triggers GIMBAL_SETTLE_S. */
    public static double GIMBAL_BIG_MOVE_DEG = 6;

    // AprilTag position correction (TagLocalizer)
    /** Fraction of the way to move odometry toward each tag fix (0 = off). */
    public static double TAG_GAIN = 0.3;
    /** Ignore fixes this far from odometry (a misread, or the wrong CELL state). */
    public static double TAG_MAX_JUMP_IN = 12;
    /** Only correct while nearly still. */
    public static double TAG_MAX_SPEED_IN_S = 15;
    public static double TAG_MAX_TURN_DEG_S = 30;
    /** Ignore tags farther than this (floor distance). */
    public static double TAG_MAX_RANGE_IN = 110;
    /** Per-tag fixes in one frame must agree within this. */
    public static double MAX_TAG_SPREAD_IN = 3;
    /** A tag's measured height must match CellTags.TAG_HEIGHT_IN within this. */
    public static double MAX_TAG_HEIGHT_ERR_IN = 8;
}
