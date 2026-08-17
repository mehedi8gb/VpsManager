package com.vpsmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Detects native terminals and starts a VPS command in the chosen terminal. */
public final class TerminalLauncher {
    public static final String AUTO = "auto";
    private static final String WINDOWS_TERMINAL = "windows-terminal";
    private static final String POWERSHELL = "powershell";
    private static final String CMD = "cmd";
    private static final String GNOME_CONSOLE = "gnome-console";
    private static final String GNOME_TERMINAL = "gnome-terminal";
    private static final String KONSOLE = "konsole";
    private static final String XFCE_TERMINAL = "xfce4-terminal";
    private static final String UBUNTU_TERMINAL = "x-terminal-emulator";
    private static final String XTERM = "xterm";
    private static final String MACOS_TERMINAL = "macos-terminal";

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
        } else if (isMac()) {
            options.add(new Option(MACOS_TERMINAL, "Terminal"));
        } else {
            addIfAvailable(options, GNOME_CONSOLE, "GNOME Console", "kgx");
            addIfAvailable(options, GNOME_TERMINAL, "GNOME Terminal", "gnome-terminal");
            addIfAvailable(options, KONSOLE, "KDE Konsole", "konsole");
            addIfAvailable(options, XFCE_TERMINAL, "Xfce Terminal", "xfce4-terminal");
            addIfAvailable(options, UBUNTU_TERMINAL, "Ubuntu default terminal", "x-terminal-emulator");
            addIfAvailable(options, XTERM, "XTerm", "xterm");
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
            List<String> result = new ArrayList<>(List.of("cmd.exe", "/c", "start", ""));
            result.addAll(shellCommand);
            return result;
        }
        if (MACOS_TERMINAL.equals(terminal)) {
            String script = macScript(shell, command);
            return List.of("osascript", "-e", "tell application \"Terminal\" to do script \"" + script + "\"");
        }
        List<String> result = new ArrayList<>();
        switch (terminal) {
            case GNOME_CONSOLE -> result.addAll(List.of("kgx", "--"));
            case GNOME_TERMINAL -> result.addAll(List.of("gnome-terminal", "--"));
            case KONSOLE -> result.addAll(List.of("konsole", "-e"));
            case XFCE_TERMINAL -> result.addAll(List.of("xfce4-terminal", "-x"));
            case UBUNTU_TERMINAL, XTERM -> result.addAll(List.of(terminal, "-e"));
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
