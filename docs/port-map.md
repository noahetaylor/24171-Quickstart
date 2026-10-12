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

## Expansion Hub (PROPOSED — wire to this, or change both here and `HardwareConfig`)

Every mechanism is optional in the code. Anything missing from the Driver
Hub configuration is skipped and listed in telemetry as "Not in config", so
the robot runs while it's still being built.

### Motor ports

| Port | Config type | Name | Device |
|---|---|---|---|
| 0 | goBILDA 5202/3/4 | `pollenFlywheel` | POLLEN flywheel (5203 1:1). **Its encoder cable must be plugged in.** |
| 1 | goBILDA 5202/3/4 | `intake` | intake rollers (5203 13.7:1) |
| 2 | goBILDA 5202/3/4 | `transfer` | transfer: trough → columns → flywheels (5203 13.7:1) |
| 3 | goBILDA 5202/3/4 | `nectarFlywheel` | NECTAR flywheel (5203 1:1). **Its encoder cable must be plugged in.** |

The flywheels go on ports 0 and 3 for the same reason the pods do: those
are the hardware-decoded encoder ports, and the flywheels need clean
speed readings for velocity control.

### Servo ports

| Port | Config type | Name | Device |
|---|---|---|---|
| 0 | Continuous Rotation Servo | `crossWheels` | NECTAR cross wheels (one servo, chain to both) |
| 1 | Servo | `rampBar` | intake ramp bar |
| 2 | Servo | `limelightPan` | Limelight pan |
| 3 | Servo | `limelightTilt` | Limelight tilt |
| 4, 5 | — | (reserved) | hood servos, adjustable-hood robot |

Control Hub servo ports 0 and 1 are reserved for turret yaw.

### USB

Limelight 3A → Control Hub USB 3.0 port (R708). In the Driver Hub configuration it appears as an Ethernet device; name it `limelight`. Setup: [vision.md](vision.md).

### Directions to check

Each mechanism's direction is in `HardwareConfig` (`*_DIR`). On the robot,
check that positive power pulls balls **in** (intake), moves them **up**
(transfer), carries NECTAR the same way as the transfer (cross wheels), and
launches the ball (flywheels). Flip any that are wrong.
