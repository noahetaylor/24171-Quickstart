package org.firstinspires.ftc.teamcode.tuning;

import org.firstinspires.ftc.teamcode.shooting.ShotTable;
import org.firstinspires.ftc.teamcode.shooting.Shooter;

/**
 * THE PLACE TO TUNE SHOTS. One table per shooter: distance (inches, from the
 * release point to the CELL opening, measured flat on the floor) -> hood
 * angle (deg) -> flywheel rpm.
 *
 * How to fill it at practice: park at a known distance (Shot Planner
 * telemetry shows it), adjust rpm until shots score reliably, write the
 * row here. Two or more rows switch that distance range from physics to
 * the table. Keep the hood value equal to the fitted hood print while the
 * hood is fixed.
 *
 * Example:  .add(48, 60, 3100)
 */
public final class ShotTables {
    private ShotTables() {}

    public static final ShotTable POLLEN = new ShotTable();
    // .add(distanceIn, hoodDeg, rpm)

    public static final ShotTable NECTAR = new ShotTable();
    // .add(distanceIn, hoodDeg, rpm)

    public static ShotTable forShooter(Shooter s) {
        return s == Shooter.POLLEN ? POLLEN : NECTAR;
    }
}
