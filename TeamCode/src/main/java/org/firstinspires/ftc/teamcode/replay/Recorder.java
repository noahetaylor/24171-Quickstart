package org.firstinspires.ftc.teamcode.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records a stream of PoseSamples over time. Call record(x, y, heading, now)
 * every loop tick — internally throttled to minSampleIntervalMs, so callers
 * don't need to manage timing themselves.
 *
 * No Pedro/Android dependency: "now" is passed in rather than read from a
 * system clock internally, which also makes this trivially unit-testable
 * with fake timestamps (no need to sleep() in a test to advance time).
 */
public class Recorder {

    private final long minSampleIntervalMs;
    private final List<PoseSample> samples = new ArrayList<>();

    private boolean recording = false;
    private long startTimeMs = -1;
    private long lastSampleTimeMs = -1;

    /**
     * @param minSampleIntervalMs minimum time between stored samples, e.g. 50ms.
     *                            Prevents storing a sample every single loop
     *                            tick (which would be excessive/noisy) while
     *                            still capturing enough resolution to
     *                            reconstruct a smooth path later.
     */
    public Recorder(long minSampleIntervalMs) {
        this.minSampleIntervalMs = minSampleIntervalMs;
    }

    /** Begin a new recording. Clears any previously recorded samples. */
    public void start(long nowMs) {
        recording = true;
        startTimeMs = nowMs;
        lastSampleTimeMs = -1;
        samples.clear();
    }

    public void stop() {
        recording = false;
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * Call every loop tick while recording. Internally decides whether
     * enough time has passed since the last stored sample to actually
     * record a new one.
     *
     * @param nowMs current time in milliseconds, from whatever clock the
     *              caller is using (e.g. System.currentTimeMillis() on
     *              a real/virtual robot, or a fake incrementing value in
     *              a unit test)
     */
    public void record(double x, double y, double headingRadians, long nowMs) {
        if (!recording) return;

        if (lastSampleTimeMs != -1 && (nowMs - lastSampleTimeMs) < minSampleIntervalMs) {
            return; // too soon since last sample, skip
        }

        long relativeTimestamp = nowMs - startTimeMs;
        samples.add(new PoseSample(relativeTimestamp, x, y, headingRadians));
        lastSampleTimeMs = nowMs;
    }

    /** Returns an unmodifiable view of the samples recorded so far. */
    public List<PoseSample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    public int sampleCount() {
        return samples.size();
    }
}
