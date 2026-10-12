package org.firstinspires.ftc.teamcode.shooting;

import org.firstinspires.ftc.teamcode.tuning.ShooterTuning;

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
 * hood, different flywheel). Tuned numbers live in tuning/ShooterTuning.
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

    public static boolean hoodIsFixed() {
        return ShooterTuning.HOOD_MAX_DEG - ShooterTuning.HOOD_MIN_DEG < 1e-9;
    }

    /** Flywheel rpm that gives this ball exit speed (in/s). */
    public double rpmForSpeed(double ballSpeedInPerS) {
        double surface = ballSpeedInPerS / ShooterTuning.EXIT_EFFICIENCY;
        return surface / (Math.PI * wheelDiameterIn) * 60.0;
    }

    /** Ball exit speed (in/s) at this flywheel rpm. */
    public double speedForRpm(double rpm) {
        return rpm / 60.0 * Math.PI * wheelDiameterIn * ShooterTuning.EXIT_EFFICIENCY;
    }
}
