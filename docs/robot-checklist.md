# Robot Checklist

Everything written without the robot that still needs checking on it.
Work top to bottom: later items trust the ones above. Tick items off in
a PR as they're done.

## 1. SDK 12.0
- [ ] REV Hardware Client: update the Driver Hub app and Control Hub
      firmware to 12.0, then deploy from Android Studio
- [ ] Robot connects; an existing OpMode runs

## 2. Odometry ([odometry-setup.md](odometry-setup.md))
- [ ] Driver Hub robot configuration matches [port-map.md](port-map.md)
- [ ] Hub mounting: set `config/HardwareConfig` `HUB_LOGO` / `HUB_USB`.
      In **Odometry Check**, the heading rises when the robot turns
      counter-clockwise
- [ ] Pod arrows: set in the Pod Placer from Odometry Check's raw counts
- [ ] Offsets Tuner: `forwardY ≈ +4.25`, `strafeX ≈ −7.5`. If `forwardY`'s
      sign is opposite, flip the rule in `config/OdometryPods` (and its test)
- [ ] Forward / Lateral Tuners → `POD_INCHES_PER_TICK`
- [ ] Localization Test: a lap returns within ~1 in; spinning in place
      barely moves x/y
- [ ] Pod Placer: dragging, rotating and saving tried in a browser
- [ ] Rest of Pedro tuning (velocities, zero-power acceleration, PIDF)

## 3. Shooting ([shot-model.md](shot-model.md))
- [ ] `tuning/ShooterTuning` `FIRE_LATENCY_S`: trigger to ball leaving
- [ ] `EXIT_EFFICIENCY`: the measured ball speed fits the physics rpm
- [ ] **Shot Planner Check**: distance readout matches a tape measure
- [ ] Shot Tuner → Shot Table Editor: fill both tables across the distances
      we shoot from (steps in shot-model.md)
- [ ] Shot Table Editor: **Pull from robot** works over the robot's Wi-Fi
- [ ] `tuning/AimTuning`: turns crisply onto target without overshoot

## 4. Mechanisms ([port-map.md](port-map.md))
- [ ] Wire to the port map (or change it and `config/HardwareConfig` together)
- [ ] Directions: intake pulls in, transfer moves up, cross wheels carry
      NECTAR, flywheels launch (`HardwareConfig` `*_DIR`)
- [ ] Ramp bar servo positions in `tuning/MechanismTuning`
      (`RAMP_DOWN` / `BLOCK` / `OPEN` / `STOW`). STOW must keep the robot
      inside the 18 in sizing box
- [ ] Flywheel velocity PIDF (`FLYWHEEL_F` first, then `P`): reaches rpm
      fast without overshoot; telemetry shows actual vs target
- [ ] `FEED_POWER`: balls go up one after another, not jammed

## 5. Vision ([vision.md](vision.md))
- [ ] Limelight web page: slot 0 = Python, slot 1 = AprilTag 36h11;
      run **Limelight Setup**
- [ ] Gimbal servo calibration in `config/LimelightMount` (Limelight Check)
- [ ] Lens position measured into `config/LimelightMount`
- [ ] A ball on a known spot is located within ~2 in
- [ ] HSV ranges tuned under event-like lighting (`ball_pipeline.py`)
- [ ] Tag mode sees the up CELL; `CELL_TAG_HEIGHT_IN` measured

## 6. Competition TeleOp ([driver-controls.md](driver-controls.md))
- [ ] Hold right trigger: turns onto the CELL and fires once both flywheels
      are at speed
- [ ] Hold left trigger (pre-align): drives to a shot spot, holds it, fires.
      Letting go hands control back at once
- [ ] Practice start spot (`MatchState`, no Auto) matches where the robot
      is placed

## Done
- [x] Balls leave out the front (`LAUNCH_YAW_DEG = 0`), team, 2026-10-12
- [x] Field CAD: HIVE ±12 in from center, up-CELL opening ~53.5 in high,
      FLOWER 3 in from the wall, team, 2026-10-12
