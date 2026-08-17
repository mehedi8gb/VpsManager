# VPS Manager

VPS Manager is a Java 17 Swing desktop application for keeping a list of VPS connections and launching their configured shell commands. It uses the FlatLaf look and feel.

## Cross-platform terminal support

Use the gear icon in the main window to choose the default terminal for new VPS entries. When adding or editing a VPS, choose its terminal and shell independently. The default **Automatic** setting detects available options for the current operating system:

- Windows: Windows Terminal, PowerShell, or Command Prompt.
- Linux/Ubuntu: GNOME Console, GNOME Terminal, Konsole, Xfce Terminal, the system `x-terminal-emulator`, or XTerm.
- macOS: Terminal.

Additional terminal-looking executables found on a Linux/Unix `PATH` are listed as `Detected: <name>` and launched with the standard `-e` convention.

Each VPS can use CMD, PowerShell, Bash, or Zsh. The self-contained JAR runs on Windows, Linux, and macOS with Java 17. Native installers/app images must be built with `jpackage` on their target operating system.

> Screenshot placeholder: add a screenshot at `docs/screenshot.png`, then replace this note with `![VPS Manager screenshot](docs/screenshot.png)`.

## Just want the app?

Download `VpsManager-1.0.0-Setup.exe` from this repository's [GitHub Releases](../../releases) page and run it. Java is bundled with the application; no JDK or separate Java installation is required.

The release also includes a portable app image (`VpsManager-1.0.0/`) and the self-contained `VpsManager-1.0.0.jar`.

## Run from source

1. Install JDK 17.
2. Clone the repository.
3. Run `build.bat` from the repository root.

The script cleans previous build output, compiles the sources, creates the fat JAR, creates the portable app image with its own minimal Java runtime, and creates the installer. If WiX is not already on `PATH`, the script downloads portable WiX 3.11 tooling into its temporary build directory automatically. Release files are written to `dist/`.

## Storage

Installed Windows builds read and write `%LOCALAPPDATA%\VpsManager\data\vps.txt`, which is writable without administrator permission. On first launch, the packaged `data/vps.txt` file is copied there as starter data. `data/vps.bin` is ignored by Git for a future encrypted-storage implementation.

## Development

`lib/flatlaf-3.5.4.jar` is the sole third-party build dependency. Build output and local VPS data are excluded from version control.

## License

This project is licensed under the [MIT License](LICENSE).
