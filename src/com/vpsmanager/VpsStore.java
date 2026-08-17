package com.vpsmanager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Reads/writes the VPS list to data/vps.txt using a simple block format:
 *
 * name=MyServer1
 * host=1.2.3.4
 * username=root
 * password=secret
 * shell=POWERSHELL
 * command=ssh {username}@{host}
 * ---
 * name=MyServer2
 * ...
 *
 * Blocks are separated by a line containing exactly "---".
 * Kept as plain text (not JSON) so you can also hand-edit it in Notepad.
 */
public class VpsStore {

    private final Path file;

    public VpsStore(Path file) {
        this.file = file;
    }

    public List<Vps> load() throws IOException {
        List<Vps> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        Map<String, String> current = new HashMap<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.equals("---")) {
                if (!current.isEmpty()) {
                    result.add(toVps(current));
                    current = new HashMap<>();
                }
                continue;
            }
            int idx = line.indexOf('=');
            if (idx > 0) {
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1);
                current.put(key, value);
            }
        }
        if (!current.isEmpty()) {
            result.add(toVps(current));
        }
        return result;
    }

    public void save(List<Vps> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Vps v : list) {
            sb.append("name=").append(nullToEmpty(v.getName())).append("\n");
            sb.append("host=").append(nullToEmpty(v.getHost())).append("\n");
            sb.append("username=").append(nullToEmpty(v.getUsername())).append("\n");
            sb.append("password=").append(nullToEmpty(v.getPassword())).append("\n");
            sb.append("shell=").append(v.getShell().name()).append("\n");
            sb.append("terminal=").append(nullToEmpty(v.getTerminal())).append("\n");
            sb.append("command=").append(nullToEmpty(v.getCommand())).append("\n");
            sb.append("---\n");
        }
        Files.createDirectories(file.getParent());
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private Vps toVps(Map<String, String> m) {
        Vps.Shell shell;
        try {
            shell = Vps.Shell.valueOf(m.getOrDefault("shell", "POWERSHELL"));
        } catch (IllegalArgumentException e) {
            shell = Vps.Shell.POWERSHELL;
        }
        return new Vps(
                m.getOrDefault("name", ""),
                m.getOrDefault("host", ""),
                m.getOrDefault("username", ""),
                m.getOrDefault("password", ""),
                shell,
                m.getOrDefault("terminal", TerminalLauncher.AUTO),
                m.getOrDefault("command", "")
        );
    }
}
