package com.slidemd.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlideEditorHelperTest {

    @Test
    void testFindSlideRangesMultipleSlides() {
        String md = "# Slide 1\n\n---\n\n# Slide 2\nConteúdo\n\n---\n\n# Slide 3";
        List<SlideEditorHelper.SlideRange> ranges = SlideEditorHelper.findSlideRanges(md);

        assertEquals(3, ranges.size());
        assertEquals(0, SlideEditorHelper.getSlideIndexAtCaret(md, 5)); // inside slide 1
        assertEquals(1, SlideEditorHelper.getSlideIndexAtCaret(md, 25)); // inside slide 2
        assertEquals(2, SlideEditorHelper.getSlideIndexAtCaret(md, md.length() - 2)); // inside slide 3
    }

    @Test
    void testRemoveMiddleSlide() {
        String md = "# Slide 1\n\n---\n\n# Slide 2\n\n---\n\n# Slide 3";
        // Caret on Slide 2
        int caret = md.indexOf("# Slide 2");
        String result = SlideEditorHelper.removeSlideAtCaret(md, caret);

        assertTrue(result.contains("# Slide 1"));
        assertFalse(result.contains("# Slide 2"));
        assertTrue(result.contains("# Slide 3"));
    }

    @Test
    void testRemoveFirstSlide() {
        String md = "# Slide 1\n\n---\n\n# Slide 2";
        String result = SlideEditorHelper.removeSlideAtCaret(md, 0);

        assertFalse(result.contains("# Slide 1"));
        assertTrue(result.contains("# Slide 2"));
    }

    @Test
    void testRemoveSingleSlideResetsDefault() {
        String md = "# Apenas Um Slide";
        String result = SlideEditorHelper.removeSlideAtCaret(md, 5);

        assertEquals("# Novo Slide\n", result);
    }

    @Test
    void testInsertSlideAfterCaret() {
        String md = "# Slide 1\n\n---\n\n# Slide 2";
        int caret = 5; // on Slide 1
        String result = SlideEditorHelper.insertSlideAfterCaret(md, caret);

        List<SlideEditorHelper.SlideRange> ranges = SlideEditorHelper.findSlideRanges(result);
        assertEquals(3, ranges.size());
        assertTrue(result.contains("# Slide 1"));
        assertTrue(result.contains("# Novo Slide"));
        assertTrue(result.contains("# Slide 2"));
    }

    @Test
    void testApplyBackgroundToSlide() {
        String md = "# Slide 1\n\n---\n\n# Slide 2";
        int caret = md.indexOf("# Slide 2");
        String result = SlideEditorHelper.applyBackgroundToCurrentSlide(md, caret, "#1e293b");

        assertTrue(result.contains("bg: #1e293b"));
        // Slide 1 should not have bg
        assertFalse(result.startsWith("bg:"));

        // If we apply again, it replaces the bg
        String updated = SlideEditorHelper.applyBackgroundToCurrentSlide(result, caret, "#090d16");
        assertTrue(updated.contains("bg: #090d16"));
        assertFalse(updated.contains("bg: #1e293b"));
    }

    @Test
    void testGetSlideStartCaret() {
        String md = "# Slide 1\n\n---\n\n# Slide 2\n\n---\n\n# Slide 3";
        assertEquals(0, SlideEditorHelper.getSlideStartCaret(md, 0));
        int slide2Start = SlideEditorHelper.getSlideStartCaret(md, 1);
        assertTrue(slide2Start > 0);
        assertTrue(md.substring(slide2Start).stripLeading().startsWith("# Slide 2"));

        int slide3Start = SlideEditorHelper.getSlideStartCaret(md, 2);
        assertTrue(slide3Start > slide2Start);
        assertTrue(md.substring(slide3Start).stripLeading().startsWith("# Slide 3"));
    }
}
