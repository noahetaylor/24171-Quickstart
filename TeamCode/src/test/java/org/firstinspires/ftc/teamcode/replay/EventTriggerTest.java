package org.firstinspires.ftc.teamcode.replay;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EventTriggerTest {

    @Test
    public void doesNotFireWhenFarFromEvent() {
        List<RecordedEvent> events = Arrays.asList(
                new RecordedEvent("shoot", 50, 50, 1000L)
        );
        EventTrigger trigger = new EventTrigger(events, 5.0);

        RecordedEvent fired = trigger.checkPose(0, 0);
        assertNull(fired);
        assertEquals(1, trigger.remainingEventCount());
    }

    @Test
    public void firesWhenWithinRadius() {
        List<RecordedEvent> events = Arrays.asList(
                new RecordedEvent("shoot", 50, 50, 1000L)
        );
        EventTrigger trigger = new EventTrigger(events, 5.0);

        RecordedEvent fired = trigger.checkPose(52, 51); // within radius 5
        assertNotNull(fired);
        assertEquals("shoot", fired.name);
        assertTrue(trigger.isComplete());
    }

    @Test
    public void firesEventsInOrder() {
        List<RecordedEvent> events = Arrays.asList(
                new RecordedEvent("intake", 10, 10, 1000L),
                new RecordedEvent("shoot", 50, 50, 2000L)
        );
        EventTrigger trigger = new EventTrigger(events, 3.0);

        // Robot passes near the second event's position first, but it
        // shouldn't fire out of order since "intake" hasn't fired yet.
        RecordedEvent firedEarly = trigger.checkPose(50, 50);
        assertNull(firedEarly);

        // Now pass near the first event — should fire "intake".
        RecordedEvent firedFirst = trigger.checkPose(10, 10);
        assertNotNull(firedFirst);
        assertEquals("intake", firedFirst.name);

        // Now pass near the second event — should fire "shoot".
        RecordedEvent firedSecond = trigger.checkPose(50, 50);
        assertNotNull(firedSecond);
        assertEquals("shoot", firedSecond.name);

        assertTrue(trigger.isComplete());
    }

    @Test
    public void doesNotFireSameEventTwice() {
        List<RecordedEvent> events = Arrays.asList(
                new RecordedEvent("shoot", 50, 50, 1000L)
        );
        EventTrigger trigger = new EventTrigger(events, 5.0);

        trigger.checkPose(50, 50); // fires
        RecordedEvent second = trigger.checkPose(50, 50); // shouldn't fire again
        assertNull(second);
    }
}
