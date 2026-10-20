package com.slidemd.model;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineOptionsTest {

    @Test
    void testParseFlagMusicWithSpace() {
        var options = CommandLineOptions.parse(List.of("demo.md", "--music", "assets/bgm.mp3"), Map.of());
        assertNotNull(options.markdownFile());
        assertEquals("demo.md", options.markdownFile().getName());
        assertEquals("assets/bgm.mp3", options.musicPath());
        assertTrue(options.musicLoop());
        assertEquals(0.8, options.musicVolume(), 0.001);
    }

    @Test
    void testParseFlagMusicWithEquals() {
        var options = CommandLineOptions.parse(List.of("--music=assets/soundtrack.mp3", "demo.md"), Map.of());
        assertNotNull(options.markdownFile());
        assertEquals("demo.md", options.markdownFile().getName());
        assertEquals("assets/soundtrack.mp3", options.musicPath());
    }

    @Test
    void testParseShortFlagM() {
        var options = CommandLineOptions.parse(List.of("-m", "theme.wav", "presentation.md"), Map.of());
        assertEquals("theme.wav", options.musicPath());
        assertEquals("presentation.md", options.markdownFile().getName());
    }

    @Test
    void testParseNamedJavaFXArgs() {
        var options = CommandLineOptions.parse(
                List.of("demo.md"),
                Map.of("music", "bgm.mp3", "music-loop", "false", "music-volume", "50%")
        );
        assertEquals("bgm.mp3", options.musicPath());
        assertFalse(options.musicLoop());
        assertEquals(0.5, options.musicVolume(), 0.001);
    }

    @Test
    void testParseOnlyMusicFlagWithoutMarkdownFile() {
        var options = CommandLineOptions.parse(List.of("--music", "ambient.mp3"), Map.of());
        assertNull(options.markdownFile());
        assertEquals("ambient.mp3", options.musicPath());
    }
}
