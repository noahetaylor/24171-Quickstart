# Architecture & Progress Notes

Living reference for how the code is organized and why, plus known gaps to
close once the season/robot are real. Update this as things change — treat
it as more durable than chat history.

## Code structure: subsystem pattern

Moved off last year's single-file "God OpMode" onto a subsystem pattern:

- **`RobotHardware`** — owns hardware device references and does the
  one-time `hardwareMap` lookups (names, directions, brake modes). No
  control logic belongs here — just declarations + init.
- **Subsystem classes** (e.g. `Drivetrain`) — take `RobotHardware` in their
  constructor, expose clean behavior methods (`driveRobotCentric(...)`,
  `stop()`, etc.). Pure logic, no hardware lookups of their own.
- **OpModes** (TeleOp/Auto) — instantiate `RobotHardware` once, build each
  subsystem off it, call subsystem methods from gamepad input or
  autonomous logic.

Adding a new mechanism (e.g. a shooter) later means: add its device(s) to
`RobotHardware`, write a new subsystem class following the `Drivetrain`
shape, wire it into the relevant OpMode(s). Nothing existing needs
restructuring — this was the point of the refactor.

Current files (`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`).
The README's "Where things are" section has the short version.
- `opmodes/competition/` — Red/Blue Auto and TeleOp (Driver Hub group
  "Competition")
- `opmodes/testing/` — testing and data-gathering OpModes (group "Testing")
- `opmodes/examples/` — `AutoTemplate` (swap-point Pedro auto skeleton) and
  `TeleOpTemplate` (minimal subsystem TeleOp)
- `tuning/` — every number tuned from testing (shot tables, shooter and
  aim settings), editable live from Panels in the pit
- `config/` — physical facts: hardware names, odometry pod layout
- `field/` — field model (positions, alliance conversion, HIVE state);
  `docs/field-model.md`
- `shooting/` — shot planner + auto-aim logic; `docs/shot-model.md`
- `subsystems/RobotHardware.java`, `subsystems/Drivetrain.java` —
  robot-centric mecanum driving only so far
- `replay/` — record-and-replay framework

## Pedro Pathing status

- Pinned at v2.1.2 in `build.dependencies.gradle`.
- Workflow: draw + export paths at https://visualizer.pedropathing.com/,
  paste the generated `Paths` class body into `opmodes/examples/AutoTemplate.java`.
- `Constants.java` is configured (Oct 2026): mecanum drivetrain + the
  **two-wheel localizer** (2 goBILDA 4-Bar pods + Control Hub IMU). All
  names, directions, and pod measurements live in
  `config/HardwareConfig.java`, which `RobotHardware` also reads, so TeleOp
  and Pedro can't disagree. Wiring: `docs/port-map.md`. Bring-up steps:
  `docs/odometry-setup.md` (starts with our `OdometryCheck` OpMode).
- Still Pedro defaults until tuned on the robot: pod ticks-per-inch
  (goBILDA spec value), drivetrain velocities, PIDF.

## virtual_robot (simulator) testing

- Two bot configs exist: **Mecanum Bot** (generic stock demo, not ours)
  and **STP Bot** (your team's actual custom config, on its own
  `StpPhysicsBase` — not just a reskinned `MecanumBot`).
- **Gotcha:** this fork's OpMode dropdown is hardcoded
  (`VirtualRobotController.java` ~line 492) to only show OpModes with
  `group = "STP"` — regardless of which bot is selected. Any new sim
  OpMode needs `group = "STP"` on its `@TeleOp`/`@Autonomous` annotation
  or it silently won't appear in the list.
- `StpBot` models **3-pod dead-wheel odometry** (`leftEncoder`,
  `rightEncoder`, `xEncoder`) — but the fork's example Pedro
  `Constants.java` configures a **Pinpoint** localizer instead. These
  don't obviously match; likely just unreconciled example config. Worth
  resolving once the real localizer choice is made (see below).
- Comments in this fork's source aren't always trustworthy — e.g.
  `StpBot.java`'s Javadoc still describes it as an "XDriveBot" with omni
  wheels (stale copy-paste from whatever it was templated from). The
  `@BotConfig` annotation is the reliable source, not the comment above it.
- Validated so far: `Drivetrain` subsystem logic drives correctly in sim
  against `StpBot` (`SubsystemDriveTest.java`), confirming the subsystem
  split (hardware-specific `RobotHardware` vs. portable `Drivetrain` logic)
  works as intended — only `RobotHardware`'s device names needed changing
  between real-robot and sim versions.

## Open questions for the build team

- **Wiring:** the Expansion Hub port map in [port-map.md](port-map.md) is
  a proposal. Wire to it, or change it and `config/HardwareConfig`
  together.
- **Shared Control Hub:** inventory shows only 1 Control Hub across both
  teams in the club. That's a scheduling/ordering conversation, not a
  code problem.
- **Turret + adjustable hood (target Nov 7):** the shot model already
  supports a hood range (`tuning/ShooterTuning` `HOOD_MIN/MAX_DEG`). Turret
  yaw is not modeled yet.
- **Ball sensor:** none planned, so Auto counts balls (see
  [auto.md](auto.md)). A trough sensor would make Auto much more reliable.

## Where things stand (Oct 2026)

All the code below was written without robot access. **Nothing has run on
hardware yet.** Bring-up order and every value still to measure:
[robot-checklist.md](robot-checklist.md).

| Area | Code | Doc |
|---|---|---|
| SDK 12.0, Gradle 9.1 | build files | README |
| Odometry (2 pods + IMU), Pod Placer | `config/`, `tools/pod-placer` | [odometry-setup.md](odometry-setup.md) |
| Field model, red/blue rotation, HIVE state | `field/` | [field-model.md](field-model.md) |
| Shot model, two-shooter auto-aim, pre-align | `shooting/`, `tuning/` | [shot-model.md](shot-model.md) |
| Shot data → tables | Shot Tuner OpMode, `tools/shot-table-editor` | [shot-model.md](shot-model.md) |
| Mechanisms | `subsystems/` | [port-map.md](port-map.md) |
| Limelight: ball pipeline, tag position fixes | `vision/`, `subsystems/Vision`, `assets/limelight/` | [vision.md](vision.md) |
| Red/Blue TeleOp | `opmodes/competition/` | [driver-controls.md](driver-controls.md) |
| Red/Blue Auto, partner plans | `auto/`, `opmodes/competition/`, `tools/partner-planner` | [auto.md](auto.md) |

## Roadmap

1. ✅ Pedro Visualizer basics + `AutoTemplate`
2. ✅ Subsystem structure (`RobotHardware` + `Drivetrain`, validated in sim)
3. ⏸ Record & replay framework (`replay/`): data format, events, path
   reconstruction. Built robot-free; not wired to the robot yet.
4. ✅ SDK 12.0, odometry config, field model, shot model, auto-aim
5. ✅ Mechanisms, Red/Blue TeleOp with pre-align, shot logging + table
   editor
6. ✅ Ball vision, AprilTag position correction, Red/Blue Auto, partner
   planner
7. ⏳ **Robot bring-up and tuning:** work through
   [robot-checklist.md](robot-checklist.md) top to bottom
8. Later: turret + adjustable hood, a ball sensor if one is added
