package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/**
 * Autonomous timing and positions. Positions are written for RED (Pedro
 * coordinates, inches / degrees) and rotated for BLUE. Editable live from
 * Panels in the pit; write final values back here.
 */
@Configurable
public final class AutoTuning {
    private AutoTuning() {}

    // --------------------------------------------------------- the CELL
    /** POLLEN-equivalents that tip a CELL (setup guide: 8 POLLEN, or 3 NECTAR + 3 POLLEN). */
    public static double TIP_FILL = 8.0;
    /** One NECTAR weighs about this many POLLEN (3 NECTAR + 3 POLLEN = 8 POLLEN). */
    public static double NECTAR_EQ = 5.0 / 3.0;
    /** Wait this long at most for the tags to confirm a tip before assuming it happened. */
    public static double TIP_CONFIRM_S = 1.0;

    // --------------------------------------------------------- shooting
    /** Transfer run time per ball held (no ball sensor). MEASURE. */
    public static double FEED_PER_BALL_S = 0.35;
    public static double FEED_EXTRA_S = 0.3;
    /** Fire anyway after this long trying to line up at the shot spot. */
    public static double AIM_TIMEOUT_S = 1.5;
    /** Always leave this much time to get to a shot spot and shoot what we hold. */
    public static double SHOOT_RESERVE_S = 5.0;

    // --------------------------------------------------------- garden (RED: tile A1, along the audience wall)
    /** Line up facing the audience wall, then push in with the intake running. PLACEHOLDERS: test on a field. */
    public static double GARDEN_X = 9.0;
    public static double GARDEN_APPROACH_Y = 24.0;
    public static double GARDEN_PUSH_Y = 10.5;
    public static double GARDEN_HEADING_DEG = -90;
    public static double GARDEN_PUSH_S = 1.2;
    /** Balls the garden run is counted as collecting (4 staged; 3 if the intake can't reach the corner one). */
    public static int GARDEN_BALLS = 4;
    /** Skip the garden if less than this much time is left. */
    public static double GARDEN_MIN_S = 12.0;

    // --------------------------------------------------------- vision collection
    /** Don't start another pickup with less than this left (pickup + drive + shoot). */
    public static double COLLECT_MIN_S = 9.0;
    /** Sweep the camera this far each side while looking for balls. */
    public static double SCAN_PAN_DEG = 60;
    public static double SCAN_S = 1.5;
    /** Give up on one ball after this long. */
    public static double PICKUP_TIMEOUT_S = 2.5;
    /** Drive the robot center this far short of the ball (front of the intake passes over it). */
    public static double PICKUP_STANDOFF_IN = 5.0;

    // --------------------------------------------------------- driving
    public static double MOVE_TIMEOUT_S = 4.0;
    public static double ARRIVE_TOL_IN = 2.0;
    public static double ARRIVE_TOL_DEG = 5.0;
}
