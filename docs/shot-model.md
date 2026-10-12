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

1. **Robot: run Shot Tuner** (Driver Hub → Testing). Pick the alliance in
   INIT. Load only the ball type for the shooter you're testing (**X**
   switches POLLEN / NECTAR).
2. **Shoot from several distances.**
   - **Y** sets the rpm to what the model suggests; the d-pad changes it in
     steps of 50.
   - **A** fires.
   - For each ball, press **RB** if it went in, **LB** if it missed.
     **B** undoes the last one.
   - Distance comes from odometry. If odometry isn't trusted yet, press
     **back** for TAPE mode and set the distance with d-pad left/right.
3. **Laptop: open the Shot Table Editor.** Double-click
   `tools/shot-table-editor/Shot Table Editor.bat`.
   - **Pull from robot** copies the logs over. The laptop must be on the
     robot's Wi-Fi, or plugged in by USB. You can also drag CSV files in.
   - Hits show as green dots, misses as red crosses, and physics as a
     dashed line.
   - Drag the blue table rows to where the green is thickest, or click
     **Suggest rows from hits**.
   - **Save** rewrites only the rows in `tuning/ShotTables.java`. Then build
     and deploy.
4. **Commit `data/shot-logs/`** along with the table change.

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
