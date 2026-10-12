package org.firstinspires.ftc.teamcode.shooting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Measured shots: horizontal distance -> hood angle + flywheel rpm, linearly
 * interpolated between rows. Rows are what actually scored at practice; they
 * win over physics wherever they cover the distance.
 */
public final class ShotTable {

    public static final class Row {
        public final double distanceIn;
        public final double hoodDeg;
        public final double rpm;

        public Row(double distanceIn, double hoodDeg, double rpm) {
            this.distanceIn = distanceIn;
            this.hoodDeg = hoodDeg;
            this.rpm = rpm;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public ShotTable add(double distanceIn, double hoodDeg, double rpm) {
        rows.add(new Row(distanceIn, hoodDeg, rpm));
        Collections.sort(rows, new Comparator<Row>() {
            @Override
            public int compare(Row a, Row b) {
                return Double.compare(a.distanceIn, b.distanceIn);
            }
        });
        return this;
    }

    public int size() {
        return rows.size();
    }

    /** True if the distance lies within the measured rows (needs at least two). */
    public boolean covers(double distanceIn) {
        return rows.size() >= 2
                && distanceIn >= rows.get(0).distanceIn
                && distanceIn <= rows.get(rows.size() - 1).distanceIn;
    }

    /** Interpolated row for a covered distance; null if not covered. */
    public Row lookup(double distanceIn) {
        if (!covers(distanceIn)) return null;
        for (int i = 1; i < rows.size(); i++) {
            Row hi = rows.get(i);
            if (distanceIn <= hi.distanceIn) {
                Row lo = rows.get(i - 1);
                double span = hi.distanceIn - lo.distanceIn;
                double t = span <= 0 ? 0 : (distanceIn - lo.distanceIn) / span;
                return new Row(distanceIn,
                        lo.hoodDeg + t * (hi.hoodDeg - lo.hoodDeg),
                        lo.rpm + t * (hi.rpm - lo.rpm));
            }
        }
        return null; // unreachable: covers() checked the range
    }
}
