"""Tests for the Limelight ball pipeline on generated images.

Needs OpenCV + numpy (the Limelight has them; on a laptop:
pip install opencv-python-headless numpy). Run:
    python tools/limelight/test_ball_pipeline.py
"""

import importlib.util
import math
import unittest
from pathlib import Path

import cv2
import numpy as np

PIPELINE = Path(__file__).resolve().parents[2] / "TeamCode/src/main/assets/limelight/ball_pipeline.py"
spec = importlib.util.spec_from_file_location("ball_pipeline", PIPELINE)
bp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bp)

W, H = 640, 480
GREY_FLOOR = (90, 90, 90)
BGR = {"yellow": (30, 220, 240), "red": (40, 40, 210), "blue": (200, 90, 30)}


def wiffle(img, center, r, color):
    """A ball with the dark holes a wiffle ball has."""
    cv2.circle(img, center, r, BGR[color], -1)
    for k in range(6):
        a = k * math.pi / 3
        hx = int(center[0] + 0.5 * r * math.cos(a))
        hy = int(center[1] + 0.5 * r * math.sin(a))
        cv2.circle(img, (hx, hy), max(2, r // 6), (25, 25, 25), -1)


def blank():
    return np.full((H, W, 3), GREY_FLOOR, np.uint8)


class BallPipelineTest(unittest.TestCase):

    def test_finds_and_labels_each_color(self):
        img = blank()
        wiffle(img, (160, 380), 30, "yellow")
        wiffle(img, (320, 300), 24, "red")
        wiffle(img, (500, 250), 20, "blue")
        _, _, out = bp.runPipeline(img, [])
        self.assertEqual(3, int(out[0]))
        classes = [int(out[1 + 4 * i + 2]) for i in range(3)]
        self.assertEqual([1, 2, 3], classes)   # nearest (lowest) first

    def test_angles_have_the_limelight_signs(self):
        img = blank()
        wiffle(img, (560, 100), 20, "yellow")   # right of center, above center
        _, _, out = bp.runPipeline(img, [])
        tx, ty = out[1], out[2]
        self.assertGreater(tx, 0)
        self.assertGreater(ty, 0)
        # Pinhole: x at 240 px right of center with f = 320 / tan(27.25 deg).
        f = 320 / math.tan(math.radians(bp.HFOV_DEG / 2))
        self.assertAlmostEqual(math.degrees(math.atan(240 / f)), tx, delta=0.5)

    def test_ignores_tape_lines_and_specks(self):
        img = blank()
        cv2.rectangle(img, (0, 420), (W, 440), BGR["red"], -1)   # red tape across the floor
        cv2.circle(img, (300, 200), 3, BGR["yellow"], -1)        # speck
        wiffle(img, (300, 300), 25, "blue")
        _, _, out = bp.runPipeline(img, [])
        self.assertEqual(1, int(out[0]))
        self.assertEqual(3, int(out[3]))

    def test_holes_do_not_split_a_ball(self):
        img = blank()
        wiffle(img, (320, 240), 40, "yellow")
        _, _, out = bp.runPipeline(img, [])
        self.assertEqual(1, int(out[0]))
        self.assertAlmostEqual(math.degrees(math.atan(40 / (320 / math.tan(math.radians(bp.HFOV_DEG / 2))))),
                               out[4], delta=0.6)

    def test_output_fits_limelight_limit(self):
        img = blank()
        for i in range(10):
            wiffle(img, (40 + 60 * i, 120 + 30 * (i % 3)), 18, "yellow")
        _, _, out = bp.runPipeline(img, [])
        self.assertEqual(bp.MAX_BALLS, int(out[0]))
        self.assertLessEqual(len(out), 32)

    def test_empty_image(self):
        largest, _, out = bp.runPipeline(blank(), [])
        self.assertEqual([0.0], out)


if __name__ == "__main__":
    unittest.main()
