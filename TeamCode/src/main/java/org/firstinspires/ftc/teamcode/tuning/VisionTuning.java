package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/** Limelight aiming and ball-picking rules. Editable live from Panels in the pit. */
@Configurable
public final class VisionTuning {
    private VisionTuning() {}

    /** Camera tilt while looking for balls (negative = down). */
    public static double BALL_TILT_DEG = -30;
    /**
     * Height of the tag cluster under an UP cell (inches). The camera tilts
     * up to it based on distance. ESTIMATE: measure on a real HIVE.
     */
    public static double CELL_TAG_HEIGHT_IN = 45;

    /** Ignore balls farther than this (inches along the floor). */
    public static double MAX_BALL_RANGE_IN = 90;
    /** Apparent size may differ from a real ball's by this fraction. */
    public static double SIZE_TOLERANCE = 0.45;

    /** In Auto, stay this far inside our half (center line = 72 in). */
    public static double OWN_SIDE_MARGIN_IN = 10;
    /** Turning costs this many inches of driving per 90 degrees when choosing a ball. */
    public static double TURN_COST_IN_PER_90 = 12;
}
