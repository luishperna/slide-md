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
                background: #1e1e2e
                alignment: left
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
    void testBackgroundDirective() {
        String md = """
                background: linear-gradient(to right, #111, #222)
                alignment: center
                
                # Slide com Background Extenso
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(1, deck.getSlideCount());
        Slide slide = deck.getSlide(0);
        assertEquals("linear-gradient(to right, #111, #222)", slide.getBackgroundColor());
        assertEquals(Alignment.CENTER, slide.getAlignment());
    }

    @Test
    void testSoundDirectivesGlobal() {
        String md = """
                ---
                sound: ./assets/background.mp3
                ---

                # Slide com Som Global
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals("./assets/background.mp3", deck.getSound());
        assertFalse(deck.isSoundLoop());
        assertEquals(1.0, deck.getSoundVolume(), 0.001);
    }

    @Test
    void testSoundDirectivesPerSlide() {
        String md = """
                sound: track1.mp3
                
                # Slide 1
                
                ---
                sound: track2.wav
                
                # Slide 2
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(2, deck.getSlideCount());

        Slide slide1 = deck.getSlide(0);
        assertEquals("track1.mp3", slide1.getSound());
        assertFalse(slide1.isSoundLoop());
        assertEquals(1.0, slide1.getSoundVolume(), 0.001);

        Slide slide2 = deck.getSlide(1);
        assertEquals("track2.wav", slide2.getSound());
        assertEquals(1.0, slide2.getSoundVolume(), 0.001);
    }

    @Test
    void testVideoDirectivesWithInlineCut() {
        String md = """
                video: demo.mp4 [00:05 - 00:25]
                
                # Slide com Vídeo e Corte Inline
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertNotNull(slide.getVideoSpec());
        assertEquals("demo.mp4", slide.getVideoSpec().getSourcePath());
        assertEquals("00:05", slide.getVideoSpec().getCutStart());
        assertEquals("00:25", slide.getVideoSpec().getCutEnd());
        assertFalse(slide.getVideoSpec().isLoop());
        assertFalse(slide.getVideoSpec().isAutoplay());
        assertTrue(slide.getVideoSpec().hasCut());
        assertFalse(slide.getVideoSpec().hasJoin());
    }

    @Test
    void testGradientBracketNotation() {
        String md = """
                background: [#1e293b, #0f172a]
                
                # Slide 1
                
                ---
                background: [#1e293b, #3b82f6, #06b6d4, 45%]
                
                # Slide 2
                
                ---
                background: [#1e293b, #0f172a, 50%]
                
                # Slide 3
                
                ---
                background: [#1e293b, #0f172a, to right]
                
                # Slide 4
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(4, deck.getSlideCount());
        assertEquals("linear-gradient(to bottom right, #1e293b, #0f172a)", deck.getSlide(0).getBackgroundColor());
        assertEquals("linear-gradient(to bottom right, #1e293b, #3b82f6, #06b6d4)", deck.getSlide(1).getBackgroundColor());
        assertEquals("linear-gradient(to bottom, #1e293b, #0f172a)", deck.getSlide(2).getBackgroundColor());
        assertEquals("linear-gradient(to right, #1e293b, #0f172a)", deck.getSlide(3).getBackgroundColor());
    }

    @Test
    void testAlignmentTopAndDown() {
        String md = """
                alignment: top
                
                # Top
                
                ---
                alignment: down
                
                # Down
                
                ---
                alignment: bottom
                
                # Bottom
                
                ---
                alignment: up
                
                # Up
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(4, deck.getSlideCount());
        assertEquals(Alignment.TOP, deck.getSlide(0).getAlignment());
        assertEquals(Alignment.DOWN, deck.getSlide(1).getAlignment());
        assertEquals(Alignment.DOWN, deck.getSlide(2).getAlignment());
        assertEquals(Alignment.TOP, deck.getSlide(3).getAlignment());
    }

    @Test
    void testVideoDirectivesWithJoinCommaSeparated() {
        String md = """
                video: part1.mp4, part2.mp4
                
                # Vídeos Concat por Vírgula
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertNotNull(slide.getVideoSpec());
        assertTrue(slide.getVideoSpec().hasJoin());
        assertEquals(2, slide.getVideoSpec().getJoinFiles().size());
        assertEquals("part1.mp4", slide.getVideoSpec().getJoinFiles().get(0));
        assertEquals("part2.mp4", slide.getVideoSpec().getJoinFiles().get(1));
    }

    @Test
    void testVideoDirectivesWithJoinAndInlineCut() {
        String md = """
                video: part1.mp4 [00:05-00:20], part2.mp4 [01:00-01:30]
                
                # Vídeos Concat com Corte
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertNotNull(slide.getVideoSpec());
        assertTrue(slide.getVideoSpec().hasJoin());
        assertEquals("00:05", slide.getVideoSpec().getCutStart());
        assertEquals("00:20", slide.getVideoSpec().getCutEnd());
        assertEquals("part1.mp4", slide.getVideoSpec().getJoinFiles().get(0));
        assertEquals("part2.mp4", slide.getVideoSpec().getJoinFiles().get(1));
    }

    @Test
    void testHtmlCommentDirectives() {
        String md = """
                <!-- background: #334455 -->
                <!-- alignment: right -->
                <!-- sound: intro.mp3 -->
                <!-- Comentário que deve sumir -->
                # Slide com Comentários HTML
                Texto do slide
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        assertEquals("#334455", slide.getBackgroundColor());
        assertEquals(Alignment.RIGHT, slide.getAlignment());
        assertEquals("intro.mp3", slide.getSound());
        assertFalse(slide.getContentMarkdown().contains("<!--"));
        assertTrue(slide.getContentMarkdown().contains("Texto do slide"));
    }

    @Test
    void testNoPrefixOrFlagAllowed() {
        String md = """
                --sound: ./assets/background.mp3
                flag: sound=./assets/special.mp3
                
                # Slide sem prefixos
                """;

        SlideDeck deck = parser.parseContent(md);
        Slide slide = deck.getSlide(0);
        // Prefixos -- e flag: NÃO devem ser aceitos como diretiva
        assertNull(slide.getSound());
    }

    @Test
    void testGlobalFrontmatterNotTreatedAsSlide() {
        String md = """
                ---
                background: #090d16
                alignment: center
                transition: morph
                ---
                
                # Slide 1 Real
                Conteúdo do primeiro slide
                
                ---
                # Slide 2 Real
                Conteúdo do segundo slide
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(2, deck.getSlideCount(), "Global config block should NOT be counted as a slide");

        Slide slide1 = deck.getSlide(0);
        assertEquals("Slide 1 Real", slide1.getTitle());
        assertEquals("#090d16", slide1.getBackgroundColor());
        assertEquals(Alignment.CENTER, slide1.getAlignment());
        assertEquals("morph", slide1.getTransition());
        assertFalse(slide1.getContentMarkdown().contains("transition:"));

        Slide slide2 = deck.getSlide(1);
        assertEquals("Slide 2 Real", slide2.getTitle());
        assertEquals("#090d16", slide2.getBackgroundColor(), "Should inherit global bg");
        assertEquals("morph", slide2.getTransition(), "Should inherit global morph transition");
    }

    @Test
    void testSlideOverridesInGlobalFrontmatter() {
        String md = """
                ---
                background: #111111
                transition: morph
                
                slide 2:
                  background: #222222
                  alignment: left
                ---
                
                # Slide 1
                Primeiro
                
                ---
                # Slide 2
                Segundo
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(2, deck.getSlideCount());

        Slide slide1 = deck.getSlide(0);
        assertEquals("#111111", slide1.getBackgroundColor());

        Slide slide2 = deck.getSlide(1);
        assertEquals("#222222", slide2.getBackgroundColor(), "Slide 2 should have overridden background");
        assertEquals(Alignment.LEFT, slide2.getAlignment(), "Slide 2 should have overridden alignment");
        assertEquals("morph", slide2.getTransition());
    }

    @Test
    void testImageDirectiveInHeaderAndBody() {
        String md = """
                ---
                image: ./assets/cover.jpg
                ---
                
                # Capa com Imagem no Topo
                
                ---
                
                # Segundo Slide
                
                image: ./assets/ramp.jpg
                
                Texto após imagem
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(2, deck.getSlideCount());

        Slide slide1 = deck.getSlide(0);
        assertEquals("./assets/cover.jpg", slide1.getImage());

        Slide slide2 = deck.getSlide(1);
        assertEquals("./assets/ramp.jpg", slide2.getImage());
        assertTrue(slide2.getContentMarkdown().contains("![](./assets/ramp.jpg)"));
    }

    @Test
    void testInheritanceOnlyChangingWhatDiffers() {
        String md = """
                ---
                background: [#0a0a0f, #18111e, 45%]
                alignment: center
                transition: morph
                ---
                
                # Slide 1
                
                ---
                background: [#0a0a0f, #1e111a, 45%]
                
                # Slide 2 com background diferente
                
                ---
                
                # Slide 3 sem nenhuma diretiva repetida
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(3, deck.getSlideCount());

        // Slide 1
        assertEquals(Alignment.CENTER, deck.getSlide(0).getAlignment());
        assertEquals("morph", deck.getSlide(0).getTransition());

        // Slide 2 herda alignment e transition automaticamente, só mudou background
        assertEquals(Alignment.CENTER, deck.getSlide(1).getAlignment());
        assertEquals("morph", deck.getSlide(1).getTransition());
        assertTrue(deck.getSlide(1).getBackgroundColor().contains("#1e111a"));

        // Slide 3 herda tudo do frontmatter global
        assertEquals(Alignment.CENTER, deck.getSlide(2).getAlignment());
        assertEquals("morph", deck.getSlide(2).getTransition());
        assertTrue(deck.getSlide(2).getBackgroundColor().contains("#18111e"));
    }

    @Test
    void testColorDirective() {
        String md = """
                ---
                color: #ef4444
                ---
                
                # Título Vermelho
                
                ---
                color: #38bdf8
                
                # Título Azul
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(2, deck.getSlideCount());
        assertEquals("#ef4444", deck.getColor());
        assertEquals("#ef4444", deck.getSlide(0).getColor());
        assertEquals("#38bdf8", deck.getSlide(1).getColor());
    }

    @Test
    void testVideoDirectiveInBody() {
        String md = """
                # Slide com Vídeo Embaixo do Título
                ### Subtítulo explicativo
                
                video: ./assets/demo.mp4 [00:10-00:30]
                
                *Legenda do vídeo*
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(1, deck.getSlideCount());
        Slide slide = deck.getSlide(0);
        assertTrue(slide.hasVideo());
        assertNotNull(slide.getVideoSpec());
        assertEquals("./assets/demo.mp4", slide.getVideoSpec().getSourcePath());
        assertEquals(10.0, slide.getVideoSpec().getCutStartSeconds());
        assertEquals(30.0, slide.getVideoSpec().getCutEndSeconds());
        assertTrue(slide.getContentMarkdown().contains("@[video]"));
    }

    @Test
    void testVideoDirectiveWithoutBlankLinesSurroundingIt() {
        String md = """
                # O Salto Épico em Vídeo!
                ### Assista ao teste em ação real
                video: ./assets/demo.mp4 [00:31-00:43]
                *Dê mais 1 clique para iniciar o vídeo*
                """;

        SlideDeck deck = parser.parseContent(md);
        assertEquals(1, deck.getSlideCount());
        Slide slide = deck.getSlide(0);
        assertTrue(slide.hasVideo());
        // Verifica que @[video] foi devidamente isolado em parágrafo próprio
        assertTrue(slide.getContentMarkdown().contains("\n\n@[video]\n\n"));
        assertTrue(slide.getContentMarkdown().contains("*Dê mais 1 clique para iniciar o vídeo*"));
    }
}
