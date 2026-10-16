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
        assertTrue(service.isAvailable(), "FFmpeg should be detected via locator");
        assertNotNull(service.getFFmpegExecutable());
    }

    @Test
    void testSystemPropertyOverride() {
        System.setProperty("slide-md.ffmpeg", "custom-ffmpeg-mock.exe");
        try {
            // Since mock doesn't exist on disk, locator should fall back gracefully
            String found = FFmpegLocator.locateFFmpeg();
            assertNotNull(found);
        } finally {
            System.clearProperty("slide-md.ffmpeg");
        }
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
