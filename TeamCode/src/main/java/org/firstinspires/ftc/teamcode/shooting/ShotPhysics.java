package org.firstinspires.ftc.teamcode.shooting;

/**
 * Drag-free ballistics: the starting guess before any real shots are
 * measured. POLLEN and NECTAR are holed balls with real drag, so measured
 * table rows (ShotTable) override this wherever they exist.
 *
 * Units: inches, seconds, radians.
 */
public final class ShotPhysics {
    private ShotPhysics() {}

    public static final double G_IN_PER_S2 = 386.09;

    /**
     * Launch speed that carries the ball a horizontal distance d while
     * rising h, at launch angle a. NaN if no speed can (the target is above
     * the launch line: d * tan(a) <= h).
     */
    public static double speedFor(double d, double h, double a) {
        double c = Math.cos(a);
        double denom = 2 * c * c * (d * Math.tan(a) - h);
        if (d <= 0 || denom <= 0) return Double.NaN;
        return Math.sqrt(G_IN_PER_S2 * d * d / denom);
    }

    /** Seconds to cover horizontal distance d at speed v, angle a. */
    public static double timeOfFlight(double d, double v, double a) {
        return d / (v * Math.cos(a));
    }

    /** True if the ball is already coming down when it reaches distance d. */
    public static boolean descendingAt(double d, double v, double a) {
        double t = timeOfFlight(d, v, a);
        return v * Math.sin(a) - G_IN_PER_S2 * t < 0;
    }

    /**
     * The launch angle needing the least speed for (d, h):
     * 45 deg + half the angle up to the target.
     */
    public static double minSpeedAngle(double d, double h) {
        return Math.PI / 4 + Math.atan2(h, d) / 2;
    }
}
