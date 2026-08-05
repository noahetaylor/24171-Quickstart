package org.firstinspires.ftc.teamcode.replay;

import java.util.List;

/**
 * Fires recorded events during replay based on proximity to their recorded
 * (x, y), not elapsed time — see class-level design note in the commit/PR
 * for why. Assumes events are supplied in the order they were originally
 * recorded (i.e. roughly the order they'll be encountered along the
 * replayed path) and fires them strictly in that order, one at a time.
 *
 * This ordering assumption is deliberate and simple: it avoids needing to
 * check every remaining event against the robot's position on every tick
 * (just the next one), and naturally prevents an event from firing twice
 * or firing out of sequence. Call checkPose(...) every loop tick during
 * replay.
 */
public class EventTrigger {

    private final List<RecordedEvent> events;
    private final double triggerRadius;
    private int nextEventIndex = 0;

    /**
     * @param events         recorded events, in original recording order
     * @param triggerRadius  distance (same units as recorded x/y, e.g.
     *                       inches) within which the robot's current
     *                       position counts as "reached" the event
     */
    public EventTrigger(List<RecordedEvent> events, double triggerRadius) {
        this.events = events;
        this.triggerRadius = triggerRadius;
    }

    /**
     * Call every loop tick with the robot's current position. Returns the
     * event that just fired, or null if none did this tick.
     */
    public RecordedEvent checkPose(double currentX, double currentY) {
        if (nextEventIndex >= events.size()) return null;

        RecordedEvent next = events.get(nextEventIndex);
        double dx = currentX - next.x;
        double dy = currentY - next.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= triggerRadius) {
            nextEventIndex++;
            return next;
        }
        return null;
    }

    public boolean isComplete() {
        return nextEventIndex >= events.size();
    }

    public int remainingEventCount() {
        return events.size() - nextEventIndex;
    }
}
