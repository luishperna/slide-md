package com.slidemd.model;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

public class VideoSpec {
    private final String sourcePath;
    private final String cutStart;
    private final String cutEnd;
    private final List<String> joinFiles;
    private final boolean autoplay;
    private final boolean loop;

    public VideoSpec(String sourcePath, String cutStart, String cutEnd, List<String> joinFiles, boolean autoplay, boolean loop) {
        this.sourcePath = sourcePath;
        this.cutStart = cutStart != null ? cutStart.trim() : null;
        this.cutEnd = cutEnd != null ? cutEnd.trim() : null;
        this.joinFiles = joinFiles != null ? new ArrayList<>(joinFiles) : Collections.emptyList();
        this.autoplay = autoplay;
        this.loop = loop;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getCutStart() {
        return cutStart;
    }

    public String getCutEnd() {
        return cutEnd;
    }

    public List<String> getJoinFiles() {
        return Collections.unmodifiableList(joinFiles);
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean hasCut() {
        return cutStart != null && !cutStart.isEmpty() || (cutEnd != null && !cutEnd.isEmpty());
    }

    public boolean hasJoin() {
        return joinFiles != null && !joinFiles.isEmpty();
    }

    public boolean requiresProcessing() {
        return hasCut() || hasJoin();
    }

    public String computeCacheKey(File baseDir) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append("source=").append(sourcePath).append(";");
            sb.append("cutStart=").append(cutStart).append(";");
            sb.append("cutEnd=").append(cutEnd).append(";");
            if (joinFiles != null) {
                for (String jf : joinFiles) {
                    sb.append("join=").append(jf).append(";");
                }
            }

            // Include file timestamp if file exists
            if (sourcePath != null) {
                File f = resolveFile(baseDir, sourcePath);
                if (f.exists()) {
                    sb.append("srcMod=").append(f.lastModified()).append(";srcLen=").append(f.length()).append(";");
                }
            }

            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(hashCode());
        }
    }

    public static File resolveFile(File baseDir, String path) {
        if (path == null) return null;
        File file = new File(path);
        if (!file.isAbsolute() && baseDir != null) {
            file = new File(baseDir, path);
        }
        return file;
    }
}
