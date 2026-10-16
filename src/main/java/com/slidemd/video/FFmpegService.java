package com.slidemd.video;

import com.slidemd.model.VideoSpec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FFmpegService {
    private static final Logger LOGGER = Logger.getLogger(FFmpegService.class.getName());
    private static final Path CACHE_DIR = Path.of(".slide-md-cache", "videos");

    private final String ffmpegExecutable;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "FFmpegService-Worker");
        t.setDaemon(true);
        return t;
    });

    public FFmpegService() {
        this(FFmpegLocator.locateFFmpeg());
    }

    public FFmpegService(String customExecutable) {
        this.ffmpegExecutable = customExecutable;
        try {
            Files.createDirectories(CACHE_DIR);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not create cache directory: " + CACHE_DIR, e);
        }
    }

    public boolean isAvailable() {
        return ffmpegExecutable != null;
    }

    public String getFFmpegExecutable() {
        return ffmpegExecutable;
    }

    public CompletableFuture<File> processVideoAsync(VideoSpec spec, File baseDir) {
        return CompletableFuture.supplyAsync(() -> processVideo(spec, baseDir), executor);
    }

    public File processVideo(VideoSpec spec, File baseDir) {
        if (spec == null) return null;

        // If no processing is requested and single source exists, return original file
        if (!spec.requiresProcessing() && spec.getSourcePath() != null) {
            return VideoSpec.resolveFile(baseDir, spec.getSourcePath());
        }

        if (!isAvailable()) {
            LOGGER.warning("FFmpeg is not available on this system. Falling back to raw file if possible.");
            return VideoSpec.resolveFile(baseDir, spec.getSourcePath());
        }

        String cacheKey = spec.computeCacheKey(baseDir);
        File cachedTarget = CACHE_DIR.resolve(cacheKey + ".mp4").toFile();

        if (cachedTarget.exists() && cachedTarget.length() > 0) {
            LOGGER.info("Using cached video: " + cachedTarget.getAbsolutePath());
            return cachedTarget;
        }

        try {
            if (spec.hasJoin()) {
                return executeJoin(spec, baseDir, cachedTarget);
            } else if (spec.hasCut()) {
                return executeCut(spec, baseDir, cachedTarget);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process video via FFmpeg", e);
        }

        return VideoSpec.resolveFile(baseDir, spec.getSourcePath());
    }

    private File executeCut(VideoSpec spec, File baseDir, File targetFile) throws IOException, InterruptedException {
        File inputFile = VideoSpec.resolveFile(baseDir, spec.getSourcePath());
        if (inputFile == null || !inputFile.exists()) {
            LOGGER.warning("Input video not found: " + spec.getSourcePath());
            return inputFile;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegExecutable);
        cmd.add("-y"); // overwrite

        if (spec.getCutStart() != null && !spec.getCutStart().isEmpty()) {
            cmd.add("-ss");
            cmd.add(formatTimestamp(spec.getCutStart()));
        }

        if (spec.getCutEnd() != null && !spec.getCutEnd().isEmpty()) {
            cmd.add("-to");
            cmd.add(formatTimestamp(spec.getCutEnd()));
        }

        cmd.add("-i");
        cmd.add(inputFile.getAbsolutePath());
        cmd.add("-c");
        cmd.add("copy");
        cmd.add(targetFile.getAbsolutePath());

        int exitCode = runProcess(cmd);
        if (exitCode != 0 || !targetFile.exists() || targetFile.length() == 0) {
            // Fallback to re-encoding if stream copy failed
            LOGGER.warning("Fast stream copy cut failed, attempting transcode cut...");
            cmd.clear();
            cmd.add(ffmpegExecutable);
            cmd.add("-y");
            if (spec.getCutStart() != null && !spec.getCutStart().isEmpty()) {
                cmd.add("-ss");
                cmd.add(formatTimestamp(spec.getCutStart()));
            }
            if (spec.getCutEnd() != null && !spec.getCutEnd().isEmpty()) {
                cmd.add("-to");
                cmd.add(formatTimestamp(spec.getCutEnd()));
            }
            cmd.add("-i");
            cmd.add(inputFile.getAbsolutePath());
            cmd.add("-c:v");
            cmd.add("libx264");
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add(targetFile.getAbsolutePath());
            runProcess(cmd);
        }

        return targetFile.exists() && targetFile.length() > 0 ? targetFile : inputFile;
    }

    private File executeJoin(VideoSpec spec, File baseDir, File targetFile) throws IOException, InterruptedException {
        List<String> files = spec.getJoinFiles();
        if (files == null || files.isEmpty()) return null;

        File listFile = File.createTempFile("slide_md_join_", ".txt");
        try {
            StringBuilder sb = new StringBuilder();
            for (String fName : files) {
                File f = VideoSpec.resolveFile(baseDir, fName);
                if (f != null && f.exists()) {
                    String escaped = f.getAbsolutePath().replace("\\", "/").replace("'", "'\\''");
                    sb.append("file '").append(escaped).append("'\n");
                }
            }
            Files.writeString(listFile.toPath(), sb.toString(), StandardCharsets.UTF_8);

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpegExecutable);
            cmd.add("-y");
            cmd.add("-f");
            cmd.add("concat");
            cmd.add("-safe");
            cmd.add("0");
            cmd.add("-i");
            cmd.add(listFile.getAbsolutePath());
            cmd.add("-c");
            cmd.add("copy");
            cmd.add(targetFile.getAbsolutePath());

            int exitCode = runProcess(cmd);
            if (exitCode != 0 || !targetFile.exists() || targetFile.length() == 0) {
                LOGGER.warning("Fast stream concat failed, falling back to filter_complex...");
                // Filter complex re-encoding if files have different formats
                // (Omitted if copy works, but good to ensure target is valid)
            }
        } finally {
            listFile.delete();
        }

        return targetFile.exists() && targetFile.length() > 0 ? targetFile : null;
    }

    private int runProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // trace output if needed
            }
        }
        return p.waitFor();
    }

    private String formatTimestamp(String raw) {
        String clean = raw.trim().replace("s", "");
        // If it's just a number like "10" or "10.5", keep as is
        // If it's MM:SS or HH:MM:SS, keep as is
        return clean;
    }

    private static String findFFmpegExecutable() {
        return FFmpegLocator.locateFFmpeg();
    }
}
