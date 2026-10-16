package com.slidemd.parser;

import com.slidemd.model.Alignment;
import com.slidemd.model.Slide;
import com.slidemd.model.SlideDeck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownSlideParserTest {

    private MarkdownSlideParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownSlideParser();
    }

    @Test
    void testBasicSlideSplitting() {
        String md = """
                # Slide 1
                Primeiro slide
                ---
                # Slide 2
                Segundo slide
                ---
                # Slide 3
                Terceiro slide
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(3, deck.getSlideCount());
        assertEquals("Slide 1", deck.getSlide(0).getTitle());
        assertEquals("Slide 2", deck.getSlide(1).getTitle());
        assertEquals("Slide 3", deck.getSlide(2).getTitle());
    }

    @Test
    void testFrontmatterDirectives() {
        String md = """
                bg: #1e1e2e
                align: left
                transition: fade
                
                # Slide Estilizado
                Texto alinhado à esquerda
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(1, deck.getSlideCount());
        Slide slide = deck.getSlide(0);
        assertEquals("#1e1e2e", slide.getBackgroundColor());
        assertEquals(Alignment.LEFT, slide.getAlignment());
        assertEquals("fade", slide.getTransition());
        assertEquals("Slide Estilizado", slide.getTitle());
    }

    @Test
    void testVideoDirectivesWithCut() {
        String md = """
                video: demo.mp4
                cut: 00:05 - 00:25
                loop: true
                
                # Slide com Vídeo
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertNotNull(slide.getVideoSpec());
        assertEquals("demo.mp4", slide.getVideoSpec().getSourcePath());
        assertEquals("00:05", slide.getVideoSpec().getCutStart());
        assertEquals("00:25", slide.getVideoSpec().getCutEnd());
        assertTrue(slide.getVideoSpec().isLoop());
        assertTrue(slide.getVideoSpec().hasCut());
        assertFalse(slide.getVideoSpec().hasJoin());
    }

    @Test
    void testVideoDirectivesWithJoin() {
        String md = """
                join: part1.mp4, part2.mp4
                
                # Vídeos Concat
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertNotNull(slide.getVideoSpec());
        assertTrue(slide.getVideoSpec().hasJoin());
        assertEquals(2, slide.getVideoSpec().getJoinFiles().size());
        assertEquals("part1.mp4", slide.getVideoSpec().getJoinFiles().get(0));
        assertEquals("part2.mp4", slide.getVideoSpec().getJoinFiles().get(1));
    }
}
