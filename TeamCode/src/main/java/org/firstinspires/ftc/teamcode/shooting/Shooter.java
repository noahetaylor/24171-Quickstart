package org.firstinspires.ftc.teamcode.shooting;

/**
 * The robot's two shooters and their fixed geometry.
 *
 * Robot frame (inches from robot center): FORWARD toward the intake/front,
 * LEFT toward the robot's left. Values come from the team CAD
 * (BIOBUZZ repo robot-design/cad/params.py, TV2_* "SIMPLE" heads): 72 mm
 * Gecko flywheel on a goBILDA 5203 1:1, hood exit 150 deg -> 60 deg launch,
 * release point = hood end about the flywheel axis.
 *
 * Re-check these when the shooter hardware changes (turret, adjustable
 * hood, different flywheel).
 */
public enum Shooter {
    // forward, left, height, wheel diameter
    /** POLLEN head, robot's right side. Release from CAD: x -184.8, y -97.1, z 378 mm. */
    POLLEN(-7.28, -3.82, 14.88, 72.0 / 25.4),
    /** NECTAR head, robot's left side. Release from CAD: x -181.3, y 97.1, z 388 mm. */
    NECTAR(-7.14, 3.82, 15.28, 72.0 / 25.4);

    /** Where the ball leaves the hood, robot frame (inches). */
    public final double releaseForwardIn;
    public final double releaseLeftIn;
    public final double releaseHeightIn;
    public final double wheelDiameterIn;

    Shooter(double releaseForwardIn, double releaseLeftIn, double releaseHeightIn, double wheelDiameterIn) {
        this.releaseForwardIn = releaseForwardIn;
        this.releaseLeftIn = releaseLeftIn;
        this.releaseHeightIn = releaseHeightIn;
        this.wheelDiameterIn = wheelDiameterIn;
    }

    // ------------------------------------------------- tunables (both heads)

    /**
     * Direction the ball flies, relative to the robot's heading, degrees CCW.
     * 0 = out the front. From the CAD's hood geometry (release at the hood's
     * rear end, tangent forward-up). CONFIRM ON THE ROBOT: if balls fly out
     * the back, set 180.
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

    public static boolean hoodIsFixed() {
        return HOOD_MAX_DEG - HOOD_MIN_DEG < 1e-9;
    }

    /** Flywheel rpm that gives this ball exit speed (in/s). */
    public double rpmForSpeed(double ballSpeedInPerS) {
        double surface = ballSpeedInPerS / EXIT_EFFICIENCY;
        return surface / (Math.PI * wheelDiameterIn) * 60.0;
    }

    /** Ball exit speed (in/s) at this flywheel rpm. */
    public double speedForRpm(double rpm) {
        return rpm / 60.0 * Math.PI * wheelDiameterIn * EXIT_EFFICIENCY;
    }
}
