# Architecture & Progress Notes (Summer 2026)

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

Current files (`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`):
- `subsystems/RobotHardware.java`
- `subsystems/Drivetrain.java` — robot-centric mecanum driving only so far;
  field-centric intentionally deferred (team decision — simpler to start,
  can add as a second method later without touching existing code)
- `AutoTemplate.java` — swap-point-based autonomous skeleton (see file
  comments: swap starting pose + `Paths` class body per autonomous)
- `TeleOpTemplate.java` — minimal TeleOp using the subsystem pattern

## Pedro Pathing status

- Pinned at v2.1.2 in `build.dependencies.gradle`.
- Workflow: draw + export paths at https://visualizer.pedropathing.com/,
  paste the generated `Paths` class body into `AutoTemplate.java`.
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

## Open questions for build team / fall

- ~~Odometry approach not finalized~~ — decided: two goBILDA 4-Bar pods
  + IMU (two-wheel localizer). The sim's StpBot still models 3-pod
  odometry and its example config a Pinpoint; neither matches the robot.
- **Shared Control Hub:** inventory shows only 1 Control Hub across both
  teams in the club — a scheduling/ordering conversation, not a code
  problem, but worth raising early.
- Real drivetrain motor names/directions (currently placeholders in
  `RobotHardware.java`) need to match whatever the Driver Station config
  ends up using once a Control Hub exists.

## Roadmap

1. ✅ Pedro Visualizer basics + `AutoTemplate`
2. ✅ Subsystem structure (`RobotHardware` + `Drivetrain`, validated in sim)
3. ⏳ Record & replay autonomous framework — record robot pose (not raw
   inputs) over time, reconstruct as a Pedro path, follow closed-loop.
   Discrete actions tagged to position/time as events. Building the
   framework (data format, event system, path reconstruction) robot-free
   this summer; wiring to real odometry recording is a fall task.
4. Fall: SDK bump for the real season, real field map, game-specific
   mechanisms, resolve odometry/localizer choice, real hardware config.
