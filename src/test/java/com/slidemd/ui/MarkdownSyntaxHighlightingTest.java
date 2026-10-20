package com.slidemd.ui;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class MarkdownSyntaxHighlightingTest {

    private static final Pattern PATTERN = Pattern.compile(
            "(?<DIVIDER>^[ \\t]*---[ \\t]*$)"
            + "|(?<COMMENT><!--[\\s\\S]*?-->)"
            + "|(?<DIRECTIVE>^(?<DIRKEY>[ \\t]*(?:background|alignment|transition|video|loop|autoplay|sound|sound-loop|sound-volume|image|color|slide\\s+\\d+):)(?<DIRVAL>[^\\n]*))"
            + "|(?<HEADER>^[ \\t]*#{1,6}[ \\t]+.*$)"
            + "|(?<QUOTE>^[ \\t]*>.*$)"
            + "|(?<LIST>^[ \\t]*([-*+]|\\d+\\.)[ \\t]+)"
            + "|(?<CODE>`[^`\\n]+`|```[\\s\\S]*?```)"
            + "|(?<BOLD>\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__)"
            + "|(?<ITALIC>(?<!\\*)\\*[^*\\n\\r]+\\*(?!\\*)|(?<!_)_[^_\\n\\r]+_(?!_))",
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
            if (matcher.group("DIRECTIVE") != null) {
                int keyStart = matcher.start("DIRKEY");
                int keyEnd = matcher.end("DIRKEY");
                int valStart = matcher.start("DIRVAL");
                int valEnd = matcher.end("DIRVAL");

                if (keyStart > lastKwEnd) {
                    spansBuilder.add(Collections.emptyList(), keyStart - lastKwEnd);
                }
                if (keyEnd > keyStart) {
                    spansBuilder.add(Collections.singleton("md-directive-key"), keyEnd - keyStart);
                }
                if (valEnd > valStart) {
                    spansBuilder.add(Collections.singleton("md-directive-val"), valEnd - valStart);
                }
                lastKwEnd = valEnd;
                continue;
            }

            String styleClass =
                    matcher.group("DIVIDER") != null ? "md-divider" :
                    matcher.group("COMMENT") != null ? "md-comment" :
                    matcher.group("HEADER") != null ? "md-header" :
                    matcher.group("QUOTE") != null ? "md-quote" :
                    matcher.group("LIST") != null ? "md-list" :
                    matcher.group("CODE") != null ? "md-code" :
                    matcher.group("BOLD") != null ? "md-bold" :
                    matcher.group("ITALIC") != null ? "md-italic" : null;

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
                color: #ef4444
                background: #000000
                ---
                # Slide Title
                - Item 1
                > Quote
                `code`
                """;
        StyleSpans<Collection<String>> spans = computeHighlighting(md);
        assertNotNull(spans);
        assertEquals(md.length(), spans.length());

        List<String> allClasses = spans.stream()
                .flatMap(span -> span.getStyle().stream())
                .toList();
        assertTrue(allClasses.contains("md-divider"));
        assertTrue(allClasses.contains("md-directive-key"));
        assertTrue(allClasses.contains("md-directive-val"));
        assertTrue(allClasses.contains("md-header"));
        assertTrue(allClasses.contains("md-list"));
        assertTrue(allClasses.contains("md-quote"));
        assertTrue(allClasses.contains("md-code"));
    }
}
