package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.tuning.AutoTuning;

/**
 * Decides Auto's next job from what the robot holds, the time left, and how
 * close the CELL is to tipping. Pure logic, unit-tested in AutoBrainTest.
 *
 * Priorities:
 *  1. SHOOT when holding enough to tip, when full, or when time is short.
 *  2. GARDEN (once, if enabled and there's time).
 *  3. COLLECT with vision while there's time to come back and shoot.
 *  4. SHOOT whatever is left; otherwise PARK.
 */
public final class AutoBrain {
    private AutoBrain() {}

    public static final int CAPACITY = 4;   // G407

    public enum Job { SHOOT, GARDEN, COLLECT, PARK }

    public static Job next(int held, double timeLeftS, boolean gardenEnabled, boolean gardenDone, int pollenToTip) {
        boolean shortOnTime = timeLeftS < AutoTuning.SHOOT_RESERVE_S;
        if (held > 0 && (held >= pollenToTip || held >= CAPACITY || shortOnTime)) return Job.SHOOT;
        if (!shortOnTime && held < CAPACITY) {
            if (gardenEnabled && !gardenDone && timeLeftS >= AutoTuning.GARDEN_MIN_S) return Job.GARDEN;
            if (timeLeftS >= AutoTuning.COLLECT_MIN_S) return Job.COLLECT;
        }
        return held > 0 ? Job.SHOOT : Job.PARK;
    }
}
