package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.tuning.AutoTuning;

/**
 * Our estimate of how full our HIVE's up CELL is, in POLLEN-equivalents
 * (NECTAR counts as ~1.7). It only counts what WE shoot, so it's a guess:
 * whenever the camera shows the HIVE flipped, tipped() resets it.
 */
public final class CellLedger {

    private double fill;

    /** Match start: the up CELL holds 3 NECTAR. */
    public CellLedger() {
        reset();
    }

    public void reset() {
        fill = 3 * AutoTuning.NECTAR_EQ;
    }

    public double fill() {
        return fill;
    }

    public void shot(int pollen, int nectar) {
        fill += pollen + nectar * AutoTuning.NECTAR_EQ;
    }

    /** The HIVE flipped: the new up CELL starts empty (its balls fell out pointing down). */
    public void tipped() {
        fill = 0;
    }

    /** True if what we've shot should have tipped it. */
    public boolean tipExpected() {
        return fill >= AutoTuning.TIP_FILL - 1e-9;
    }

    /** POLLEN still needed to tip; at least 1 (if it "should" have tipped but hasn't, keep shooting). */
    public int pollenToTip() {
        return Math.max(1, (int) Math.ceil(AutoTuning.TIP_FILL - fill - 1e-9));
    }
}
