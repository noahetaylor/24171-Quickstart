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
    public static double HEADING_TOL_DEG = 2.0;       // allowed aim error at release
    public static double MAX_TURN_RATE_DEG_S = 30.0;  // must be nearly settled
    public static double RPM_TOL = 0.03;              // fraction of target rpm
    public static double MAX_DRIVE_SPEED_IN_S = 40.0; // faster than this: don't fire
    public static boolean LEAD_MOVING_SHOTS = true;
}
