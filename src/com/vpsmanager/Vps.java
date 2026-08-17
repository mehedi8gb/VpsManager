package com.vpsmanager;

/**
 * Simple data holder for one VPS entry.
 */
public class Vps {
    public enum Shell { CMD, POWERSHELL, BASH, ZSH }

    private String name;
    private String host;
    private String username;
    private String password;
    private Shell shell;
    private String command; // supports {host} {username} {password} placeholders

    public Vps(String name, String host, String username, String password, Shell shell, String command) {
        this.name = name;
        this.host = host;
        this.username = username;
        this.password = password;
        this.shell = shell;
        this.command = command;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Shell getShell() { return shell; }
    public void setShell(Shell shell) { this.shell = shell; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    /** Replaces {host} {username} {password} placeholders inside the command string. */
    public String buildFinalCommand() {
        String c = command == null ? "" : command;
        c = c.replace("{host}", host == null ? "" : host);
        c = c.replace("{username}", username == null ? "" : username);
        c = c.replace("{password}", password == null ? "" : password);
        return c;
    }

    @Override
    public String toString() {
        return name + "  (" + host + ")";
    }
}
