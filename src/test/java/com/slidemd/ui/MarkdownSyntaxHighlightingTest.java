package com.slidemd.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class MarkdownSyntaxHighlightingTest {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<DIVIDER>^\\s*---\\s*$)"
            + "|(?<HEADER>^\\s*#{1,6}\\s+.*$)"
            + "|(?<DIRECTIVE>^\\s*(bg|align|transition|video|cut|join|loop|autoplay|slide\\s+\\d+):.*$)"
            + "|(?<QUOTE>^\\s*>.*$)"
            + "|(?<LIST>^\\s*([-*+]|\\d+\\.)\\s+)"
            + "|(?<CODE>`[^`\\n]+`|```[\\s\\S]*?```)"
            + "|(?<BOLD>\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__)",
            Pattern.MULTILINE
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return StyleSpans.singleton(Collections.emptyList(), 0);
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("DIVIDER") != null ? "md-divider" :
                    matcher.group("HEADER") != null ? "md-header" :
                    matcher.group("DIRECTIVE") != null ? "md-directive" :
                    matcher.group("QUOTE") != null ? "md-quote" :
                    matcher.group("LIST") != null ? "md-list" :
                    matcher.group("CODE") != null ? "md-code" :
                    matcher.group("BOLD") != null ? "md-bold" : null;

            if (matcher.start() > lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            }
            int matchLen = matcher.end() - matcher.start();
            if (matchLen > 0) {
                spansBuilder.add(Collections.singleton(styleClass), matchLen);
            }
            lastKwEnd = matcher.end();
        }
        if (text.length() > lastKwEnd) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        }
        return spansBuilder.create();
    }

    @Test
    void testEmptyText() {
        StyleSpans<Collection<String>> spans = computeHighlighting("");
        assertNotNull(spans);
        assertEquals(0, spans.length());
    }

    @Test
    void testMarkdownHighlighting() {
        String md = """
                ---
                bg: #000000
                ---
                # Slide Title
                - Item 1
                > Quote
                `code`
                """;
        StyleSpans<Collection<String>> spans = computeHighlighting(md);
        assertNotNull(spans);
        assertEquals(md.length(), spans.length());
    }
}
