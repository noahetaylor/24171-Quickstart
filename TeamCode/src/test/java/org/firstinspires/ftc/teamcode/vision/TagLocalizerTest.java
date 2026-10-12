package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.config.LimelightMount;
import org.firstinspires.ftc.teamcode.field.Cell;
import org.firstinspires.ftc.teamcode.field.CellTags;
import org.firstinspires.ftc.teamcode.field.FieldPoint;
import org.firstinspires.ftc.teamcode.tuning.VisionTuning;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TagLocalizerTest {

    /** What the Limelight would report (camera space: x right, y down, z out) for a tag seen from a pose. */
    private static TagLocalizer.TagSeen see(int id, double rx, double ry, double rh, double pan, double tilt) {
        FieldPoint t = CellTags.position(id);
        double lensX = rx + LimelightMount.LENS_FORWARD_IN * Math.cos(rh) - LimelightMount.LENS_LEFT_IN * Math.sin(rh);
        double lensY = ry + LimelightMount.LENS_FORWARD_IN * Math.sin(rh) + LimelightMount.LENS_LEFT_IN * Math.cos(rh);
        double vx = t.x - lensX, vy = t.y - lensY, vz = t.z - LimelightMount.LENS_HEIGHT_IN;
        double yaw = Math.toRadians(pan) + rh;
        double f1 = vx * Math.cos(yaw) + vy * Math.sin(yaw);
        double l = -vx * Math.sin(yaw) + vy * Math.cos(yaw);
        double tr = Math.toRadians(tilt);
        double f = f1 * Math.cos(tr) + vz * Math.sin(tr);
        double u = -f1 * Math.sin(tr) + vz * Math.cos(tr);
        return new TagLocalizer.TagSeen(id, -l, -u, f);
    }

    // ------------------------------------------------------------ tag layout

    @Test
    public void tagOrderFollowsTheSetupGuide() {
        assertTrue(CellTags.position(34).x < CellTags.position(37).x);   // red audience: up toward +x
        assertTrue(CellTags.position(30).x > CellTags.position(33).x);   // red scoring: up toward -x
        assertEquals(13.0, Math.abs(CellTags.position(34).x - CellTags.position(37).x), 1e-9);
        assertNull(CellTags.position(7));
    }

    @Test
    public void blueTagsAreRedTagsRotated() {
        for (int k = 0; k < 4; k++) {
            FieldPoint redA = CellTags.position(Cell.RED_AUDIENCE.firstTagId + k).rotated180();
            FieldPoint blueS = CellTags.position(Cell.BLUE_SCORING.firstTagId + k);
            assertEquals(redA.x, blueS.x, 1e-9);
            assertEquals(redA.y, blueS.y, 1e-9);
            FieldPoint redS = CellTags.position(Cell.RED_SCORING.firstTagId + k).rotated180();
            FieldPoint blueA = CellTags.position(Cell.BLUE_AUDIENCE.firstTagId + k);
            assertEquals(redS.x, blueA.x, 1e-9);
        }
    }

    // ------------------------------------------------------------ localizing

    @Test
    public void recoversThePoseFromAnyViewpoint() {
        Random r = new Random(11);
        int checked = 0;
        for (int i = 0; i < 300; i++) {
            int id = 30 + r.nextInt(16);
            double rx = 15 + 114 * r.nextDouble(), ry = 15 + 114 * r.nextDouble();
            double rh = (r.nextDouble() * 2 - 1) * Math.PI;
            double pan = -60 + 120 * r.nextDouble(), tilt = -10 + 50 * r.nextDouble();
            TagLocalizer.TagSeen t = see(id, rx, ry, rh, pan, tilt);
            if (t.camZ <= 1) continue;   // behind the camera
            double[] p = TagLocalizer.fromTag(t, pan, tilt, rh);
            assertNotNull(p);
            assertEquals("x " + i, rx, p[0], 1e-6);
            assertEquals("y " + i, ry, p[1], 1e-6);
            checked++;
        }
        assertTrue(checked > 100);
    }

    @Test
    public void averagesAgreeingTagsAndRejectsDisagreement() {
        double rx = 40, ry = 30, rh = Math.toRadians(70), pan = 5, tilt = 25;
        List<TagLocalizer.TagSeen> tags = new ArrayList<>();
        for (int id = 34; id <= 37; id++) tags.add(see(id, rx, ry, rh, pan, tilt));
        TagLocalizer.Fix fix = TagLocalizer.locate(tags, pan, tilt, rh);
        assertNotNull(fix);
        assertEquals(4, fix.tags);
        assertEquals(rx, fix.x, 1e-6);
        assertEquals(ry, fix.y, 1e-6);

        // One tag misread by 8 in sideways: the frame is thrown out.
        TagLocalizer.TagSeen bad = tags.get(2);
        tags.set(2, new TagLocalizer.TagSeen(bad.id, bad.camX + 8, bad.camY, bad.camZ));
        assertNull(TagLocalizer.locate(tags, pan, tilt, rh));
    }

    @Test
    public void rejectsATagAtTheWrongHeight() {
        double rh = Math.toRadians(70);
        TagLocalizer.TagSeen t = see(35, 40, 30, rh, 0, 25);
        TagLocalizer.TagSeen low = new TagLocalizer.TagSeen(35, t.camX, t.camY + 20, t.camZ);  // 20 in lower
        assertNull(TagLocalizer.fromTag(low, 0, 25, rh));
    }

    @Test
    public void ignoresNonCellTags() {
        assertNull(TagLocalizer.locate(Arrays.asList(new TagLocalizer.TagSeen(7, 0, 0, 50)), 0, 0, 0));
    }

    // ------------------------------------------------------------ correcting

    @Test
    public void correctionBlendsAndRejects() {
        TagLocalizer.Fix fix = new TagLocalizer.Fix(50, 40, 3, 60);
        double[] c = TagLocalizer.correct(46, 40, fix, 0, 0);
        assertEquals(46 + VisionTuning.TAG_GAIN * 4, c[0], 1e-9);
        assertEquals(40, c[1], 1e-9);
        assertNull("moving", TagLocalizer.correct(46, 40, fix, 40, 0));
        assertNull("turning", TagLocalizer.correct(46, 40, fix, 0, 90));
        assertNull("wild jump", TagLocalizer.correct(20, 40, fix, 0, 0));
        assertNull("too far", TagLocalizer.correct(46, 40, new TagLocalizer.Fix(50, 40, 3, 200), 0, 0));
        assertNull("no fix", TagLocalizer.correct(46, 40, null, 0, 0));
    }
}
