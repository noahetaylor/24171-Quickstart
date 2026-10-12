# Shot Model

Code: `TeamCode/.../shooting/` (logic) and `TeamCode/.../tuning/` (the numbers). Given the robot's pose and a target (normally
the up CELL from `HiveState`), it works out:
- **which heading to face**, allowing for the shooters being off-center
- **the distance** from the ball's release point to the target, along the floor
- **the hood angle and flywheel rpm**
- **whether the shot is possible**, and whether the ball is already falling
  when it reaches the CELL

## Where the numbers come from

1. **`tuning/ShotTables.java`, the measured table.** Distance → hood → rpm, from
   shots that actually scored. If the table covers the distance (needs at
   least 2 rows), its values are used.
2. **Physics, the starting guess.** Simple ball flight without air drag.
   POLLEN and NECTAR have holes and real drag, so expect practice to need
   **more** rpm than physics says.

Telemetry shows `TABLE` or `PHYSICS` so you can tell which one is in use.

Physics starting values for the fixed 60° hood, ball rising 38.6 in to the
CELL opening:

| distance | 3 ft | 4 ft | 6 ft | 8 ft | 10 ft |
|---|---|---|---|---|---|
| rpm | ~2770 (still rising) | ~2690 | ~2910 | ~3180 | ~3450 |

## Auto-aim

`shooting/AutoAim` turns the robot onto the shot, sets the flywheel rpm, and
says when to fire. It judges the shot at where the robot **will** point
`FIRE_LATENCY_S` after the trigger, so it can fire while still settling. It
also leads shots taken while moving. Gains and fire rules are in
`tuning/AimTuning.java`.

## Filling the table at practice

1. Run **Shot Planner Check** (TeleOp list). In INIT, pick the alliance and
   place the robot's center on the start spot it shows, facing the far wall.
2. Drive to a spot. Turn by the "turn" amount shown, so the shooter points
   at the CELL.
3. Run the flywheel at the rpm shown and adjust until shots go in reliably.
4. Add a row in `tuning/ShotTables.java`: `.add(distance, hood, rpm)`. Spread rows
   across the distances you actually shoot from.

## Settings in `tuning/ShooterTuning.java`

These can also be changed live from Panels in the pit (not at matches: R704).
Write the final values back into the file, or they reset on the next deploy.

| Setting | Now | Notes |
|---|---|---|
| `LAUNCH_YAW_DEG` | 0 (out the front) | Confirmed by the team. |
| `HOOD_MIN_DEG` / `HOOD_MAX_DEG` | 60 / 60 | Fixed hood: both equal to the fitted print. For the adjustable hood, set the range (e.g. 50 / 70). |
| `EXIT_EFFICIENCY` | 0.5 | Ball speed ÷ wheel surface speed. |
| `MAX_RPM` | 5100 | Below the 5203's ~6000 free speed. |
| `FIRE_LATENCY_S` | 0.15 | Time from trigger to the ball leaving the hood. Measure it; auto-aim will use it. |

Release points come from the team CAD (BIOBUZZ repo, `TV2_*` SIMPLE
heads). POLLEN leaves on the robot's right and NECTAR on the left, both
about 7.2 in behind center and 15 in up. Update them if the shooter
hardware changes.
