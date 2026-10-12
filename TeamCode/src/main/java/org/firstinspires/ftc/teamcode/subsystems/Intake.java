package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.tuning.MechanismTuning;

/** Intake rollers and the ramp bar. Missing devices are skipped. */
public class Intake {

    public enum Ramp { DOWN, BLOCK, OPEN, STOW }

    private final RobotHardware hw;
    private Ramp ramp = Ramp.STOW;

    public Intake(RobotHardware hw) {
        this.hw = hw;
    }

    public void in() {
        power(MechanismTuning.INTAKE_POWER);
    }

    public void eject() {
        power(MechanismTuning.EJECT_POWER);
    }

    public void stop() {
        power(0);
    }

    public void setRamp(Ramp r) {
        ramp = r;
        if (hw.rampBar == null) return;
        switch (r) {
            case DOWN: hw.rampBar.setPosition(MechanismTuning.RAMP_DOWN); break;
            case BLOCK: hw.rampBar.setPosition(MechanismTuning.RAMP_BLOCK); break;
            case OPEN: hw.rampBar.setPosition(MechanismTuning.RAMP_OPEN); break;
            default: hw.rampBar.setPosition(MechanismTuning.RAMP_STOW); break;
        }
    }

    public Ramp ramp() {
        return ramp;
    }

    private void power(double p) {
        if (hw.intake != null) hw.intake.setPower(p);
    }
}
