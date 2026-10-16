package com.slidemd.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitários puros para manipulação de texto de slides no editor Markdown.
 */
public final class SlideEditorHelper {

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("(?m)^[ \\t]*---[ \\t]*$");
    private static final Pattern BG_PATTERN = Pattern.compile("(?m)^[ \\t]*bg:[ \\t]*(.*)$");

    private SlideEditorHelper() {}

    /**
     * Intervalo de um slide no texto do documento (início e fim, inclusive/exclusive).
     */
    public record SlideRange(int startIndex, int endIndex, int slideNumber) {}

    /**
     * Localiza todos os intervalos de slides delimitados por '---'.
     */
    public static List<SlideRange> findSlideRanges(String text) {
        List<SlideRange> ranges = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            ranges.add(new SlideRange(0, 0, 1));
            return ranges;
        }

        Matcher matcher = DELIMITER_PATTERN.matcher(text);
        int prevEnd = 0;
        int slideNumber = 1;

        while (matcher.find()) {
            int delimiterStart = matcher.start();
            ranges.add(new SlideRange(prevEnd, delimiterStart, slideNumber++));
            prevEnd = matcher.end();
            // Pula a quebra de linha após o delimitador se houver
            if (prevEnd < text.length() && text.charAt(prevEnd) == '\r') prevEnd++;
            if (prevEnd < text.length() && text.charAt(prevEnd) == '\n') prevEnd++;
        }

        ranges.add(new SlideRange(prevEnd, text.length(), slideNumber));
        return ranges;
    }

    /**
     * Identifica o índice do slide onde o cursor está localizado.
     */
    public static int getSlideIndexAtCaret(String text, int caret) {
        List<SlideRange> ranges = findSlideRanges(text);
        int clampedCaret = Math.max(0, Math.min(caret, text != null ? text.length() : 0));
        for (int i = 0; i < ranges.size(); i++) {
            SlideRange range = ranges.get(i);
            if (clampedCaret >= range.startIndex && clampedCaret <= range.endIndex) {
                return i;
            }
        }
        return Math.max(0, ranges.size() - 1);
    }

    /**
     * Retorna a posição do caractere onde o slide indicado inicia.
     */
    public static int getSlideStartCaret(String text, int slideIndex) {
        List<SlideRange> ranges = findSlideRanges(text);
        if (ranges.isEmpty()) return 0;
        if (slideIndex <= 0) return 0;
        if (slideIndex >= ranges.size()) return ranges.get(ranges.size() - 1).startIndex();
        return ranges.get(slideIndex).startIndex();
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
            if (suffix.startsWith("---")) {
                sb.append(suffix);
            } else {
                sb.append(suffix);
            }
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
            // Atualiza bg existente
            updatedSlideText = bgMatcher.replaceFirst("bg: " + bgValue);
        } else {
            // Adiciona bg no início do slide
            String trimmed = currentSlideText.stripLeading();
            if (trimmed.startsWith("---")) {
                // Já tem delimitador local
                updatedSlideText = "\nbg: " + bgValue + "\n" + trimmed;
            } else {
                updatedSlideText = "bg: " + bgValue + "\n" + currentSlideText;
            }
        }

        return text.substring(0, targetRange.startIndex) + updatedSlideText + text.substring(targetRange.endIndex);
    }
}
