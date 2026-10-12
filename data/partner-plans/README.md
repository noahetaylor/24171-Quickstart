# Partner plans

One `.txt` file per plan, made with the Partner Planner
(`tools/partner-planner`). The planner sends these to the robot
(`/sdcard/FIRST/data/partner-plans/`), and Auto's INIT partner page picks
one. Commit them, so the next time you're paired with a team its plan is
already here.

Format (read by `TeamCode/.../auto/PartnerPlan.fromText`):
```
name=14250 left push
zone=x0,y0,x1,y1,fromS,toS     # inches, drawn for RED; seconds into Auto
```
