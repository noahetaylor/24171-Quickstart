package org.firstinspires.ftc.teamcode.replay;

/**
 * A single recorded robot pose at a moment in time. Plain data holder, no
 * Android/FTC/Pedro dependencies — keeps this testable as ordinary Java.
 */
public class PoseSample {

    public final long timestampMs; // milliseconds since recording started
    public final double x;
    public final double y;
    public final double headingRadians;

    public PoseSample(long timestampMs, double x, double y, double headingRadians) {
        this.timestampMs = timestampMs;
        this.x = x;
        this.y = y;
        this.headingRadians = headingRadians;
    }

    @Override
    public String toString() {
        return String.format("PoseSample{t=%dms, x=%.2f, y=%.2f, heading=%.3frad}",
                timestampMs, x, y, headingRadians);
    }
}
