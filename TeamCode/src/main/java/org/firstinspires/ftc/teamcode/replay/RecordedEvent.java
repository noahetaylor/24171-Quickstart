package org.firstinspires.ftc.teamcode.replay;

/**
 * A discrete action (e.g. "shoot", "intake") tagged to a position/time
 * during a recording. Triggered during replay by proximity to (x, y), not
 * by elapsed time — see EventTrigger for why.
 */
public class RecordedEvent {

    public final String name;
    public final double x;
    public final double y;
    public final long timestampMs; // for reference/ordering/logging only

    public RecordedEvent(String name, double x, double y, long timestampMs) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.timestampMs = timestampMs;
    }

    @Override
    public String toString() {
        return String.format("RecordedEvent{%s @ (%.2f, %.2f), t=%dms}", name, x, y, timestampMs);
    }
}
