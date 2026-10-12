package org.firstinspires.ftc.teamcode.shooting;

import java.util.Locale;

/**
 * One logged ball: what the robot did and whether it went in. Written as a
 * CSV line by the Shot Tuner, read by the Shot Table Editor
 * (tools/shot-table-editor). Keep HEADER and the editor's parser in sync.
 */
public final class ShotLog {

    public enum Result { HIT, MISS }

    /** Where the distance came from: odometry, or typed in from a tape measure. */
    public enum DistanceSource { POSE, TAPE }

    public static final String HEADER =
            "time,shooter,result,distance_in,hood_deg,rpm_target,rpm_actual,distance_source,x,y,heading_deg";

    public final String time;
    public final Shooter shooter;
    public final Result result;
    public final double distanceIn;
    public final double hoodDeg;
    public final double rpmTarget;
    public final double rpmActual;
    public final DistanceSource source;
    public final double x, y, headingDeg;

    public ShotLog(String time, Shooter shooter, Result result, double distanceIn, double hoodDeg,
                   double rpmTarget, double rpmActual, DistanceSource source,
                   double x, double y, double headingDeg) {
        this.time = time;
        this.shooter = shooter;
        this.result = result;
        this.distanceIn = distanceIn;
        this.hoodDeg = hoodDeg;
        this.rpmTarget = rpmTarget;
        this.rpmActual = rpmActual;
        this.source = source;
        this.x = x;
        this.y = y;
        this.headingDeg = headingDeg;
    }

    public String toCsv() {
        return String.format(Locale.US, "%s,%s,%s,%.1f,%.1f,%.0f,%.0f,%s,%.1f,%.1f,%.1f",
                time, shooter, result, distanceIn, hoodDeg, rpmTarget, rpmActual, source, x, y, headingDeg);
    }

    /** Parses a line written by toCsv(). Throws IllegalArgumentException if malformed. */
    public static ShotLog parse(String line) {
        String[] f = line.trim().split(",");
        if (f.length != 11) throw new IllegalArgumentException("expected 11 fields: " + line);
        try {
            return new ShotLog(f[0], Shooter.valueOf(f[1]), Result.valueOf(f[2]),
                    Double.parseDouble(f[3]), Double.parseDouble(f[4]),
                    Double.parseDouble(f[5]), Double.parseDouble(f[6]),
                    DistanceSource.valueOf(f[7]),
                    Double.parseDouble(f[8]), Double.parseDouble(f[9]), Double.parseDouble(f[10]));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("bad shot log line: " + line, e);
        }
    }
}
