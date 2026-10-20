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

        assertTrue(result.contains("background: #1e293b"));
        // Slide 1 should not have background
        assertFalse(result.startsWith("background:"));

        // If we apply again, it replaces the background
        String updated = SlideEditorHelper.applyBackgroundToCurrentSlide(result, caret, "#090d16");
        assertTrue(updated.contains("background: #090d16"));
        assertFalse(updated.contains("background: #1e293b"));

        // If replacing an old "bg:" directive, it should update to "background:"
        String oldBgSlide = "# Slide 1\n\n---\nbg: #333333\n# Slide 2";
        int caretOld = oldBgSlide.indexOf("# Slide 2");
        String updatedFromOld = SlideEditorHelper.applyBackgroundToCurrentSlide(oldBgSlide, caretOld, "#444444");
        assertTrue(updatedFromOld.contains("background: #444444"));
        assertFalse(updatedFromOld.contains("bg:"));
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

    @Test
    void testSlideRangesWithGlobalFrontmatter() {
        String md = """
                ---
                bg: #090d16
                transition: slide
                ---

                # Slide 1 (Apresentacao)
                Conteudo do primeiro slide

                ---

                # Slide 2 (Segundo)
                Conteudo do segundo slide
                """;

        List<SlideEditorHelper.SlideRange> ranges = SlideEditorHelper.findSlideRanges(md);
        assertEquals(2, ranges.size());

        // Caret in frontmatter belongs to Slide 0
        assertEquals(0, SlideEditorHelper.getSlideIndexAtCaret(md, 10));
        // Caret in Slide 1 body belongs to Slide 0
        int slide1Body = md.indexOf("# Slide 1");
        assertEquals(0, SlideEditorHelper.getSlideIndexAtCaret(md, slide1Body));

        // Caret in Slide 2 body belongs to Slide 1
        int slide2Body = md.indexOf("# Slide 2");
        assertEquals(1, SlideEditorHelper.getSlideIndexAtCaret(md, slide2Body));
    }

    @Test
    void testGetParagraphIndexAtCaret() {
        String text = "Line 0\nLine 1\nLine 2\nLine 3";
        assertEquals(0, SlideEditorHelper.getParagraphIndexAtCaret(text, 0));
        assertEquals(0, SlideEditorHelper.getParagraphIndexAtCaret(text, 4));
        assertEquals(1, SlideEditorHelper.getParagraphIndexAtCaret(text, 7)); // start of Line 1
        assertEquals(2, SlideEditorHelper.getParagraphIndexAtCaret(text, 14)); // start of Line 2
        assertEquals(3, SlideEditorHelper.getParagraphIndexAtCaret(text, 21)); // start of Line 3
    }

    @Test
    void testGetSlideStartParagraph() {
        String md = "# Slide 1\nTexto do slide 1\n\n---\n\n# Slide 2\nTexto do slide 2\n\n---\n\n# Slide 3\nTexto do slide 3";

        // Slide 0 deve iniciar no topo absoluto (parágrafo 0)
        assertEquals(0, SlideEditorHelper.getSlideStartParagraph(md, 0));

        // Slide 1 deve posicionar no '---' do separador (parágrafo 3)
        int p1 = SlideEditorHelper.getSlideStartParagraph(md, 1);
        String[] lines = md.split("\r?\n");
        assertEquals("---", lines[p1].trim());

        // Slide 2 deve posicionar no segundo '---' (parágrafo 7)
        int p2 = SlideEditorHelper.getSlideStartParagraph(md, 2);
        assertEquals("---", lines[p2].trim());
        assertTrue(p2 > p1);
    }

    @Test
    void testGetSlideStartParagraphWithFrontmatter() {
        String md = """
                ---
                background: #111111
                color: #ffffff
                ---

                # Slide 1
                Conteudo

                ---

                # Slide 2
                Conteudo 2
                """;

        // Slide 0 começa no parágrafo 0
        assertEquals(0, SlideEditorHelper.getSlideStartParagraph(md, 0));

        // Slide 1 deve posicionar no '---' divisor
        int p1 = SlideEditorHelper.getSlideStartParagraph(md, 1);
        String[] lines = md.split("\r?\n");
        assertEquals("---", lines[p1].trim());
    }
}
