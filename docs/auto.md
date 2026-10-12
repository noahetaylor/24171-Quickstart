# Autonomous

Driver Hub group **Competition** has **Red Auto** and **Blue Auto**. Each
one pre-selects the matching TeleOp. Everything is written for red and
rotated for blue.

## INIT choices (gamepad 1)

| Button | Choice |
|---|---|
| D-pad left / right | Start spot. Telemetry shows the pose and warns if the spot breaks G304. |
| Y | Garden ON / OFF. Turn it off when our partner's Auto takes the garden. |
| D-pad up / down | Start delay, in 0.5 s steps |

Load **4 POLLEN**. Place the robot's center on the spot shown.

Start spots are placeholders until the team picks real ones. They're in
`auto/StartSpot.java`, and a unit test checks every spot against G304:
- our side
- touching a wall
- not in the LOADING ZONE
- not touching a FLOWER
- not on the garden

## What it does

`auto/AutoBrain` picks each next job:

1. **Shoot the preloads.**
   - Drive to the nearest shot spot and aim.
   - Fire 3. The starting CELL already holds 3 NECTAR, so 3 POLLEN tips
     it.
   - Point the Limelight at our HIVE to **confirm the tip from the tags**.
     If the tags aren't seen within 1 s, assume the tip happened.
   - Fire the 4th into the new up CELL.
2. **Garden** (if ON): line up facing the audience wall, drive in with the
   intake running, back out. Counted as 4 balls.
3. **Shoot** when full, or when holding enough to tip.
4. **Vision pickups:**
   - Sweep the camera, pick the best ball (POLLEN or our NECTAR, our half
     only), drive the intake over it.
   - If nothing is seen, turn 60° and look again, up to 3 tries.
5. **Shoot** what's left. Stop with time to spare.

The second tip needs about 8 POLLEN in the new CELL. That's 1 (4th
preload) + 4 (garden) + 3 more from pickups. That's tight in 30 s. A
partner shooting our HIVE makes it easier, and the tag check keeps us
right about which CELL is up either way.

## Without a ball sensor

- **Balls held is a count.** Every pickup attempt is assumed to succeed.
  The garden counts as `GARDEN_BALLS`.
- **Feeding runs on a timer:** `FEED_PER_BALL_S` × balls, plus
  `FEED_EXTRA_S`.
- **The CELL fill is our own estimate** (`auto/CellLedger`: POLLEN = 1,
  NECTAR ≈ 1.7, tip at 8). The camera corrects it whenever it confirms a
  tip.

If the team adds a sensor later, the count is the one thing to change.

## Tuning (`tuning/AutoTuning.java`, also live in Panels)

| What | Settings |
|---|---|
| Garden line-up and push | `GARDEN_X`, `GARDEN_APPROACH_Y`, `GARDEN_PUSH_Y`, `GARDEN_PUSH_S`. Placeholders: test on a field. |
| Feed timing | `FEED_PER_BALL_S`. Measure how long one ball takes to go up. |
| Time budget | `SHOOT_RESERVE_S`, `GARDEN_MIN_S`, `COLLECT_MIN_S` |
| Pickup | `PICKUP_STANDOFF_IN`: how far short of the ball the robot center stops |

## Not yet handled

- **An early tip by our partner.** If the partner tips our HIVE before we
  expect it, the tags only catch it at our next tip check.
- **Pickups that miss** are counted anyway.
- **Driving around robots:** routes avoid the HIVE frame only, not other
  robots.
