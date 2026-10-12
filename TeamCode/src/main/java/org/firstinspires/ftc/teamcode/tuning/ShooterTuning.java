package org.firstinspires.ftc.teamcode.tuning;

import com.bylazar.configurables.annotations.Configurable;

/**
 * Shooter numbers tuned from testing (both heads). Editable live from
 * Panels in the pit (not at matches: R704). Write the values you settle on
 * back here, or they reset on the next deploy.
 */
@Configurable
public final class ShooterTuning {
    private ShooterTuning() {}

    /**
     * Direction the ball flies, relative to the robot's heading, degrees CCW.
     * 0 = out the front. From the CAD's hood geometry (release at the hood's
     * rear end, tangent forward-up). Confirmed by the team: balls leave out
     * the front.
     */
    public static double LAUNCH_YAW_DEG = 0.0;

    /**
     * Hood range in degrees of launch elevation. Equal = fixed hood (the
     * SIMPLE heads: whichever print is fitted, 50..70 in steps of 5).
     */
    public static double HOOD_MIN_DEG = 60.0;
    public static double HOOD_MAX_DEG = 60.0;

    /**
     * Ball speed / flywheel surface speed. A ball squeezed between a spinning
     * wheel and a fixed hood rolls, so its center moves at about half the
     * surface speed; slip lowers it. Tune from measured shots.
     */
    public static double EXIT_EFFICIENCY = 0.5;

    /** Flywheel rpm we will ask for. 5203 1:1 is ~6000 free; leave headroom for load. */
    public static double MAX_RPM = 5100.0;

    /** Trigger to ball leaving the hood, seconds. Auto-aim fires this much early. MEASURE. */
    public static double FIRE_LATENCY_S = 0.15;
}
