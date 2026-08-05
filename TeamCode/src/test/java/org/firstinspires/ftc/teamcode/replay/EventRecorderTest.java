package org.firstinspires.ftc.teamcode.replay;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventRecorderTest {

    @Test
    public void doesNotTagBeforeStart() {
        EventRecorder recorder = new EventRecorder();
        recorder.tagEvent("shoot", 10, 20, 1000L);
        assertEquals(0, recorder.eventCount());
    }

    @Test
    public void tagsEventWithRelativeTimestamp() {
        EventRecorder recorder = new EventRecorder();
        recorder.start(1000L);
        recorder.tagEvent("shoot", 10, 20, 1500L);

        List<RecordedEvent> events = recorder.getEvents();
        assertEquals(1, events.size());
        assertEquals("shoot", events.get(0).name);
        assertEquals(10, events.get(0).x, 0.0001);
        assertEquals(500L, events.get(0).timestampMs);
    }

    @Test
    public void startClearsPreviousEvents() {
        EventRecorder recorder = new EventRecorder();
        recorder.start(1000L);
        recorder.tagEvent("shoot", 0, 0, 1000L);
        assertEquals(1, recorder.eventCount());

        recorder.start(5000L);
        assertEquals(0, recorder.eventCount());
    }
}
