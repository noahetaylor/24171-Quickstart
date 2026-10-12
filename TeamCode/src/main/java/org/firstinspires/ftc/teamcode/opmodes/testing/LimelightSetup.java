package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.subsystems.RobotHardware;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Uploads the ball-finding Python pipeline (TeamCode assets:
 * limelight/ball_pipeline.py) to the Limelight's pipeline slot 0. Run it
 * once after changing the Python file (or after resetting the Limelight).
 *
 * The AprilTag pipeline (slot 1, family 36h11) is set up once on the
 * Limelight's web page; see docs/vision.md.
 */
@TeleOp(name = "Limelight Setup", group = "Testing")
public class LimelightSetup extends OpMode {

    private static final String ASSET = "limelight/ball_pipeline.py";
    private String result = "not run";

    @Override
    public void init() {
        RobotHardware hw = new RobotHardware();
        hw.initMechanisms(hardwareMap);
        if (hw.limelight == null) {
            result = "No \"limelight\" in the robot configuration.";
            return;
        }
        try (InputStream in = hardwareMap.appContext.getAssets().open(ASSET)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
            String code = buf.toString("UTF-8");
            boolean ok = hw.limelight.uploadPython(code, LimelightMount.BALL_PIPELINE);
            result = ok
                    ? "Uploaded " + code.length() + " characters to pipeline " + LimelightMount.BALL_PIPELINE
                      + ". Check it with Limelight Check."
                    : "Upload FAILED. Is slot " + LimelightMount.BALL_PIPELINE
                      + " a Python pipeline? (Limelight web page, see docs/vision.md)";
        } catch (IOException e) {
            result = "Could not read " + ASSET + ": " + e.getMessage();
        }
    }

    @Override
    public void init_loop() {
        telemetry.addData("Limelight Setup", result);
        telemetry.update();
    }

    @Override
    public void loop() {
        telemetry.addData("Limelight Setup", result);
        telemetry.update();
    }
}
