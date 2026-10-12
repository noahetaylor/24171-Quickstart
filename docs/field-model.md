# Field Model

Code: `TeamCode/.../field/`. It records where things are on the BIOBUZZ
field, in the same coordinates the robot's pose uses (Pedro, inches).

## Coordinates

Stand where the audience stands:

```
              far wall ("Scoring" side)          y = 144
   +--------------------------------------------+
   |  F (far flower, x=48)        blue garden F6|
   |                                             |
 R | red loading zone A5                         | B
 E |                 RED   BLUE                  | L
 D |     flower      HIVE  HIVE        flower    | U
   |     (y=48)      (C)   (D)         (y=96)    | E
   |                         blue loading zone F2|
   |red garden A1         (audience flower, x=96)|
   +--------------------------------------------+
 (0,0)            audience wall                  x = 144
```

- Origin is the corner where the audience wall meets the red wall.
- +x points toward the blue wall, +y toward the far wall. Heading 0 faces
  +x, and counter-clockwise is positive.
- Field center is (72, 72), with a tile seam every 24 in.

## Red vs blue

The field is **rotated 180°** between alliances, **not mirrored**. Write
every route for RED, then convert it with `Alliance.fromRed(...)` /
`headingFromRed(...)`. For BLUE, `(x, y, h)` becomes `(144−x, 144−y, h+180°)`.
A left turn stays a left turn, so turn-direction logic carries over
unchanged.

## HIVE state

`HiveState` tracks which CELL is up on each HIVE. At match start, RED's
AUDIENCE CELL and BLUE's SCORING CELL are up, each holding 3 NECTAR.

- **Tipping:** each CELL is calibrated to tip at 3 NECTAR + 3 POLLEN, or at
  8 POLLEN. So a starting CELL tips on its 3rd POLLEN.
- **Other robots tip HIVEs too.** Update the state from the camera with
  `observeTag(id)`; don't count our own shots.

| CELL | AprilTag IDs |
|---|---|
| RED SCORING | 30–33 |
| RED AUDIENCE | 34–37 |
| BLUE AUDIENCE | 38–41 |
| BLUE SCORING | 42–45 |

SDK 12.0's tag library treats each CELL's tags as one group, with the
group's origin at the center of the CELL opening.

## Still estimated (check against the Onshape field CAD)

Everything else comes straight from the manual or the Event Field Setup
Guide. These values were read off figures:

| Constant in `Field.java` | Value now | What to measure |
|---|---|---|
| `HIVE_X_OFFSET_IN` | 12.0 | sideways distance from field center to each HIVE's middle |
| `UP_CELL_OPENING_HEIGHT_IN` | 43.95 (pivot height, placeholder) | height of an up CELL's opening center |
| `FLOWER_WALL_INSET_IN` | 3.0 | wall to FLOWER center |

Two more things to confirm against the CAD:
- **CELL position when up.** `Cell.openingWhenUp()` places each CELL 9.4 in
  (half the 18.8 in spacing) from center, straight out along y. A tipped-up
  CELL probably sits somewhat differently.
- **FLOWER positions.** They were read off a figure, to about ±1 in.
