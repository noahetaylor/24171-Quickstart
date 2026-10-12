# Autonomous

Driver Hub group **Competition** has **Red Auto** and **Blue Auto**. Each
one pre-selects the matching TeleOp. Everything is written for red and
rotated for blue.

## INIT choices (gamepad 1)

Two pages. **BACK** switches between them.

### Setup page

| Button | Choice |
|---|---|
| D-pad left / right | Start spot. Telemetry shows the pose and warns if the spot breaks G304. |
| Y | Garden ON / OFF. Turn it off when our partner's Auto takes the garden. |
| D-pad up / down | Start delay, in 0.5 s steps |

Load **4 POLLEN**. Place the robot's center on the spot shown.

### Partner page: keep out of our partner's way

Before each match, ask your partner where their Auto drives and mark
those tiles. The grid is drawn **as your drive team sees the field from
its own wall**: your nearest column at the bottom, your left on the left.

```
far wall <--          --> audience
C  .  .  .  .  .  .
B  .  .  #  #  .  .
A  . [.] .  1  1  .
      (our wall / drive team)
```

| Button | Does |
|---|---|
| D-pad | Move the cursor |
| A | Cycle the tile: `.` free → `#` whole Auto → `1` 0–10 s → `2` 10–20 s → `3` 20–30 s |
| B | Clear all |

**While a tile is marked, Auto won't:**
- chase a ball in it
- chase a ball whose straight path crosses it
- pick a shot spot in it, or drive through it to one
- run the garden, if the garden is in a marked tile

A robot-sized margin is kept around marked tiles
(`PartnerPlan.MARGIN_IN`). Starting inside one is fine: the robot is
allowed to drive out.

The plan **stays set until you change it**. The setup page shows how
many tiles are marked, so check it every match: partners change. It's
the same plan for red or blue, drawn from your own side.

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
   - **Position lock first.** If the last tag fix is more than
     `RELOCK_AFTER_S` old and the HIVE is within the gimbal's reach, take
     a quick tag fix. Ball positions are computed from our pose, so a
     drifted pose would send us to the wrong spot.
   - **Search.** Three stepped looks: straight, left, right. The camera
     holds still for each, because sweeping smears the angle. Pick the
     best ball: POLLEN or our NECTAR, our half only.
   - **Drive the intake over it.** On the way the camera stays on the ball
     and re-steers if it moved (knocked, or first seen from far away).
   - If nothing is seen, turn 60° and look again, up to 3 tries.
5. **Shoot** what's left. Stop with time to spare.

The second tip needs about 8 POLLEN in the new CELL. That's 1 (4th
preload) + 4 (garden) + 3 more from pickups. That's tight in 30 s. A
partner shooting our HIVE makes it easier, and the tag check keeps us
right about which CELL is up either way.

## The Limelight's two jobs

| When | Mode | For |
|---|---|---|
| Lining up, shooting, tip check | Tags | which CELL is up; position fix |
| Just before a ball search, if the last fix is old | Tags (LOCK, at most `LOCK_S`) | position fix |
| Searching, driving to a ball | Balls | where the balls are |

After every pipeline switch or big gimbal swing, frames are ignored for a
moment (`VisionTuning.SWITCH_SETTLE_S`, `GIMBAL_SETTLE_S`). Otherwise
something could be located using the old camera angle.

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

- **An early tip by our partner** is caught only when the camera is on the
  tags, which is while lining up and shooting.
- **Pickups that miss** are counted anyway.
- **Driving around robots:** routes avoid the HIVE frame and the partner
  plan's tiles. They don't see robots.
