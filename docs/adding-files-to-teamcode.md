# Adding a new Java file to TeamCode

Quick reference for getting a new OpMode/class into the robot code so Android
Studio and Gradle pick it up correctly.

## Which project?

- **Android Studio** → `~/24171-Quickstart` (real robot code, Pedro-based). New
  OpModes/classes go here.
- **IntelliJ** → `virtual_robot` fork (2D simulator). Separate repo, separate
  purpose. Don't mix these up.

## Steps

1. **Target folder:**
   ```
   ~/24171-Quickstart/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
   ```

2. **Copy the file in:**
   ```
   cp ~/Downloads/<YourFile>.java ~/24171-Quickstart/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
   ```

3. **Verify:**
   ```
   ls ~/24171-Quickstart/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
   ```

4. **Android Studio:** new files in an existing package are usually picked up
   automatically. If not, right-click project root → **Sync Project with
   Gradle Files**.

5. **Build check:**
   ```
   cd ~/24171-Quickstart
   ./gradlew assembleDebug
   ```

6. **Commit** (standard branch-and-PR habit — see repo README for the
   local-merge workaround for the GitHub ruleset issue):
   ```
   git checkout -b <branch-name>
   git add .
   git commit -m "<message>"
   git push -u origin <branch-name>
   git checkout master && git merge <branch-name> && git push origin master
   ```

## Note on red import errors

Pedro Pathing's package names (`localization`/`pathgen` vs `geometry`/`paths`,
etc.) have shifted across versions. If a Pedro-related import shows red in
Android Studio, retype the class name and use autocomplete (Alt+Enter) to let
it resolve against the version actually installed (currently pinned 2.1.2) —
don't assume the import line as originally written is correct.
