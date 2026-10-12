# Limelight 3A Python pipeline: finds POLLEN (yellow) and NECTAR (red, blue).
#
# The robot uploads this file to the Limelight's pipeline slot 0 with the
# "Limelight Setup" OpMode (Testing group); you can also paste it into the
# Limelight web page. Robot-side parser: vision/LimelightBalls.java. Keep
# the output format in sync with it.
#
# Output (llpython, up to 32 numbers):
#   [count, then per ball (nearest first, at most 7):
#    tx_deg, ty_deg, class, angular_radius_deg]
#   tx: + = right of image center. ty: + = above image center.
#   class: 1 = POLLEN (yellow), 2 = red NECTAR, 3 = blue NECTAR.

import math

import cv2
import numpy as np

# Limelight 3A field of view (degrees).
HFOV_DEG = 54.5
VFOV_DEG = 42.0

# HSV ranges, OpenCV scale (H 0-179, S and V 0-255). Tune on the real balls
# under event lighting: Limelight web page -> this pipeline -> view "mask".
YELLOW = [((18, 90, 90), (38, 255, 255))]
RED = [((0, 120, 70), (8, 255, 255)), ((170, 120, 70), (179, 255, 255))]
BLUE = [((98, 120, 60), (128, 255, 255))]
CLASSES = ((1, YELLOW), (2, RED), (3, BLUE))

MIN_AREA_PX = 60          # smaller blobs are noise or too far to chase
MIN_FILL = 0.45           # blob area / enclosing-circle area: rejects tape lines and edges
MAX_BALLS = 7
CLOSE_KERNEL = np.ones((7, 7), np.uint8)   # fills the wiffle-ball holes

COLORS_BGR = {1: (0, 255, 255), 2: (0, 0, 255), 3: (255, 0, 0)}


def find_balls(image):
    """Returns a list of (tx_deg, ty_deg, cls, ang_r_deg, contour), nearest (lowest in image) first."""
    h, w = image.shape[:2]
    fx = (w / 2.0) / math.tan(math.radians(HFOV_DEG / 2))
    fy = (h / 2.0) / math.tan(math.radians(VFOV_DEG / 2))
    hsv = cv2.cvtColor(cv2.GaussianBlur(image, (5, 5), 0), cv2.COLOR_BGR2HSV)
    found = []
    for cls, ranges in CLASSES:
        mask = None
        for lo, hi in ranges:
            m = cv2.inRange(hsv, np.array(lo, np.uint8), np.array(hi, np.uint8))
            mask = m if mask is None else cv2.bitwise_or(mask, m)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, CLOSE_KERNEL)
        mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        for c in contours:
            area = cv2.contourArea(c)
            if area < MIN_AREA_PX:
                continue
            (cx, cy), r = cv2.minEnclosingCircle(c)
            if r <= 0 or area / (math.pi * r * r) < MIN_FILL:
                continue
            tx = math.degrees(math.atan((cx - w / 2.0) / fx))
            ty = math.degrees(math.atan((h / 2.0 - cy) / fy))
            ang_r = math.degrees(math.atan(r / fx))
            found.append((tx, ty, cls, ang_r, c, (int(cx), int(cy), int(r))))
    found.sort(key=lambda b: b[1])   # lowest in the image = nearest to the robot
    return found[:MAX_BALLS]


def runPipeline(image, llrobot):
    balls = find_balls(image)
    llpython = [float(len(balls))]
    largest = np.array([[]])
    best_area = 0
    for tx, ty, cls, ang_r, c, (cx, cy, r) in balls:
        llpython += [tx, ty, float(cls), ang_r]
        cv2.circle(image, (cx, cy), r, COLORS_BGR[cls], 2)
        a = cv2.contourArea(c)
        if a > best_area:
            best_area, largest = a, c
    return largest, image, llpython
