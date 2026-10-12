package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.tuning.MechanismTuning;

/**
 * The transfer motor (trough -> columns -> flywheels) and the NECTAR cross
 * wheels. There is no separate gate: a stopped transfer holds the balls, so
 * running it IS the trigger.
 *
 * update(wantFire, now) keeps feeding for at least FEED_MIN_S once started,
 * so a READY that flickers for one loop doesn't stutter the balls.
 */
public class Feeder {

    private final RobotHardware hw;
    private double feedingSince = Double.NaN;

    public Feeder(RobotHardware hw) {
        this.hw = hw;
    }

    /** Call every loop. Returns true while feeding. */
    public boolean update(boolean wantFire, double nowS) {
        boolean feeding;
        if (wantFire) {
            if (Double.isNaN(feedingSince)) feedingSince = nowS;
            feeding = true;
        } else {
            feeding = !Double.isNaN(feedingSince) && nowS - feedingSince < MechanismTuning.FEED_MIN_S;
            if (!feeding) feedingSince = Double.NaN;
        }
        run(feeding ? MechanismTuning.FEED_POWER : 0,
                feeding ? MechanismTuning.CROSS_WHEEL_POWER : 0);
        return feeding;
    }

    /** Direct control (unjamming, testing). Clears the minimum-feed timer. */
    public void run(double transferPower, double crossWheelPower) {
        if (hw.transfer != null) hw.transfer.setPower(transferPower);
        if (hw.crossWheels != null) hw.crossWheels.setPower(crossWheelPower);
    }

    public void stop() {
        feedingSince = Double.NaN;
        run(0, 0);
    }
}
