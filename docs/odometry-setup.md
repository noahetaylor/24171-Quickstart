# Odometry Setup & Test

Hardware: two goBILDA 4-Bar odometry pods + the Control Hub IMU (Pedro's
**two-wheel** localizer). Wiring: [port-map.md](port-map.md). All values:
`config/HardwareConfig.java`.

Do the steps in order — each one assumes the previous passed.

## 1. Configure the Driver Hub

Create a robot configuration matching [port-map.md](port-map.md) and
activate it. Fill in the hub's real mounting (logo / USB direction) in
`HardwareConfig.HUB_LOGO` / `HUB_USB`.

## 2. Odometry Check (our OpMode, TeleOp list → "Odometry Check")

Wheels off the ground for the motor part.

| Do this | Expect | If wrong, flip |
|---|---|---|
| Hold X / Y / A / B | that one wheel spins, rolling the robot forward | its `*_DIR` |
| Push robot forward by hand | forward pod inches go **up** | `FORWARD_POD_DIR` |
| Push robot left by hand | strafe pod inches go **up** | `STRAFE_POD_DIR` |
| Turn robot counter-clockwise | heading goes **up** | `HUB_LOGO` / `HUB_USB` |
| Push forward exactly 48 in (tape measure) | forward pod reads ~48 | fine-tuned in step 4 |

A wheel on the wrong button means the motor is in the wrong port or named
wrong in the config. A pod that never changes means it's in the wrong
encoder connector.

## 3. Pod offsets (Tuning → Localization → Offsets Tuner)

Confirms the pod offsets, including their sign (see the SIGN NOTE in
`HardwareConfig`).

1. Temporarily set `FORWARD_POD_Y` and `STRAFE_POD_X` to `0`.
2. Run the tuner, turn the robot 180° in place, slowly.
3. It prints `forwardY` and `strafeX`. Expected: about **+4.25** and
   **−7.5**. Put the printed values back in `HardwareConfig`.

If `forwardY` comes out near −4.25 instead, trust the tuner.

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
