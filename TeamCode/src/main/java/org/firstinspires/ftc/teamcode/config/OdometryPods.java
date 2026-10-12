package org.firstinspires.ftc.teamcode.config;

/**
 * Turns where the two odometry pods physically are (OdometryGeometry, edited
 * with the Pod Placer tool) into the values Pedro's two-wheel localizer
 * wants. All the sign conventions live here, in one tested place, so
 * nobody has to reason about them when a pod moves.
 *
 * Plain Java (no Pedro/Android types) so OdometryPodsTest can cover it.
 */
public final class OdometryPods {
    private OdometryPods() {}

    /** Arrow directions, degrees counter-clockwise from the robot's front. */
    public static final int FRONT = 0, LEFT = 90, BACK = 180, RIGHT = 270;

    /**
     * One pod as it physically sits on the robot.
     *
     * @param encoderName  hardwareMap name of the motor port the pod plugs into
     * @param forwardIn    inches in front of robot center (negative = behind)
     * @param leftIn       inches left of robot center (negative = right)
     * @param arrowDeg     which way the ROBOT moves to make this pod's raw
     *                     count go UP (FRONT/LEFT/BACK/RIGHT). This also says
     *                     which axis the wheel rolls along.
     */
    public static final class Placement {
        public final String encoderName;
        public final double forwardIn, leftIn;
        public final int arrowDeg;

        public Placement(String encoderName, double forwardIn, double leftIn, int arrowDeg) {
            if (arrowDeg != FRONT && arrowDeg != LEFT && arrowDeg != BACK && arrowDeg != RIGHT) {
                throw new IllegalArgumentException(encoderName + ": arrow must be 0, 90, 180 or 270, got " + arrowDeg);
            }
            this.encoderName = encoderName;
            this.forwardIn = forwardIn;
            this.leftIn = leftIn;
            this.arrowDeg = arrowDeg;
        }

        boolean rollsForward() {
            return arrowDeg == FRONT || arrowDeg == BACK;
        }
    }

    /** Pedro-ready values. Directions are +1 / -1, same as Pedro's Encoder.FORWARD / REVERSE. */
    public static final class Layout {
        public final String forwardPodName, strafePodName;
        public final double forwardPodY, strafePodX;
        public final double forwardPodDir, strafePodDir;

        Layout(String forwardPodName, String strafePodName, double forwardPodY, double strafePodX,
               double forwardPodDir, double strafePodDir) {
            this.forwardPodName = forwardPodName;
            this.strafePodName = strafePodName;
            this.forwardPodY = forwardPodY;
            this.strafePodX = strafePodX;
            this.forwardPodDir = forwardPodDir;
            this.strafePodDir = strafePodDir;
        }
    }

    /**
     * Works out which pod is the forward pod and which is the strafe pod
     * from their arrows, so the pods can be plugged into either port.
     *
     * @throws IllegalArgumentException unless exactly one pod rolls
     *         front-to-back and the other side-to-side
     */
    public static Layout resolve(Placement a, Placement b) {
        if (a.rollsForward() == b.rollsForward()) {
            throw new IllegalArgumentException("Need one front-to-back pod and one side-to-side pod; "
                    + a.encoderName + " and " + b.encoderName + " both roll "
                    + (a.rollsForward() ? "front-to-back" : "side-to-side"));
        }
        Placement forward = a.rollsForward() ? a : b;
        Placement strafe = a.rollsForward() ? b : a;

        // Pedro wants each pod to count UP when the robot moves +X (forward)
        // or +Y (left); a pod whose count goes up the other way is reversed.
        double forwardDir = forward.arrowDeg == FRONT ? 1 : -1;
        double strafeDir = strafe.arrowDeg == LEFT ? 1 : -1;

        // Pedro's TwoWheelLocalizer computes  forward = ticks - forwardPodY * dHeading.
        // Turning counter-clockwise moves a LEFT-side pod backward, so that
        // correction only cancels the turn if forwardPodY is positive to the
        // RIGHT: forwardPodY = -leftIn. (strafeX uses the normal forward-positive
        // sign: strafe = ticks - strafePodX * dHeading.) Confirm with Tuning >
        // Offsets Tuner; if it disagrees, flip this one line.
        double forwardPodY = -forward.leftIn;
        double strafePodX = strafe.forwardIn;

        return new Layout(forward.encoderName, strafe.encoderName, forwardPodY, strafePodX, forwardDir, strafeDir);
    }
}
