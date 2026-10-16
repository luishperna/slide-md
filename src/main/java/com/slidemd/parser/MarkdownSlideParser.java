package com.slidemd.parser;

import com.slidemd.model.Alignment;
import com.slidemd.model.Slide;
import com.slidemd.model.SlideDeck;
import com.slidemd.model.VideoSpec;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownSlideParser {

    private static final Pattern SLIDE_SEPARATOR = Pattern.compile("(?m)^\\s*---\\s*$");
    private static final Pattern HTML_COMMENT_DIRECTIVE = Pattern.compile("<!--\\s*([a-zA-Z0-9_-]+)\\s*:\\s*(.*?)\\s*-->");
    private static final Pattern FRONTMATTER_LINE = Pattern.compile("^([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$");

    private final Parser commonmarkParser;

    public MarkdownSlideParser() {
        this.commonmarkParser = Parser.builder().build();
    }

    public SlideDeck parseFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        SlideDeck deck = parseContent(content);
        deck.setSourceFile(file);
        if (deck.getTitle() == null || deck.getTitle().equals("Untitled Presentation")) {
            deck.setTitle(file.getName().replaceFirst("[.][^.]+$", ""));
        }
        return deck;
    }

    public SlideDeck parseContent(String content) {
        SlideDeck deck = new SlideDeck();
        if (content == null || content.isBlank()) {
            Slide emptySlide = new Slide(0, "");
            emptySlide.setTitle("Novo Slide");
            deck.addSlide(emptySlide);
            return deck;
        }

        // Split into slides
        String[] rawSlides = SLIDE_SEPARATOR.split(content);
        int slideIndex = 0;

        for (String raw : rawSlides) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() && rawSlides.length > 1 && slideIndex == 0) {
                // If the document begins with `---` delimiter, the first item is empty
                continue;
            }

            Slide slide = parseSlide(slideIndex++, trimmed);
            deck.addSlide(slide);

            if (slideIndex == 1 && slide.getTitle() != null && !slide.getTitle().isBlank()) {
                deck.setTitle(slide.getTitle());
            }
        }

        if (deck.getSlideCount() == 0) {
            Slide emptySlide = new Slide(0, "");
            emptySlide.setTitle("Novo Slide");
            deck.addSlide(emptySlide);
        }

        return deck;
    }

    public Slide parseSlide(int index, String rawContent) {
        Slide slide = new Slide(index, rawContent);

        String workingContent = rawContent;

        // 1. Parse HTML comment directives: <!-- bg: #ff0000 -->
        Matcher commentMatcher = HTML_COMMENT_DIRECTIVE.matcher(workingContent);
        while (commentMatcher.find()) {
            String key = commentMatcher.group(1).toLowerCase();
            String value = commentMatcher.group(2).trim();
            applyDirective(slide, key, value);
        }
        workingContent = commentMatcher.replaceAll("");

        // 2. Parse top key-value lines (frontmatter style before first markdown element)
        String[] lines = workingContent.split("\\r?\\n");
        StringBuilder markdownBody = new StringBuilder();
        boolean inHeader = true;

        String videoSource = null;
        String videoCut = null;
        String videoJoin = null;
        boolean videoAutoplay = true;
        boolean videoLoop = false;

        for (String line : lines) {
            String lineTrim = line.trim();
            if (inHeader) {
                if (lineTrim.isEmpty()) {
                    continue;
                }
                Matcher fmM = FRONTMATTER_LINE.matcher(lineTrim);
                if (fmM.matches() && isKnownDirectiveKey(fmM.group(1))) {
                    String key = fmM.group(1).toLowerCase();
                    String val = fmM.group(2).trim();
                    switch (key) {
                        case "video" -> videoSource = val;
                        case "cut", "video-cut" -> videoCut = val;
                        case "join", "video-join" -> videoJoin = val;
                        case "autoplay" -> videoAutoplay = Boolean.parseBoolean(val);
                        case "loop" -> videoLoop = Boolean.parseBoolean(val);
                        default -> applyDirective(slide, key, val);
                    }
                    continue;
                } else {
                    inHeader = false;
                }
            }

            // Check for speaker notes: lines starting with 'Note:' or '???'
            if (lineTrim.startsWith("Note:") || lineTrim.startsWith("note:")) {
                slide.addNote(lineTrim.substring(5).trim());
            } else if (lineTrim.equals("???")) {
                // marp style note separator
            } else {
                markdownBody.append(line).append("\n");
            }
        }

        // Configure VideoSpec if directives were found
        if (videoSource != null || videoJoin != null || slide.getVideoSpec() != null) {
            List<String> joinList = new ArrayList<>();
            if (videoJoin != null) {
                String[] parts = videoJoin.split("[,;]");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) {
                        joinList.add(p.trim());
                    }
                }
            }

            String cutStart = null;
            String cutEnd = null;
            if (videoCut != null) {
                String[] cutParts = videoCut.split("[-–—]");
                if (cutParts.length >= 1) cutStart = cutParts[0].trim();
                if (cutParts.length >= 2) cutEnd = cutParts[1].trim();
            }

            VideoSpec spec = new VideoSpec(videoSource, cutStart, cutEnd, joinList, videoAutoplay, videoLoop);
            slide.setVideoSpec(spec);
        }

        // 3. Extract title from Markdown
        Node document = commonmarkParser.parse(markdownBody.toString());
        String detectedTitle = findHeadingText(document);
        if (detectedTitle != null && !detectedTitle.isBlank()) {
            slide.setTitle(detectedTitle);
        } else {
            slide.setTitle("Slide " + (index + 1));
        }

        return slide;
    }

    private boolean isKnownDirectiveKey(String key) {
        String k = key.toLowerCase();
        return switch (k) {
            case "bg", "background", "background-color", "background-image",
                 "align", "alignment", "layout", "transition",
                 "video", "cut", "video-cut", "join", "video-join",
                 "autoplay", "loop" -> true;
            default -> false;
        };
    }

    private void applyDirective(Slide slide, String key, String value) {
        switch (key.toLowerCase()) {
            case "bg", "background", "background-color" -> {
                if (value.startsWith("url(") || value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".webp")) {
                    slide.setBackgroundImage(cleanUrl(value));
                } else {
                    slide.setBackgroundColor(value);
                }
            }
            case "background-image" -> slide.setBackgroundImage(cleanUrl(value));
            case "align", "alignment" -> slide.setAlignment(Alignment.fromString(value));
            case "transition" -> slide.setTransition(value.trim().toLowerCase());
        }
    }

    private String cleanUrl(String val) {
        String res = val.trim();
        if (res.startsWith("url(") && res.endsWith(")")) {
            res = res.substring(4, res.length() - 1).trim();
            if ((res.startsWith("\"") && res.endsWith("\"")) || (res.startsWith("'") && res.endsWith("'"))) {
                res = res.substring(1, res.length() - 1);
            }
        }
        return res;
    }

    private String findHeadingText(Node root) {
        HeadingVisitor visitor = new HeadingVisitor();
        root.accept(visitor);
        return visitor.firstHeadingText;
    }

    public Node parseMarkdownAst(String markdown) {
        return commonmarkParser.parse(markdown);
    }

    private static class HeadingVisitor extends AbstractVisitor {
        String firstHeadingText = null;

        @Override
        public void visit(Heading heading) {
            if (firstHeadingText == null) {
                StringBuilder sb = new StringBuilder();
                Node child = heading.getFirstChild();
                while (child != null) {
                    if (child instanceof Text t) {
                        sb.append(t.getLiteral());
                    } else if (child instanceof Code c) {
                        sb.append(c.getLiteral());
                    }
                    child = child.getNext();
                }
                firstHeadingText = sb.toString().trim();
            }
            super.visit(heading);
        }
    }
}
