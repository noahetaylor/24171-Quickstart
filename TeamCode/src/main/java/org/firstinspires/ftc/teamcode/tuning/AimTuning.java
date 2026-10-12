package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/**
 * Auto-aim turning and fire rules. Editable live from Panels in the pit.
 * Write the values you settle on back here.
 */
@Configurable
public final class AimTuning {
    private AimTuning() {}

    public static double TURN_KP = 1.2;               // turn power per radian of error
    public static double TURN_KD = 0.08;              // turn power per rad/s (damping)
    public static double TURN_MAX = 0.8;
    public static double TURN_MIN = 0.06;             // enough to actually move the robot

    /**
     * Fire only if each ball's center will pass within this many inches
     * (sideways) of the CELL opening's center. The opening is 20 in wide:
     * half-width 10, minus NECTAR's radius ~1.8 = 8.2, minus margin. The two
     * shooters are parallel and 7.6 in apart, so even a perfect aim puts each
     * ball ~4 in off center; what's left is the allowed aim error.
     */
    public static double MAX_MISS_IN = 6.5;

    /** Aim error (inches at the target) small enough to stop turning. */
    public static double AIM_DEADBAND_IN = 0.75;

    public static double MAX_TURN_RATE_DEG_S = 30.0;  // must be nearly settled
    public static double RPM_TOL = 0.03;              // fraction of target rpm
    public static double MAX_DRIVE_SPEED_IN_S = 40.0; // faster than this: don't fire
    public static boolean LEAD_MOVING_SHOTS = true;

    // Pre-align (hold the button: the robot drives itself to a shot spot)
    /** Robot-center distance from the target to shoot from. Pick a distance the table covers well. */
    public static double PREALIGN_DISTANCE_IN = 60.0;
    /** Re-send Pedro's hold target when the wanted pose moves more than this. */
    public static double PREALIGN_RETARGET_IN = 1.5;
    public static double PREALIGN_RETARGET_DEG = 2.0;
}
