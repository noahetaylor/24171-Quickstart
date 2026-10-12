package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.field.Alliance;

@TeleOp(name = "Blue TeleOp", group = "Competition")
public class BlueTeleOp extends CompetitionTeleOp {
    public BlueTeleOp() {
        super(Alliance.BLUE);
    }
}
