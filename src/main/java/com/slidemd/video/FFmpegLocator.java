package com.slidemd.video;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FFmpegLocator {
    private static final Logger LOGGER = Logger.getLogger(FFmpegLocator.class.getName());

    public static String locateFFmpeg() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        String exeName = isWindows ? "ffmpeg.exe" : "ffmpeg";

        // 1. Check System Property override (-Dslide-md.ffmpeg=... or -Dffmpeg.path=...)
        String propPath = System.getProperty("slide-md.ffmpeg", System.getProperty("ffmpeg.path"));
        if (propPath != null && !propPath.isBlank()) {
            File f = new File(propPath.trim());
            if (f.exists() && f.canExecute()) {
                LOGGER.info("Using FFmpeg from system property: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        }

        // 2. Check Environment Variable
        String envPath = System.getenv("SLIDEMD_FFMPEG");
        if (envPath == null || envPath.isBlank()) {
            envPath = System.getenv("FFMPEG_PATH");
        }
        if (envPath != null && !envPath.isBlank()) {
            File f = new File(envPath.trim());
            if (f.exists() && f.canExecute()) {
                LOGGER.info("Using FFmpeg from environment variable: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        }

        // 3. Check App Bundled Directory (for jpackage / portable distributions)
        List<File> appLocations = getAppCandidateDirectories();
        for (File dir : appLocations) {
            File direct = new File(dir, exeName);
            if (direct.exists() && direct.canExecute()) {
                LOGGER.info("Using bundled FFmpeg: " + direct.getAbsolutePath());
                return direct.getAbsolutePath();
            }

            File inBin = new File(new File(dir, "bin"), exeName);
            if (inBin.exists() && inBin.canExecute()) {
                LOGGER.info("Using bundled FFmpeg in bin: " + inBin.getAbsolutePath());
                return inBin.getAbsolutePath();
            }

            if (isMac) {
                // macOS app bundle: Contents/Resources/ffmpeg or Contents/MacOS/ffmpeg
                File inResources = new File(new File(dir, "Resources"), exeName);
                if (inResources.exists() && inResources.canExecute()) {
                    return inResources.getAbsolutePath();
                }
            }
        }

        // 4. Check User Home directory (~/.slide-md/bin/ffmpeg)
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File userBin = Path.of(userHome, ".slide-md", "bin", exeName).toFile();
            if (userBin.exists() && userBin.canExecute()) {
                LOGGER.info("Using FFmpeg from user home: " + userBin.getAbsolutePath());
                return userBin.getAbsolutePath();
            }
        }

        // 5. Check System PATH via OS command (where / which)
        String fromPath = findInSystemPath(isWindows, exeName);
        if (fromPath != null) {
            LOGGER.info("Using FFmpeg from system PATH: " + fromPath);
            return fromPath;
        }

        // 6. Check well-known Linux / macOS paths
        if (!isWindows) {
            String[] unixStandardPaths = {
                "/usr/local/bin/ffmpeg",
                "/opt/homebrew/bin/ffmpeg", // Apple Silicon Homebrew
                "/usr/bin/ffmpeg",
                "/bin/ffmpeg"
            };
            for (String p : unixStandardPaths) {
                File f = new File(p);
                if (f.exists() && f.canExecute()) {
                    return f.getAbsolutePath();
                }
            }
        }

        LOGGER.warning("FFmpeg was not found in bundled app directories, environment, or system PATH.");
        return null;
    }

    private static List<File> getAppCandidateDirectories() {
        List<File> candidates = new ArrayList<>();

        // jpackage application directory
        String jpackageAppDir = System.getProperty("app.dir");
        if (jpackageAppDir != null) {
            candidates.add(new File(jpackageAppDir));
        }

        // Working directory
        candidates.add(new File("."));

        // Location of the running JAR / Class
        try {
            URL location = FFmpegLocator.class.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File jarOrClasses = new File(location.toURI());
                File parent = jarOrClasses.getParentFile();
                if (parent != null) {
                    candidates.add(parent); // target/ or app/
                    if (parent.getParentFile() != null) {
                        candidates.add(parent.getParentFile()); // app root
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return candidates;
    }

    private static String findInSystemPath(boolean isWindows, String exeName) {
        try {
            ProcessBuilder pb = isWindows
                ? new ProcessBuilder("where.exe", exeName)
                : new ProcessBuilder("which", exeName);

            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    File found = new File(line.trim());
                    if (found.exists() && (isWindows || found.canExecute())) {
                        return found.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
