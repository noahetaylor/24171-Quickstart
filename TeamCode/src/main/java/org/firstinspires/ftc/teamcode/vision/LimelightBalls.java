package org.firstinspires.ftc.teamcode.vision;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the ball pipeline's output (assets/limelight/ball_pipeline.py):
 *   [count, then per ball: tx_deg, ty_deg, class, angular_radius_deg]
 * and locates each ball on the field. Keep in sync with the Python file.
 */
public final class LimelightBalls {
    private LimelightBalls() {}

    public static final int FIELDS_PER_BALL = 4;

    /** Raw detection, before locating it on the field. */
    public static final class Detection {
        public final BallSighting.Kind kind;
        public final double txDeg, tyDeg, angRadiusDeg;

        Detection(BallSighting.Kind kind, double txDeg, double tyDeg, double angRadiusDeg) {
            this.kind = kind;
            this.txDeg = txDeg;
            this.tyDeg = tyDeg;
            this.angRadiusDeg = angRadiusDeg;
        }
    }

    /** Detections from llpython; malformed or truncated output gives what could be read. */
    public static List<Detection> parse(double[] out) {
        List<Detection> list = new ArrayList<>();
        if (out == null || out.length == 0) return list;
        int n = (int) Math.round(out[0]);
        for (int i = 0; i < n; i++) {
            int b = 1 + i * FIELDS_PER_BALL;
            if (b + FIELDS_PER_BALL > out.length) break;
            BallSighting.Kind kind = BallSighting.Kind.fromCode((int) Math.round(out[b + 2]));
            if (kind == null) continue;
            list.add(new Detection(kind, out[b], out[b + 1], out[b + 3]));
        }
        return list;
    }

    /** Parse and locate; balls that fail the floor/size checks are dropped. */
    public static List<BallSighting> locate(double[] out, double panDeg, double tiltDeg,
                                            double robotX, double robotY, double robotHeadingRad) {
        List<BallSighting> balls = new ArrayList<>();
        for (Detection d : parse(out)) {
            BallSighting s = BallLocator.locate(d.kind, d.txDeg, d.tyDeg, d.angRadiusDeg,
                    panDeg, tiltDeg, robotX, robotY, robotHeadingRad);
            if (s != null) balls.add(s);
        }
        return balls;
    }
}
