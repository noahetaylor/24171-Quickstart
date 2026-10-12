package org.firstinspires.ftc.teamcode.field;

/** Field areas to keep the robot out of (e.g. where our partner's Auto runs). */
public interface Blocker {

    /** True if the robot's center may not be at (x, y). */
    boolean pointBlocked(double x, double y);

    /** True if driving straight from a to b would put the robot in a blocked area. */
    boolean segmentBlocked(double ax, double ay, double bx, double by);

    Blocker NONE = new Blocker() {
        @Override
        public boolean pointBlocked(double x, double y) {
            return false;
        }

        @Override
        public boolean segmentBlocked(double ax, double ay, double bx, double by) {
            return false;
        }
    };
}
