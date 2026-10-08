package com.vpsmanager;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UpdateService {
    private static final String RELEASE_API =
            "https://api.github.com/repos/mehedi8gb/VpsManager/releases/latest";
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "\\\"tag_name\\\"\\s*:\\s*\\\"v(\\d+\\.\\d+\\.\\d+)\\\"");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(UpdateService.class);
    private static final String DEFERRED_VERSION_KEY = "deferredUpdateVersion";

    private UpdateService() {
    }

    static void checkForUpdates(JFrame parent, boolean reportWhenCurrent) {
        if (!isWindows()) {
            if (reportWhenCurrent) {
                showMessage(parent,
                        "Automatic installer updates are currently available on Windows only.",
                        "Updates");
            }
            return;
        }

        Thread updateThread = new Thread(() -> checkInBackground(parent, reportWhenCurrent),
                "vpsmanager-update-check");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private static void checkInBackground(JFrame parent, boolean reportWhenCurrent) {
        try {
            String latestVersion = fetchLatestStableVersion();
            if (compareVersions(latestVersion, AppVersion.current()) <= 0) {
                if (reportWhenCurrent) {
                    showMessage(parent, "You are using the latest stable version.", "Updates");
                }
                return;
            }

            Path installer = downloadInstaller(latestVersion);
            SwingUtilities.invokeLater(() -> {
                if (!parent.isDisplayable()) {
                    return;
                }
                MainFrame mainFrame = (MainFrame) parent;
                mainFrame.setUpdateAvailable(latestVersion, installer);
                if (reportWhenCurrent || !isDeferred(latestVersion)) {
                    offerInstall(parent, latestVersion, installer);
                }
            });
        } catch (Exception exception) {
            if (reportWhenCurrent) {
                String detail = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                showMessage(parent, "Could not check for updates:\n" + detail, "Update check failed");
            }
        }
    }

    private static String fetchLatestStableVersion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(RELEASE_API))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "VpsManager-Updater")
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("GitHub returned HTTP " + response.statusCode() + ".");
        }

        Matcher matcher = TAG_PATTERN.matcher(response.body());
        if (!matcher.find()) {
            throw new IOException("The latest stable release does not have a vMAJOR.MINOR.PATCH tag.");
        }
        return matcher.group(1);
    }

    private static Path downloadInstaller(String version) throws IOException, InterruptedException {
        String fileName = "VpsManager-" + version + "-Setup.exe";
        Path updateDirectory = getUpdateDirectory();
        Files.createDirectories(updateDirectory);
        Path installer = updateDirectory.resolve(fileName);
        if (Files.isRegularFile(installer) && Files.size(installer) > 0) {
            return installer;
        }

        URI downloadUri = URI.create("https://github.com/mehedi8gb/VpsManager/releases/download/v"
                + version + "/" + fileName);
        Path temporaryFile = Files.createTempFile(updateDirectory, "vpsmanager-update-", ".download");
        try {
            HttpRequest request = HttpRequest.newBuilder(downloadUri)
                    .timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "VpsManager-Updater")
                    .GET()
                    .build();
            HttpResponse<Path> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofFile(temporaryFile));
            if (response.statusCode() != 200 || Files.size(temporaryFile) == 0) {
                throw new IOException("GitHub could not provide the Windows installer (HTTP "
                        + response.statusCode() + ").");
            }

            try {
                Files.move(temporaryFile, installer, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile, installer, StandardCopyOption.REPLACE_EXISTING);
            }
            return installer;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static Path getUpdateDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Paths.get(localAppData, "VpsManager", "updates");
        }
        return Paths.get(System.getProperty("user.home"), ".vpsmanager", "updates");
    }

    private static void offerInstall(JFrame parent, String version, Path installer) {
        if (!parent.isDisplayable()) {
            return;
        }
        Object[] options = {"Install now", "Later"};
        int choice = JOptionPane.showOptionDialog(parent,
                "Version " + version + " has been downloaded.\n\n"
                        + "Install it now? VPS Manager will close while the installer runs.",
                "Update ready",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            installUpdate(parent, installer);
        } else {
            PREFERENCES.put(DEFERRED_VERSION_KEY, version);
        }
    }

    static void installUpdate(JFrame parent, Path installer) {
        try {
            new ProcessBuilder(installer.toAbsolutePath().toString()).start();
            parent.dispose();
        } catch (IOException exception) {
            showMessage(parent, "Could not start the installer:\n" + exception.getMessage(),
                    "Update failed");
        }
    }

    private static boolean isDeferred(String version) {
        return version.equals(PREFERENCES.get(DEFERRED_VERSION_KEY, ""));
    }

    private static void showMessage(JFrame parent, String message, String title) {
        SwingUtilities.invokeLater(() -> {
            if (parent.isDisplayable()) {
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        for (int index = 0; index < 3; index++) {
            int comparison = Integer.compare(Integer.parseInt(leftParts[index]),
                    Integer.parseInt(rightParts[index]));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }
}