package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.field.Alliance;

@Autonomous(name = "Blue Auto", group = "Competition", preselectTeleOp = "Blue TeleOp")
public class BlueAuto extends CompetitionAuto {
    public BlueAuto() {
        super(Alliance.BLUE);
    }
}
