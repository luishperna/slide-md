package com.slidemd.video;

import com.slidemd.model.VideoSpec;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FFmpegServiceTest {

    @Test
    void testFFmpegServiceDetection() {
        FFmpegService service = new FFmpegService();
        // Since C:\yt-dlp\ffmpeg.exe exists, it should be available
        assertTrue(service.isAvailable(), "FFmpeg should be detected on the system");
        assertNotNull(service.getFFmpegExecutable());
    }

    @Test
    void testVideoSpecRequiresProcessing() {
        VideoSpec raw = new VideoSpec("clip.mp4", null, null, null, true, false);
        assertFalse(raw.requiresProcessing());

        VideoSpec cut = new VideoSpec("clip.mp4", "00:05", "00:15", null, true, false);
        assertTrue(cut.requiresProcessing());
        assertTrue(cut.hasCut());
        assertFalse(cut.hasJoin());

        VideoSpec join = new VideoSpec(null, null, null, List.of("v1.mp4", "v2.mp4"), true, false);
        assertTrue(join.requiresProcessing());
        assertTrue(join.hasJoin());
        assertFalse(join.hasCut());
    }

    @Test
    void testCacheKeyDeterminism() {
        VideoSpec spec1 = new VideoSpec("clip.mp4", "00:05", "00:15", null, true, false);
        VideoSpec spec2 = new VideoSpec("clip.mp4", "00:05", "00:15", null, true, false);
        VideoSpec spec3 = new VideoSpec("clip.mp4", "00:05", "00:20", null, true, false);

        File baseDir = new File(".");
        assertEquals(spec1.computeCacheKey(baseDir), spec2.computeCacheKey(baseDir));
        assertNotEquals(spec1.computeCacheKey(baseDir), spec3.computeCacheKey(baseDir));
    }
}
