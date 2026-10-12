package org.firstinspires.ftc.teamcode.opmodes.competition;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.field.Alliance;

@TeleOp(name = "Red TeleOp", group = "Competition")
public class RedTeleOp extends CompetitionTeleOp {
    public RedTeleOp() {
        super(Alliance.RED);
    }
}
