# Vision (Limelight 3A)

The Limelight sits on pan/tilt servos and does two jobs, one at a time:

| Mode | Pipeline slot | Camera points | Gives |
|---|---|---|---|
| **Balls** | 0, Python (`TeamCode/src/main/assets/limelight/ball_pipeline.py`) | down, ahead of the intake | every POLLEN / red NECTAR / blue NECTAR, located on the field |
| **Tags** | 1, AprilTag (36h11) | up at our HIVE | which CELL is up (the visible tag cluster is the up CELL), and a correction to the robot's position |

Code: `subsystems/Vision` (gimbal + pipelines), `vision/` (locating and
picking balls). Settings: `config/LimelightMount` (position, servo
calibration) and `tuning/VisionTuning`.

## One-time setup

1. **Driver Hub configuration:** the Limelight shows up as an Ethernet
   device. Name it `limelight`. Name the gimbal servos `limelightPan` and
   `limelightTilt` (see [port-map.md](port-map.md)).
2. **Limelight web page** (http://limelight.local:5801 with the laptop on
   the robot's network, or over USB):
   - **Slot 0:** set the pipeline type to **Python**.
   - **Slot 1:** set the type to **Fiducial / AprilTag**, family **36h11**,
     tag size **82.55 mm** (3.25 in). The size is what makes the tag
     distances right.
3. **Upload the ball pipeline:** run **Limelight Setup** (Testing group).
   Re-run it whenever `ball_pipeline.py` changes.

## Calibrate (Limelight Check, Testing group)

1. **Gimbal:** nudge with the d-pad until the camera looks straight ahead
   and level.
   - If telemetry then doesn't read pan 0 / tilt 0, fix
     `PAN_CENTER_POS` / `TILT_LEVEL_POS` in `config/LimelightMount`.
   - Then aim at something 45° off and fix the `*_POS_PER_DEG` values.
2. **Mount position:** measure from the robot's center to the lens
   (forward, left, height) and enter the results in `config/LimelightMount`.
3. **Ball locations:**
   - Put a ball on a known spot.
   - Telemetry should place it within about 2 in.
   - If it's off, recheck the lens height and the tilt calibration.
4. **Colors:** under event lighting, view the mask on the Limelight web
   page. Adjust the HSV ranges at the top of `ball_pipeline.py`, then
   re-run Limelight Setup.
5. **Tags:**
   - Press **Y**. The camera should point at our HIVE and show the up
     CELL's tags.
   - Measure the tag row's height under an up CELL. Put it in
     `field/CellTags.TAG_HEIGHT_IN`, with the row's offset in `TAG_ROW_Y_OFFSET_IN`.
6. **Position correction:** in tag mode, telemetry shows **Tag fix** and
   how far it is from odometry.
   - Parked still, it should agree within about 2 in.
   - **X** turns on applying the fixes.
   - If the fix never appears while tags are visible, the 4 tags disagree.
     The tag order in `CellTags` (taken from the setup guide figure) or
     the tag size is probably wrong.

## Position correction from the CELL tags

The tags move when the HIVE tips, but in each stable state they're in a
known place, and the visible cluster tells us which state it is. Each
tag's camera-space position, the gimbal angles and the gyro heading give
the robot's x/y (`vision/TagLocalizer`).

**A fix is used only when:**
- all the tags in the frame agree within `MAX_TAG_SPREAD_IN`, and each
  is at the right height
- the robot is nearly still and the tags are close enough
- the fix isn't a wild jump from odometry (`TAG_MAX_JUMP_IN`)

Odometry then moves `TAG_GAIN` of the way toward the fix.

**When it runs:**
- TeleOp keeps the camera on our HIVE all match. It tracks the up CELL
  and corrects position.
- Auto watches the tags while lining up and shooting. That's also how it
  notices a tip, ours or our partner's.

## How balls are judged

- **Opposing NECTAR is ignored.** Only POLLEN and NECTAR of our color are
  wanted.
- **Size check:** a ball whose apparent size doesn't match its distance is
  dropped (`SIZE_TOLERANCE`). This keeps tape lines and bumpers out.
- **In Auto,** balls on the other side of the field are skipped, to stay
  `OWN_SIDE_MARGIN_IN` inside our half. Balls under the HIVE frame or
  jammed against a wall are skipped too.
- **Choosing among the rest:** the robot picks the cheapest to reach,
  counting distance plus a turn penalty.

## Testing the pipeline on a laptop

```
pip install opencv-python-headless numpy
python tools/limelight/test_ball_pipeline.py
```
It runs the pipeline on generated images: three colors, tape lines,
wiffle-ball holes, and too many balls.
