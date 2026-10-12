# Team 24171 — Pedro Pathing Quickstart

FTC robot code for the **BIOBUZZ (2026–2027)** season, built on a fork of the
[Pedro Pathing Quickstart](https://github.com/Pedro-Pathing/Quickstart).
This is the real FTC SDK + Pedro Pathing project that gets deployed to the Control Hub.

> Not to be confused with our `virtual_robot` simulator repo — that's a separate
> desktop app for testing logic without the physical robot. This repo is the one
> that runs on the actual bot.

---

## Where things are

All code is under `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.

| I want to… | Go to |
|---|---|
| Run a match | Driver Hub group **Competition**: Red/Blue Auto, Red/Blue TeleOp (`opmodes/competition/`). Controls: [docs/driver-controls.md](docs/driver-controls.md) |
| Test something or gather data | Driver Hub group **Testing** (`opmodes/testing/`) |
| Change a number we tuned from testing (shot table, rpm, aim) | `tuning/` (also editable live from Panels in the pit) |
| Change a hardware name or the odometry pod layout | `config/` (pods: the Pod Placer tool, `tools/pod-placer/`) |
| See what still needs checking on the robot | [docs/robot-checklist.md](docs/robot-checklist.md) |
| Understand the field coordinates / red vs blue | [docs/field-model.md](docs/field-model.md) |
| Understand shooting + auto-aim | [docs/shot-model.md](docs/shot-model.md) |
| Wire the robot | [docs/port-map.md](docs/port-map.md) |

Logic lives in `field/`, `shooting/`, `subsystems/`, `replay/` and
`pedroPathing/`. You shouldn't need to touch these at practice.

---

## What you need (one-time, per machine)

Any laptop that will build or deploy code needs this set up **once**:

- **Git** — https://git-scm.com/downloads
- **Android Studio** — Narwhal 3 Feature Drop or later (needed by FTC SDK 12.0's
  Gradle 9.1 / Android Gradle Plugin 8.13.2), https://developer.android.com/studio
  (bundles its own JDK, so no separate Java install needed)
- **Android SDK Platform API 34** — install via Android Studio's SDK Manager
  (Settings ▸ Languages & Frameworks ▸ Android SDK ▸ SDK Platforms tab ▸
  check "Android 14.0 (UpsideDownCake)" / API 34)
- **SDK Tools** (SDK Manager ▸ SDK Tools tab): Android SDK Build-Tools,
  Platform-Tools, and Command-line Tools (latest)

> **Note on compileSdk vs. your machine's SDKs:** the project compiles against
> **API 34**. It's fine to have other API levels (16.0, etc.) installed alongside
> it — the build uses whichever `compileSdk` is set, not whatever's newest.

> **FTC SDK version:** the code is on **SDK 12.0** (the BIOBUZZ release). The
> Driver Hub app and the Control Hub firmware should match it. Update them with
> the REV Hardware Client before the first deploy of a new SDK version.

---

## Getting the code onto a new machine

We clone over SSH (make sure your GitHub SSH key is set up on the machine first).

```bash
git clone git@github.com:noahetaylor/24171-Quickstart.git
cd 24171-Quickstart
git remote add upstream https://github.com/Pedro-Pathing/Quickstart.git   # optional: lets you pull Pedro's updates later
```

Then in Android Studio: **File ▸ Open** ▸ select the `24171-Quickstart` folder ▸
**Trust Project**. Let the first Gradle sync finish (5–15 min — it downloads all
dependencies).

---

## Building

Confirm everything compiles (no robot needed):

```bash
./gradlew assembleDebug
```

or in Android Studio: **Build ▸ Make Project** (Ctrl+F9). Success = `BUILD SUCCESSFUL`.
APKs land in `TeamCode/build/outputs/apk/`.

---

## Deploying to the Control Hub (in-season, needs the robot)

1. Connect the laptop to the Control Hub (USB, or the hub's WiFi Direct network).
2. Select the run configuration and hit Run/Deploy to install the app on the hub.
3. Watch live telemetry over the robot's WiFi.

*(This is the only step that requires the physical laptop-plus-robot combo.)*

---

## Pedro Pathing notes

- **Pinned Pedro version:** `com.pedropathing:ftc:2.1.2`
  (defined in `build.dependencies.gradle` — bump deliberately, then re-sync)
- **Pedro constants / tuning files** live in the `pedroPathing` package under
  `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedroPathing/`
- Docs: https://pedropathing.com/docs/pathing

---

## Working conventions

- **Don't commit straight to `master`.** Use a feature branch, then merge via PR.
- Keep OpModes and subsystems organized under `TeamCode/`.
- After pulling upstream Pedro updates, re-sync Gradle and re-run a build before trusting anything.
