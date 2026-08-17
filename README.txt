VPS MANAGER — Java Swing Desktop App (v2 — Phase 1 Redesign)
=============================================================

WHAT IT DOES
- Shows your VPS entries as modern rounded cards.
- Single click on a card → immediately opens a new terminal window and
  runs that VPS's command (CMD or PowerShell, per the entry's setting).
- Hover a card → small ✎ (edit) and 🗑 (delete) icons appear on the right.
- Click ✎ → opens the Edit dialog pre-filled with that VPS's details.
- Click 🗑 → an inline "Delete? ✓ ✗" confirm appears (no popup dialog).
- "+ Add" button (top-right) → opens the Add dialog.
- Everything is saved to data\vps.txt automatically.

REQUIREMENTS
- OpenJDK 17  (java --version to confirm)
- lib\flatlaf-3.5.4.jar  (already placed in lib\ — do not move)

HOW TO BUILD (Windows)
1. Open a Command Prompt or PowerShell in this folder.
2. Run:
       build.bat
   This compiles everything and packages VpsManager.jar (fat jar —
   FlatLaf is embedded, no extra -cp needed at runtime).

HOW TO RUN
   run.bat                          ← always use this (no console window)
   javaw -jar VpsManager.jar        ← equivalent, console-free
   java  -jar VpsManager.jar        ← works but may flash a console briefly

   DO NOT double-click the .jar directly — Windows may use the console
   java.exe launcher instead of javaw.exe. Use run.bat instead.

TERMINAL LAUNCH (BUG FIX NOTE)
The connect action uses:
   cmd /c start cmd.exe /k <your-command>          (for CMD shell)
   cmd /c start powershell.exe -NoExit -Command … (for PowerShell)

This "cmd /c start" wrapper guarantees a new visible window always
appears, even when the Java GUI process has no console of its own.
The old approach (ProcessBuilder directly to powershell.exe / cmd.exe)
would silently inherit the hidden JVM console and never show a window.

USING THE APP
- Click "+ Add" to create a new VPS entry:
    Name       - label shown on the card
    Host / IP  - the server address
    Username   - optional; used if your command has {username}
    Password   - optional; used if your command has {password}
                 (stored in plain text in data\vps.txt — Phase 3 adds encryption)
    Shell      - CMD or PowerShell: which one opens on connect
    Command    - the command to run, e.g.:
                   ssh {username}@{host}
                   ssh root@203.0.113.20
                   plink -pw {password} {username}@{host}
                   mstsc /v:{host}

  Placeholders {host}, {username}, {password} are substituted automatically.

EDITING THE DATA FILE DIRECTLY
data\vps.txt is plain text, one block per VPS, separated by "---".
Restart the app after hand-editing to pick up changes.

PROJECT FILES
src\com\vpsmanager\App.java         - entry point; sets up FlatLaf theme
src\com\vpsmanager\MainFrame.java   - main window, top bar, connect logic
src\com\vpsmanager\VpsCardList.java - custom card-list component (NEW)
src\com\vpsmanager\VpsDialog.java   - Add/Edit popup form
src\com\vpsmanager\Vps.java         - data model
src\com\vpsmanager\VpsStore.java    - loads/saves data\vps.txt
lib\flatlaf-3.5.4.jar               - FlatLaf Light theme (bundled at build)
data\vps.txt                        - your saved VPS list
build.bat                           - compiles + builds VpsManager.jar
run.bat                             - runs the app (javaw, no console)
