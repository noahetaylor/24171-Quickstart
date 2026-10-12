# Driver Controls

Driver Hub → group **Competition** → **Red TeleOp** or **Blue TeleOp**.
If Auto ran first, TeleOp continues from where Auto left the robot. If not
(practice), place the robot's center on the practice start spot: tile A2
for red, facing the blue wall. Blue's spot is the same, rotated (tile F5,
facing the red wall).

## Gamepad 1

| Control | What it does |
|---|---|
| Left stick | Drive (robot-centric: up = robot's front) |
| Right stick X | Turn |
| **Right trigger** (hold) | **Aim + fire from here.** You keep driving; the robot turns onto the up CELL, spins both flywheels, and feeds when both balls will go in. |
| **Left trigger** (hold) | **Pre-align.** The robot drives itself to the nearest shot spot, faces the CELL, and fires. Let go to take back control instantly. |
| RB (hold) | Intake |
| LB (hold) | Eject |
| B (hold) | Feed without aiming (unjam) |
| X | Flywheel idle spin on/off (on = faster first shot) |
| Y | Our HIVE just tipped (until the camera tracks it) |
| Back | Reset HIVEs to the match-start state |
| D-pad up / down / left / right | Ramp bar: stow / down / block / open |

Rumble: one long rumble at **1:00 left** (FLOWER scoring opens), three
blips at **0:20 left**.

## What telemetry shows

- **Shot** says what the robot is waiting for:
  - `TURNING`: not lined up yet
  - `SPINNING_UP`: flywheels not at speed
  - `MOVING_TOO_FAST`: slow down
  - `NO_SHOT`: too close or too far
  - `READY`: shooting

  `FEEDING` means balls are going up.
- **Target** shows which CELL the robot thinks is up. Press **Y** when our
  HIVE tips.
- **Not in config** lists any mechanism missing from the robot
  configuration. That part is skipped.
