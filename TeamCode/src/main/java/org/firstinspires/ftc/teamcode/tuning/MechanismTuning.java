package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/**
 * Powers, servo positions and flywheel control for the mechanisms.
 * Editable live from Panels in the pit; write final values back here.
 */
@Configurable
public final class MechanismTuning {
    private MechanismTuning() {}

    // Intake
    public static double INTAKE_POWER = 1.0;
    public static double EJECT_POWER = -0.6;

    // Feeding (transfer motor + NECTAR cross wheels). Running the transfer IS the trigger.
    public static double FEED_POWER = 1.0;
    public static double CROSS_WHEEL_POWER = 1.0;
    /** Once feeding starts, keep going at least this long (no stutter on a flickering READY). */
    public static double FEED_MIN_S = 0.25;

    // Flywheels
    /** Spin between shots so spin-up is short. 0 = off. */
    public static double IDLE_RPM = 1500;
    /** Velocity PIDF (SDK units). F ~ 32767 / max ticks per second. TUNE. */
    public static double FLYWHEEL_P = 10.0;
    public static double FLYWHEEL_I = 0.0;
    public static double FLYWHEEL_D = 0.0;
    public static double FLYWHEEL_F = 12.5;

    // Ramp bar servo positions (0..1). PLACEHOLDERS: set on the robot.
    public static double RAMP_DOWN = 0.10;   // scoops the FLOWER
    public static double RAMP_BLOCK = 0.35;  // POLLEN under, NECTAR stopped
    public static double RAMP_OPEN = 0.40;
    public static double RAMP_STOW = 0.95;   // start of match, inside the sizing box
}
