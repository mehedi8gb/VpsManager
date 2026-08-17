package com.vpsmanager;

import java.util.prefs.Preferences;

/** Per-user application preferences, kept outside the installed application folder. */
public final class AppSettings {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(AppSettings.class);
    private static final String TERMINAL_KEY = "preferredTerminal";

    private AppSettings() { }

    public static String getPreferredTerminal() {
        return PREFERENCES.get(TERMINAL_KEY, TerminalLauncher.AUTO);
    }

    public static void setPreferredTerminal(String terminalId) {
        PREFERENCES.put(TERMINAL_KEY, terminalId);
    }
}
