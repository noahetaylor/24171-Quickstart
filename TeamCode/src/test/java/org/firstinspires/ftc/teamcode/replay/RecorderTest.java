package org.firstinspires.ftc.teamcode.replay;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecorderTest {

    @Test
    public void doesNotRecordBeforeStart() {
        Recorder recorder = new Recorder(50);
        recorder.record(1.0, 2.0, 0.0, 1000L);
        assertEquals(0, recorder.sampleCount());
    }

    @Test
    public void firstSampleAfterStartHasZeroRelativeTimestamp() {
        Recorder recorder = new Recorder(50);
        recorder.start(1000L);
        recorder.record(5.0, 6.0, 0.0, 1000L);

        List<PoseSample> samples = recorder.getSamples();
        assertEquals(1, samples.size());
        assertEquals(0L, samples.get(0).timestampMs);
        assertEquals(5.0, samples.get(0).x, 0.0001);
        assertEquals(6.0, samples.get(0).y, 0.0001);
    }

    @Test
    public void throttlesSamplesWithinMinInterval() {
        Recorder recorder = new Recorder(50);
        recorder.start(1000L);
        recorder.record(0, 0, 0, 1000L); // stored, t=0
        recorder.record(1, 1, 0, 1020L); // only 20ms later, should be skipped
        recorder.record(2, 2, 0, 1040L); // only 40ms since last stored, still skipped

        assertEquals(1, recorder.sampleCount());
    }

    @Test
    public void recordsNewSampleOnceIntervalElapses() {
        Recorder recorder = new Recorder(50);
        recorder.start(1000L);
        recorder.record(0, 0, 0, 1000L); // stored, t=0
        recorder.record(1, 1, 0, 1060L); // 60ms later, should be stored

        assertEquals(2, recorder.sampleCount());
        assertEquals(60L, recorder.getSamples().get(1).timestampMs);
    }

    @Test
    public void stopPreventsFurtherRecording() {
        Recorder recorder = new Recorder(50);
        recorder.start(1000L);
        recorder.record(0, 0, 0, 1000L);
        recorder.stop();
        recorder.record(1, 1, 0, 2000L);

        assertEquals(1, recorder.sampleCount());
        assertFalse(recorder.isRecording());
    }

    @Test
    public void startClearsPreviousSamples() {
        Recorder recorder = new Recorder(50);
        recorder.start(1000L);
        recorder.record(0, 0, 0, 1000L);
        assertEquals(1, recorder.sampleCount());

        recorder.start(5000L); // new recording session
        assertEquals(0, recorder.sampleCount());
        assertTrue(recorder.isRecording());
    }
}
