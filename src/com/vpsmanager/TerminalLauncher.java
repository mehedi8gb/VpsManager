package com.vpsmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Detects native terminals and starts a VPS command in the chosen terminal. */
public final class TerminalLauncher {
    public static final String AUTO = "auto";
    private static final String WINDOWS_TERMINAL = "windows-terminal";
    private static final String POWERSHELL = "powershell";
    private static final String CMD = "cmd";
    private static final String GIT_BASH = "git-bash";
    private static final String WSL = "wsl";
    private static final String GNOME_CONSOLE = "gnome-console";
    private static final String GNOME_TERMINAL = "gnome-terminal";
    private static final String KONSOLE = "konsole";
    private static final String XFCE_TERMINAL = "xfce4-terminal";
    private static final String UBUNTU_TERMINAL = "x-terminal-emulator";
    private static final String TILIX = "tilix";
    private static final String TERMINATOR = "terminator";
    private static final String MATE_TERMINAL = "mate-terminal";
    private static final String LXTERMINAL = "lxterminal";
    private static final String QTERMINAL = "qterminal";
    private static final String ALACRITTY = "alacritty";
    private static final String KITTY = "kitty";
    private static final String WEZTERM = "wezterm";
    private static final String FOOT = "foot";
    private static final String URXVT = "urxvt";
    private static final String XTERM = "xterm";
    private static final String MACOS_TERMINAL = "macos-terminal";
    private static final String ITERM = "iterm";
    private static final Set<String> KNOWN_TERMINAL_EXECUTABLES = Set.of(
            "kgx", "gnome-terminal", "konsole", "xfce4-terminal", "x-terminal-emulator",
            "tilix", "terminator", "mate-terminal", "lxterminal", "qterminal", "alacritty",
            "kitty", "wezterm", "foot", "urxvt", "xterm");

    public record Option(String id, String label) {
        @Override public String toString() { return label; }
    }

    private TerminalLauncher() { }

