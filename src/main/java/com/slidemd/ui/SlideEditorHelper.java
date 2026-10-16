package com.slidemd.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitários para identificação e manipulação de slides no editor Markdown.
 * Alinhado rigorosamente com o algoritmo de parsing do MarkdownSlideParser.
 */
public final class SlideEditorHelper {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^\\s*---\\r?\\n(.*?)\\r?\\n---\\r?\\n?", Pattern.DOTALL);
    private static final Pattern DELIMITER_PATTERN = Pattern.compile("(?m)^[ \\t]*---[ \\t]*$");
    private static final Pattern BG_PATTERN = Pattern.compile("(?m)^[ \\t]*bg:[ \\t]*(.*)$");

    private SlideEditorHelper() {}

    /**
     * Intervalo de um slide no texto do documento (início e fim, inclusive/exclusive).
     */
    public record SlideRange(int startIndex, int endIndex, int slideIndex) {}

    /**
     * Localiza todos os intervalos de slides no documento Markdown.
     * Trata o frontmatter global como parte do primeiro slide (slide 0) para sincronização perfeita.
     */
    public static List<SlideRange> findSlideRanges(String text) {
        List<SlideRange> ranges = new ArrayList<>();
        if (text == null || text.isBlank()) {
            ranges.add(new SlideRange(0, 0, 0));
            return ranges;
        }

        int contentStart = 0;
        Matcher fmM = FRONTMATTER_PATTERN.matcher(text);
        if (fmM.find()) {
            contentStart = fmM.end();
        }

        String remaining = text.substring(contentStart);
        Matcher matcher = DELIMITER_PATTERN.matcher(remaining);

        int prevEnd = 0;
        int slideIndex = 0;

        while (matcher.find()) {
            int delimiterStart = matcher.start();
            int startInDoc = (slideIndex == 0) ? 0 : (contentStart + prevEnd);
            int endInDoc = contentStart + delimiterStart;

            ranges.add(new SlideRange(startInDoc, endInDoc, slideIndex++));

            prevEnd = matcher.end();
            if (prevEnd < remaining.length() && remaining.charAt(prevEnd) == '\r') prevEnd++;
            if (prevEnd < remaining.length() && remaining.charAt(prevEnd) == '\n') prevEnd++;
        }

        int lastStart = (slideIndex == 0) ? 0 : (contentStart + prevEnd);
        ranges.add(new SlideRange(lastStart, text.length(), slideIndex));

        return ranges;
    }

    /**
     * Identifica o índice do slide (0-based) onde o cursor está localizado.
     */
    public static int getSlideIndexAtCaret(String text, int caret) {
        List<SlideRange> ranges = findSlideRanges(text);
        int clampedCaret = Math.max(0, Math.min(caret, text != null ? text.length() : 0));
        for (SlideRange range : ranges) {
            if (clampedCaret >= range.startIndex && clampedCaret <= range.endIndex) {
                return range.slideIndex;
            }
        }
        return Math.max(0, ranges.size() - 1);
    }

    /**
     * Retorna a posição do caractere no texto onde o conteúdo do slide indicado inicia.
     */
    public static int getSlideStartCaret(String text, int slideIndex) {
        if (text == null || text.isBlank()) return 0;
        List<SlideRange> ranges = findSlideRanges(text);
        if (ranges.isEmpty()) return 0;
        if (slideIndex <= 0) {
            // Se houver frontmatter global no slide 0, posiciona no início do conteúdo real do slide
            Matcher fmM = FRONTMATTER_PATTERN.matcher(text);
            if (fmM.find()) {
                return fmM.end();
            }
            return 0;
        }

        int target = Math.min(slideIndex, ranges.size() - 1);
        SlideRange r = ranges.get(target);
        return r.startIndex;
    }

    /**
     * Remove o slide onde o cursor está localizado.
     * Se for o único slide, reinicia com um slide vazio padrão.
     */
    public static String removeSlideAtCaret(String text, int caret) {
        if (text == null || text.isBlank()) {
            return "# Novo Slide\n";
        }

        List<SlideRange> ranges = findSlideRanges(text);
        if (ranges.size() <= 1) {
            return "# Novo Slide\n";
        }

        int targetIndex = getSlideIndexAtCaret(text, caret);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ranges.size(); i++) {
            if (i == targetIndex) continue;

            SlideRange r = ranges.get(i);
            String slideContent = text.substring(r.startIndex, r.endIndex).trim();

            if (sb.length() > 0) {
                sb.append("\n\n---\n\n");
            }
            sb.append(slideContent);
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Insere um novo slide logo após o slide onde o cursor está posicionado.
     */
    public static String insertSlideAfterCaret(String text, int caret) {
        if (text == null || text.isBlank()) {
            return "# Novo Slide\n";
        }

        List<SlideRange> ranges = findSlideRanges(text);
        int targetIndex = getSlideIndexAtCaret(text, caret);
        SlideRange targetRange = ranges.get(targetIndex);

        int insertPosition = targetRange.endIndex;
        String prefix = text.substring(0, insertPosition).stripTrailing();
        String suffix = text.substring(insertPosition).stripLeading();

        StringBuilder sb = new StringBuilder(prefix);
        sb.append("\n\n---\n\n# Novo Slide\n\n");
        if (!suffix.isEmpty()) {
            sb.append(suffix);
        }
        return sb.toString();
    }

    /**
     * Aplica ou atualiza a diretiva de fundo (bg) no slide onde o cursor está posicionado.
     */
    public static String applyBackgroundToCurrentSlide(String text, int caret, String bgValue) {
        if (text == null) text = "";
        if (text.isBlank()) {
            return "---\nbg: " + bgValue + "\n---\n\n# Novo Slide\n";
        }

        List<SlideRange> ranges = findSlideRanges(text);
        int targetIndex = getSlideIndexAtCaret(text, caret);
        SlideRange targetRange = ranges.get(targetIndex);

        String currentSlideText = text.substring(targetRange.startIndex, targetRange.endIndex);
        Matcher bgMatcher = BG_PATTERN.matcher(currentSlideText);

        String updatedSlideText;
        if (bgMatcher.find()) {
            updatedSlideText = bgMatcher.replaceFirst("bg: " + bgValue);
        } else {
            String trimmed = currentSlideText.stripLeading();
            if (trimmed.startsWith("---")) {
                updatedSlideText = "\nbg: " + bgValue + "\n" + trimmed;
            } else {
                updatedSlideText = "bg: " + bgValue + "\n" + currentSlideText;
            }
        }

        return text.substring(0, targetRange.startIndex) + updatedSlideText + text.substring(targetRange.endIndex);
    }
}
