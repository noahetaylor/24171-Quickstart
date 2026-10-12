# Port Map

What plugs in where, and what to name it in the Driver Hub's robot
configuration. **Names are case-sensitive and must match exactly.** The code
side of this table is `config/HardwareConfig.java` — change both together.

## Control Hub

### Motor ports

| Port | Config type | Name | Device | Encoder connector |
|---|---|---|---|---|
| 0 | goBILDA 5202/3/4 | `leftRear` | left rear drive motor | **strafe odometry pod** |
| 1 | goBILDA 5202/3/4 | `leftFront` | left front drive motor | (unused) |
| 2 | goBILDA 5202/3/4 | `rightFront` | right front drive motor | (unused) |
| 3 | goBILDA 5202/3/4 | `rightRear` | right rear drive motor | **forward odometry pod** |

Each pod's cable goes into the **encoder** connector next to that motor
port, *not* the drive motor's own encoder cable. The drive motors' encoder
cables on ports 0 and 3 stay unplugged. Leaving 1 and 2's plugged in is fine
but unused.

Why 0 and 3: REV hubs decode encoder ports 0 and 3 in hardware. Ports 1 and
2 are fine for a motor's own encoder but can drop counts from a 2000-count
odometry pod at full speed.

### I2C

| Bus | Name | Device |
|---|---|---|
| 0 | `imu` | built-in IMU (already in the default config) |

### Physical mounting (fill in)

The IMU setting in `HardwareConfig` must match how the hub is mounted:

- Logo faces: **UP** (placeholder — confirm)
- USB ports face: **FORWARD** (placeholder — confirm)

## Expansion Hub

Not used yet. Mechanisms (intake, transfer, flywheel ×2, servos) will go here.
