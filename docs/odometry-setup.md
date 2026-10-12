# Odometry Setup & Test

Hardware: two goBILDA 4-Bar odometry pods + the Control Hub IMU (Pedro's
**two-wheel** localizer). Wiring: [port-map.md](port-map.md).

Where the values live:
- **Pod position, rolling direction, count direction** → the **Pod Placer**
  (visual editor, below). It writes `config/OdometryGeometry.java`.
- Everything else (names, motor directions, IMU mounting, ticks per inch)
  → `config/HardwareConfig.java`.

You never type Pedro's signed offsets by hand: `config/OdometryPods.java`
converts the physical layout into them (unit-tested in `OdometryPodsTest`).

Do the steps in order — each one assumes the previous passed.

## The Pod Placer (laptop only)

Double-click `tools/pod-placer/Pod Placer.bat` (or run
`python tools/pod-placer/server.py`). It opens a top-down view of the robot
on a 0.1 in grid in your browser.

- Drag each pod's dot to where its **wheel touches the floor**, or type the
  numbers. Drags snap to 0.1 in (hold Shift for 0.01).
- Set each pod's arrow to the way the **robot moves to make that pod's raw
  count go up** (from Odometry Check, step 2). `R` also rotates.
- **Save** (or Ctrl+S) writes the file. Nothing is written until you click
  Save. If the file was changed elsewhere (Android Studio, `git pull`) after
  the tool loaded it, Save refuses and asks what to do.
- It changes source code only. Rebuild and deploy from Android Studio to get
  it onto the robot.

## 1. Configure the Driver Hub

Create a robot configuration matching [port-map.md](port-map.md) and
activate it. Fill in the hub's real mounting (logo / USB direction) in
`HardwareConfig.HUB_LOGO` / `HUB_USB`.

## 2. Odometry Check (our OpMode, TeleOp list → "Odometry Check")

Wheels off the ground for the motor part.

| Do this | Expect | If wrong |
|---|---|---|
| Hold X / Y / A / B | that one wheel spins, rolling the robot forward | flip its `*_DIR` in `HardwareConfig` |
| Push the robot by hand in each direction | each RAW pod count rises for one direction | set that pod's arrow in the Pod Placer, Save, redeploy |
| Push forward / left by hand | "as Pedro sees it" forward / strafe go **up** | arrows are wrong in the Pod Placer |
| Turn robot counter-clockwise | heading goes **up** | fix `HUB_LOGO` / `HUB_USB` |
| Push forward exactly 48 in (tape measure) | forward pod reads ~48 | fine-tuned in step 4 |

A wheel on the wrong button means the motor is in the wrong port or named
wrong in the config. A pod that never changes means it's in the wrong
encoder connector.

## 3. Pod offsets (Tuning → Localization → Offsets Tuner)

Confirms the pod offsets, including their sign.

1. Set `HardwareConfig.ZERO_OFFSETS_FOR_OFFSETS_TUNER = true`, deploy.
2. Run the tuner, turn the robot 180° in place, slowly.
3. It prints `forwardY` and `strafeX`. Compare with the "Check on the robot"
   box in the Pod Placer (currently about **+4.25** and **−7.5**).
4. Set the flag back to `false`.

Small differences (a few tenths of an inch) mean the measurements were a
little off. Nudge the pods in the Pod Placer until its numbers match the
tuner's. If `forwardY` has the **opposite sign**, the sign rule in
`OdometryPods.java` is wrong for our Pedro version. Flip that one line
(and its test) rather than moving pods to the wrong side.

## 4. Ticks per inch (Tuning → Localization → Forward Tuner, Lateral Tuner)

Push the robot the distance the tuner asks (48 in by default) and copy the
multiplier into `POD_INCHES_PER_TICK`. The forward and lateral values
should be nearly identical (same pod model); if they differ by more than
~1%, give the strafe pod its own constant.

## 5. Localization Test (Tuning → Localization → Localization Test)

Drive around with the gamepad and watch the pose.

- Drive a lap around the field and back to the start mark: x/y should
  return within ~1 in.
- Spin in place 5–10 times: x/y should barely move. Drift here means a
  wrong offset (redo step 3).

After this passes, the robot knows where it is and the rest of Pedro's
tuning (velocity, zero-power acceleration, PIDF) can begin.
