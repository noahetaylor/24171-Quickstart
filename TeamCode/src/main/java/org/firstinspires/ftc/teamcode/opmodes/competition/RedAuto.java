package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.field.Alliance;

@Autonomous(name = "Red Auto", group = "Competition", preselectTeleOp = "Red TeleOp")
public class RedAuto extends CompetitionAuto {
    public RedAuto() {
        super(Alliance.RED);
    }
}
