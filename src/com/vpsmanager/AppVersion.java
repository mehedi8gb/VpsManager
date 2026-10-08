package com.vpsmanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

final class AppVersion {
    private static final String DEFAULT_VERSION = "1.0.2";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private AppVersion() {
    }

    static String current() {
        Properties properties = new Properties();
        try (InputStream input = AppVersion.class.getResourceAsStream("/version.properties")) {
            if (input != null) {
                properties.load(input);
                String version = properties.getProperty("version", "").trim();
                if (VERSION_PATTERN.matcher(version).matches()) {
                    return version;
                }
            }
        } catch (IOException ignored) {
        }
        return DEFAULT_VERSION;
    }
}