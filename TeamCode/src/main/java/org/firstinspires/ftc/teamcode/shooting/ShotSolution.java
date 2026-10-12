package org.firstinspires.ftc.teamcode.shooting;

/** Everything needed to take one shot from where the robot is. */
public final class ShotSolution {

    public enum Source { TABLE, PHYSICS }

    /** Robot heading that points the shooter at the target (radians, field frame). */
    public final double aimHeadingRad;
    /** aimHeading minus the current heading, in (-pi, pi]: how far to turn (CCW positive). */
    public final double turnRad;
    /** Floor distance, release point to target (inches). */
    public final double distanceIn;
    /** Target height above the release point (inches). */
    public final double riseIn;
    public final double hoodDeg;
    public final double rpm;
    /** Estimated flight time (seconds). */
    public final double flightTimeS;
    /** Ball already falling when it reaches the target. */
    public final boolean descending;
    /** False if the shot can't be made; see reason. */
    public final boolean feasible;
    public final String reason;
    public final Source source;

    ShotSolution(double aimHeadingRad, double turnRad, double distanceIn, double riseIn,
                 double hoodDeg, double rpm, double flightTimeS, boolean descending,
                 boolean feasible, String reason, Source source) {
        this.aimHeadingRad = aimHeadingRad;
        this.turnRad = turnRad;
        this.distanceIn = distanceIn;
        this.riseIn = riseIn;
        this.hoodDeg = hoodDeg;
        this.rpm = rpm;
        this.flightTimeS = flightTimeS;
        this.descending = descending;
        this.feasible = feasible;
        this.reason = reason;
        this.source = source;
    }

    @Override
    public String toString() {
        return String.format("%s d=%.1fin rise=%.1fin hood=%.1f rpm=%.0f turn=%.1fdeg%s",
                source, distanceIn, riseIn, hoodDeg, rpm, Math.toDegrees(turnRad),
                feasible ? "" : " NOT FEASIBLE: " + reason);
    }
}