    public static List<Option> availableOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(AUTO, "Automatic (recommended)"));
        if (isWindows()) {
            addIfAvailable(options, WINDOWS_TERMINAL, "Windows Terminal", "wt.exe");
            addIfAvailable(options, POWERSHELL, "PowerShell", "powershell.exe");
            options.add(new Option(CMD, "Command Prompt"));
            addIfAvailable(options, GIT_BASH, "Git Bash", "bash.exe");
            addIfAvailable(options, WSL, "WSL / Ubuntu terminal", "wsl.exe");
        } else if (isMac()) {
            options.add(new Option(MACOS_TERMINAL, "Terminal"));
            if (Files.exists(Paths.get("/Applications/iTerm.app"))) {
                options.add(new Option(ITERM, "iTerm"));
            }
        } else {
            addIfAvailable(options, GNOME_CONSOLE, "GNOME Console", "kgx");
            addIfAvailable(options, GNOME_TERMINAL, "GNOME Terminal", "gnome-terminal");
            addIfAvailable(options, KONSOLE, "KDE Konsole", "konsole");
            addIfAvailable(options, XFCE_TERMINAL, "Xfce Terminal", "xfce4-terminal");
            addIfAvailable(options, UBUNTU_TERMINAL, "Ubuntu default terminal", "x-terminal-emulator");
            addIfAvailable(options, TILIX, "Tilix", "tilix");
            addIfAvailable(options, TERMINATOR, "Terminator", "terminator");
            addIfAvailable(options, MATE_TERMINAL, "MATE Terminal", "mate-terminal");
            addIfAvailable(options, LXTERMINAL, "LXTerminal", "lxterminal");
            addIfAvailable(options, QTERMINAL, "QTerminal", "qterminal");
            addIfAvailable(options, ALACRITTY, "Alacritty", "alacritty");
            addIfAvailable(options, KITTY, "Kitty", "kitty");
            addIfAvailable(options, WEZTERM, "WezTerm", "wezterm");
            addIfAvailable(options, FOOT, "Foot", "foot");
            addIfAvailable(options, URXVT, "URxvt", "urxvt");
            addIfAvailable(options, XTERM, "XTerm", "xterm");
            discoverAdditionalTerminals(options);
        }
        return options;
    }

    public static void launch(Vps.Shell shell, String command, String preference) throws IOException {
        String terminal = resolveTerminal(shell, preference);
        if (terminal == null) {
            throw new IOException("No supported terminal was found. Choose one in Settings or install a terminal emulator.");
        }
        new ProcessBuilder(buildLaunchCommand(terminal, shell, command)).start();
    }

    private static String resolveTerminal(Vps.Shell shell, String preference) {
        if (!AUTO.equals(preference)) return preference;
        if (isWindows()) {
            if (isOnPath("wt.exe")) return WINDOWS_TERMINAL;
            return shell == Vps.Shell.POWERSHELL && isOnPath("powershell.exe") ? POWERSHELL : CMD;
        }
        if (isMac()) return MACOS_TERMINAL;
        for (Option option : availableOptions()) {
            if (!AUTO.equals(option.id())) return option.id();
        }
        return null;
    }

    private static List<String> buildLaunchCommand(String terminal, Vps.Shell shell, String command) throws IOException {
        List<String> shellCommand = shellCommand(shell, command);
        if (isWindows()) {
            if (WINDOWS_TERMINAL.equals(terminal)) {
                List<String> result = new ArrayList<>(List.of("wt.exe", "-w", "new"));
                result.addAll(shellCommand);
                return result;
            }
            if (POWERSHELL.equals(terminal)) return shell == Vps.Shell.POWERSHELL
                    ? shellCommand : List.of("powershell.exe", "-NoExit", "-Command", joinForPowerShell(shellCommand));
            if (GIT_BASH.equals(terminal)) return List.of("bash.exe", "-lc", command + "; exec bash");
            if (WSL.equals(terminal)) return List.of("wsl.exe", "bash", "-lc", command + "; exec bash");
            List<String> result = new ArrayList<>(List.of("cmd.exe", "/c", "start", ""));
            result.addAll(shellCommand);
            return result;
        }
        if (MACOS_TERMINAL.equals(terminal)) {
            String script = macScript(shell, command);
            return List.of("osascript", "-e", "tell application \"Terminal\" to do script \"" + script + "\"");
        }
        if (ITERM.equals(terminal)) {
            String script = macScript(shell, command);
            return List.of("osascript", "-e", "tell application \"iTerm\" to create window with default profile command \"" + script + "\"");
        }
        List<String> result = new ArrayList<>();
        if (terminal.startsWith("generic:")) {
            result.addAll(List.of(terminal.substring("generic:".length()), "-e"));
            result.addAll(shellCommand);
            return result;
        }
        switch (terminal) {
            case GNOME_CONSOLE -> result.addAll(List.of("kgx", "--"));
            case GNOME_TERMINAL -> result.addAll(List.of("gnome-terminal", "--"));
            case KONSOLE -> result.addAll(List.of("konsole", "-e"));
            case XFCE_TERMINAL -> result.addAll(List.of("xfce4-terminal", "-x"));
            case TERMINATOR -> result.addAll(List.of("terminator", "-x"));
            case UBUNTU_TERMINAL, TILIX, MATE_TERMINAL, LXTERMINAL, QTERMINAL,
                    ALACRITTY, KITTY, WEZTERM, FOOT, URXVT, XTERM -> result.addAll(List.of(terminal, "-e"));
            default -> throw new IOException("The configured terminal is not supported on this operating system.");
        }
        result.addAll(shellCommand);
        return result;
    }

    private static List<String> shellCommand(Vps.Shell shell, String command) {
        return switch (shell) {
            case CMD -> isWindows()
                    ? List.of("cmd.exe", "/k", command)
                    : List.of("sh", "-lc", command + "; exec sh");
            case POWERSHELL -> isWindows()
                    ? List.of("powershell.exe", "-NoExit", "-Command", command)
                    : List.of("pwsh", "-NoExit", "-Command", command);
            case BASH -> List.of("bash", "-lc", command + "; exec bash");
            case ZSH -> List.of("zsh", "-lc", command + "; exec zsh");
        };
    }

    private static String macScript(Vps.Shell shell, String command) {
        String executable = switch (shell) {
            case CMD -> "sh";
            case POWERSHELL -> "pwsh";
            case BASH -> "bash";
            case ZSH -> "zsh";
        };
        String escapedCommand = (command + "; exec " + executable)
                .replace("\\", "\\\\").replace("\"", "\\\"");
        return executable + " -lc \\\"" + escapedCommand + "\\\"";
    }

    private static String joinForPowerShell(List<String> command) {
        return String.join(" ", command.stream().map(TerminalLauncher::quoteForPowerShell).toList());
    }

    private static String quoteForPowerShell(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static void addIfAvailable(List<Option> options, String id, String label, String executable) {
        if (isOnPath(executable)) options.add(new Option(id, label));
    }

    /** Finds extra terminal emulators placed on PATH by a distribution or user. */
    private static void discoverAdditionalTerminals(List<Option> options) {
        Set<String> known = new HashSet<>();
        for (Option option : options) known.add(option.id());

        String path = System.getenv("PATH");
        if (path == null) return;
        for (String directory : path.split(java.io.File.pathSeparator)) {
            try (Stream<Path> entries = Files.list(Paths.get(directory))) {
                entries.filter(Files::isExecutable).forEach(executable -> {
                    String name = executable.getFileName().toString();
                    String lowerName = name.toLowerCase();
                    boolean looksLikeTerminal = lowerName.contains("terminal")
                            || lowerName.endsWith("term")
                            || lowerName.contains("console")
                            || lowerName.contains("kitty")
                            || lowerName.contains("alacritty")
                            || lowerName.contains("wezterm")
                            || lowerName.contains("xterm")
                            || lowerName.contains("rxvt");
                    String id = "generic:" + name;
                    if (looksLikeTerminal && !KNOWN_TERMINAL_EXECUTABLES.contains(lowerName)
                            && known.add(id)) {
                        options.add(new Option(id, "Detected: " + name));
                    }
                });
            } catch (Exception ignored) {
                // A PATH entry can be unreadable or malformed; keep detecting others.
            }
        }
    }

    public static String labelFor(String terminalId) {
        for (Option option : availableOptions()) {
            if (option.id().equals(terminalId)) return option.label();
        }
        return AUTO.equals(terminalId) ? "Automatic" : terminalId;
    }

    private static boolean isOnPath(String executable) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String directory : path.split(java.io.File.pathSeparator)) {
            try {
                if (Files.isExecutable(Paths.get(directory, executable))) return true;
            } catch (Exception ignored) {
                // Ignore malformed PATH entries and continue detection.
            }
        }
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }
}
