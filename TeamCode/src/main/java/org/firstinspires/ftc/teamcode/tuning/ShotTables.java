package org.firstinspires.ftc.teamcode.tuning;

import org.firstinspires.ftc.teamcode.shooting.ShotTable;
import org.firstinspires.ftc.teamcode.shooting.Shooter;

/**
 * THE PLACE TO TUNE SHOTS. One table per shooter: distance (inches, from the
 * release point to the CELL opening, measured flat on the floor) -> hood
 * angle (deg) -> flywheel rpm. Two or more rows switch that distance range
 * from physics to the table.
 *
 * Easiest: the Shot Table Editor (tools/shot-table-editor) plots the shots
 * logged by the Shot Tuner OpMode and rewrites the rows between the
 * BEGIN/END markers below when you click Save. Hand edits are fine too;
 * keep each row as  .add(distanceIn, hoodDeg, rpm)  and keep the markers.
 */
public final class ShotTables {
    private ShotTables() {}

    // BEGIN POLLEN ROWS
    public static final ShotTable POLLEN = new ShotTable()
            ;
    // END POLLEN ROWS

    // BEGIN NECTAR ROWS
    public static final ShotTable NECTAR = new ShotTable()
            ;
    // END NECTAR ROWS

    public static ShotTable forShooter(Shooter s) {
        return s == Shooter.POLLEN ? POLLEN : NECTAR;
    }
}
