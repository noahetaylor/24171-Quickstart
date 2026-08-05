package org.firstinspires.ftc.teamcode.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tags discrete events to a position/time during a recording session (e.g.
 * the driver presses a button to mark "shoot happened here"). Separate
 * from Recorder (which handles the continuous pose stream) so each piece
 * stays small and independently testable; a later step composes them.
 */
public class EventRecorder {

    private final List<RecordedEvent> events = new ArrayList<>();
    private long startTimeMs = -1;
    private boolean recording = false;

    public void start(long nowMs) {
        recording = true;
        startTimeMs = nowMs;
        events.clear();
    }

    public void stop() {
        recording = false;
    }

    /** Tag an event at the given position, at the current moment. */
    public void tagEvent(String name, double x, double y, long nowMs) {
        if (!recording) return;
        long relativeTimestamp = nowMs - startTimeMs;
        events.add(new RecordedEvent(name, x, y, relativeTimestamp));
    }

    public List<RecordedEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public int eventCount() {
        return events.size();
    }
}
