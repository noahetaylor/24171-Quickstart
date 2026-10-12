package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.teamcode.config.HardwareConfig;
import org.firstinspires.ftc.teamcode.shooting.Shooter;
import org.firstinspires.ftc.teamcode.tuning.MechanismTuning;

/**
 * Both flywheels, under the motor controller's own velocity loop
 * (RUN_USING_ENCODER). A missing flywheel reads 0 rpm and ignores commands.
 */
public class Flywheels {

    private final RobotHardware hw;
    private double lastP = Double.NaN, lastI, lastD, lastF;

    public Flywheels(RobotHardware hw) {
        this.hw = hw;
    }

    /** Set both targets. 0 lets them coast. */
    public void setRpm(double pollenRpm, double nectarRpm) {
        applyPidfIfChanged();
        set(hw.pollenFlywheel, pollenRpm);
        set(hw.nectarFlywheel, nectarRpm);
    }

    public void idle() {
        setRpm(MechanismTuning.IDLE_RPM, MechanismTuning.IDLE_RPM);
    }

    public void stop() {
        setRpm(0, 0);
    }

    public double rpm(Shooter s) {
        DcMotorEx m = s == Shooter.POLLEN ? hw.pollenFlywheel : hw.nectarFlywheel;
        if (m == null) return 0;
        return m.getVelocity() / HardwareConfig.FLYWHEEL_TICKS_PER_REV * 60.0;
    }

    private static void set(DcMotorEx m, double rpm) {
        if (m == null) return;
        if (rpm <= 0) {
            m.setPower(0);
        } else {
            m.setVelocity(rpm / 60.0 * HardwareConfig.FLYWHEEL_TICKS_PER_REV);
        }
    }

    /** Lets Panels edits to the PIDF take effect without restarting. */
    private void applyPidfIfChanged() {
        if (MechanismTuning.FLYWHEEL_P == lastP && MechanismTuning.FLYWHEEL_I == lastI
                && MechanismTuning.FLYWHEEL_D == lastD && MechanismTuning.FLYWHEEL_F == lastF) return;
        lastP = MechanismTuning.FLYWHEEL_P;
        lastI = MechanismTuning.FLYWHEEL_I;
        lastD = MechanismTuning.FLYWHEEL_D;
        lastF = MechanismTuning.FLYWHEEL_F;
        PIDFCoefficients c = new PIDFCoefficients(lastP, lastI, lastD, lastF);
        for (DcMotorEx m : new DcMotorEx[]{hw.pollenFlywheel, hw.nectarFlywheel}) {
            if (m != null) m.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, c);
        }
    }
}
