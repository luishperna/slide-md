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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownSlideParser {

    private static final Pattern SLIDE_SEPARATOR = Pattern.compile("(?m)^\\s*---\\s*$");
    private static final Pattern HTML_COMMENT_DIRECTIVE = Pattern.compile("<!--\\s*([a-zA-Z0-9_-]+)\\s*:\\s*(.*?)\\s*-->");
    private static final Pattern FRONTMATTER_LINE = Pattern.compile("^([a-zA-Z0-9_-]+)\\s*:\\s*(.*)$");
    private static final Pattern SLIDE_OVERRIDE_LINE = Pattern.compile("^(?:slide\\s+(\\d+)|\\[slide\\s+(\\d+)\\])\\s*:?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

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

        Map<String, String> globalDirectives = new HashMap<>();
        Map<Integer, Map<String, String>> slideOverrides = new HashMap<>();
        String remainingContent = content;

        // Check if content begins with frontmatter (global presentation configuration)
        // Format: ^\s*---\r?\n ... \r?\n---\r?\n?
        Matcher fmM = Pattern.compile("^\\s*---\\r?\\n(.*?)\\r?\\n---\\r?\\n?", Pattern.DOTALL).matcher(content);
        if (fmM.find()) {
            String frontmatterText = fmM.group(1);
            remainingContent = content.substring(fmM.end());
            parseGlobalFrontmatter(frontmatterText, globalDirectives, slideOverrides);

            // Apply global presentation-level sound directives
            for (Map.Entry<String, String> entry : globalDirectives.entrySet()) {
                String k = entry.getKey().toLowerCase();
                String v = entry.getValue();
                if ("sound".equals(k)) {
                    deck.setSound(cleanUrl(v));
                    deck.setSoundLoop(false);
                    deck.setSoundVolume(1.0);
                } else if ("color".equals(k)) {
                    deck.setColor(v.trim());
                }
            }
        }

        // Split slides in remaining content by '---'
        String[] rawSlides = SLIDE_SEPARATOR.split(remainingContent);
        int slideNumber = 1;

        for (String raw : rawSlides) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Slide slide = parseSlideWithContext(slideNumber - 1, trimmed, globalDirectives, slideOverrides.get(slideNumber));
            deck.addSlide(slide);

            if (slideNumber == 1 && slide.getTitle() != null && !slide.getTitle().isBlank()) {
                deck.setTitle(slide.getTitle());
            }
            slideNumber++;
        }

        if (deck.getSlideCount() == 0) {
            Slide emptySlide = new Slide(0, "");
            emptySlide.setTitle("Novo Slide");
            deck.addSlide(emptySlide);
        }

        return deck;
    }

    private void parseGlobalFrontmatter(String text, Map<String, String> globalDirectives, Map<Integer, Map<String, String>> slideOverrides) {
        String[] lines = text.split("\\r?\\n");
        Integer currentOverrideSlide = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher slideMatch = SLIDE_OVERRIDE_LINE.matcher(trimmed);
            if (slideMatch.matches()) {
                String numStr = slideMatch.group(1) != null ? slideMatch.group(1) : slideMatch.group(2);
                currentOverrideSlide = Integer.parseInt(numStr);
                slideOverrides.putIfAbsent(currentOverrideSlide, new HashMap<>());

                String inlineDirective = slideMatch.group(3);
                if (inlineDirective != null && !inlineDirective.isBlank()) {
                    parseDirectivesLine(inlineDirective, slideOverrides.get(currentOverrideSlide));
                }
                continue;
            }

            Matcher fmM = FRONTMATTER_LINE.matcher(trimmed);
            if (fmM.matches()) {
                String key = fmM.group(1).toLowerCase();
                String val = fmM.group(2).trim();

                if (!isKnownDirectiveKey(key)) {
                    continue;
                }

                if (line.startsWith(" ") || line.startsWith("\t")) {
                    if (currentOverrideSlide != null) {
                        slideOverrides.get(currentOverrideSlide).put(key, val);
                        continue;
                    }
                }

                currentOverrideSlide = null;
                globalDirectives.put(key, val);
            }
        }
    }

    private void parseDirectivesLine(String line, Map<String, String> targetMap) {
        String[] parts = line.split("[,;]");
        for (String part : parts) {
            Matcher m = FRONTMATTER_LINE.matcher(part.trim());
            if (m.matches() && isKnownDirectiveKey(m.group(1))) {
                targetMap.put(m.group(1).toLowerCase(), m.group(2).trim());
            }
        }
    }

    public Slide parseSlide(int index, String rawContent) {
        return parseSlideWithContext(index, rawContent, Collections.emptyMap(), Collections.emptyMap());
    }

    public Slide parseSlideWithContext(int index, String rawContent, Map<String, String> globalDirectives, Map<String, String> specificOverrides) {
        Slide slide = new Slide(index, rawContent);

        // 1. Apply global defaults
        if (globalDirectives != null) {
            globalDirectives.forEach((k, v) -> applyDirective(slide, k, v));
        }

        // 2. Apply per-slide overrides from global frontmatter (if specified)
        if (specificOverrides != null) {
            specificOverrides.forEach((k, v) -> applyDirective(slide, k, v));
        }

        String workingContent = rawContent;

        // 3. Parse HTML comment directives: <!-- background: #ff0000 -->
        Matcher commentMatcher = HTML_COMMENT_DIRECTIVE.matcher(workingContent);
        while (commentMatcher.find()) {
            String key = commentMatcher.group(1).toLowerCase();
            String value = commentMatcher.group(2).trim();
            if (isKnownDirectiveKey(key)) {
                applyDirective(slide, key, value);
            }
        }
        // Remove todos os comentários HTML do corpo visual do slide
        workingContent = Pattern.compile("<!--[\\s\\S]*?-->").matcher(workingContent).replaceAll("");

        // 4. Parse in-slide directives at the beginning of the slide
        String[] lines = workingContent.split("\\r?\\n");
        StringBuilder markdownBody = new StringBuilder();
        boolean inHeader = true;

        String rawVideo = null;

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
                    if ("video".equals(key)) {
                        rawVideo = val;
                    } else {
                        applyDirective(slide, key, val);
                    }
                    continue;
                } else {
                    inHeader = false;
                }
            }

            // Check for speaker notes
            if (lineTrim.startsWith("Note:") || lineTrim.startsWith("note:")) {
                slide.addNote(lineTrim.substring(5).trim());
            } else if (lineTrim.equals("???")) {
                // marp style note separator
            } else if (lineTrim.startsWith("image:") || lineTrim.startsWith("Image:")) {
                String imgPath = cleanUrl(lineTrim.substring(6).trim());
                if (!imgPath.isEmpty()) {
                    slide.setImage(imgPath);
                    if (markdownBody.length() > 0 && !markdownBody.toString().endsWith("\n\n")) {
                        if (!markdownBody.toString().endsWith("\n")) {
                            markdownBody.append("\n");
                        }
                        markdownBody.append("\n");
                    }
                    markdownBody.append("![](").append(imgPath).append(")\n\n");
                }
            } else if (lineTrim.startsWith("video:") || lineTrim.startsWith("Video:")) {
                String vVal = lineTrim.substring(6).trim();
                if (!vVal.isEmpty()) {
                    rawVideo = vVal;
                    if (markdownBody.length() > 0 && !markdownBody.toString().endsWith("\n\n")) {
                        if (!markdownBody.toString().endsWith("\n")) {
                            markdownBody.append("\n");
                        }
                        markdownBody.append("\n");
                    }
                    markdownBody.append("@[video]\n\n");
                }
            } else {
                markdownBody.append(line).append("\n");
            }
        }

        // Apply video directive if specified
        if (rawVideo != null) {
            slide.setVideoSpec(parseVideoSpec(rawVideo, false, false));
        }

        // Clean markdown content (without directives)
        String cleanMarkdown = markdownBody.toString().trim();
        slide.setContentMarkdown(cleanMarkdown);

        // 5. Extract title from Markdown AST
        Node document = commonmarkParser.parse(cleanMarkdown);
        String detectedTitle = findHeadingText(document);
        if (detectedTitle != null && !detectedTitle.isBlank()) {
            slide.setTitle(detectedTitle);
        } else {
            slide.setTitle("Slide " + (index + 1));
        }

        return slide;
    }

    private boolean isKnownDirectiveKey(String key) {
        if (key == null) return false;
        String k = key.toLowerCase();
        return switch (k) {
            case "background", "alignment", "transition", "video", "sound", "image", "color" -> true;
            default -> false;
        };
    }

    private void applyDirective(Slide slide, String key, String value) {
        switch (key.toLowerCase()) {
            case "background" -> {
                String trimmed = value.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    slide.setBackgroundColor(parseGradientBracketNotation(trimmed));
                } else if (trimmed.startsWith("url(") || trimmed.endsWith(".png") || trimmed.endsWith(".jpg") || trimmed.endsWith(".jpeg") || trimmed.endsWith(".webp")) {
                    slide.setBackgroundImage(cleanUrl(trimmed));
                } else {
                    slide.setBackgroundColor(trimmed);
                }
            }
            case "alignment" -> slide.setAlignment(Alignment.fromString(value));
            case "transition" -> slide.setTransition(value.trim().toLowerCase());
            case "video" -> slide.setVideoSpec(parseVideoSpec(value, false, false));
            case "image" -> slide.setImage(cleanUrl(value));
            case "color" -> slide.setColor(value.trim());
            case "sound" -> {
                slide.setSound(cleanUrl(value));
                slide.setSoundLoop(false);
                slide.setSoundVolume(1.0);
            }
        }
    }

    public static VideoSpec parseVideoSpec(String rawDirective, boolean autoplay, boolean loop) {
        if (rawDirective == null || rawDirective.isBlank()) {
            return null;
        }
        // Divide por vírgulas que não estão dentro de colchetes
        String[] parts = rawDirective.split(",(?![^\\[]*\\])");
        List<VideoItem> items = new ArrayList<>();
        Pattern bracketPattern = Pattern.compile("\\[(.*?)\\]");

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            Matcher bm = bracketPattern.matcher(trimmed);
            String cutStart = null;
            String cutEnd = null;
            String path = trimmed;

            if (bm.find()) {
                String cutRange = bm.group(1).trim();
                String[] cutParts = cutRange.split("[-–—]");
                if (cutParts.length >= 1 && !cutParts[0].trim().isEmpty()) {
                    cutStart = cutParts[0].trim();
                }
                if (cutParts.length >= 2 && !cutParts[1].trim().isEmpty()) {
                    cutEnd = cutParts[1].trim();
                }
                path = bm.replaceAll("").trim();
            }

            if ((path.startsWith("\"") && path.endsWith("\"")) || (path.startsWith("'") && path.endsWith("'"))) {
                path = path.substring(1, path.length() - 1).trim();
            }

            if (!path.isEmpty()) {
                items.add(new VideoItem(path, cutStart, cutEnd));
            }
        }

        if (items.isEmpty()) {
            return null;
        }

        VideoItem first = items.get(0);
        List<String> joinPaths = items.size() > 1
                ? items.stream().map(VideoItem::path).toList()
                : Collections.emptyList();

        return new VideoSpec(first.path(), first.cutStart(), first.cutEnd(), joinPaths, autoplay, loop);
    }

    private record VideoItem(String path, String cutStart, String cutEnd) {}

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

    public static String parseGradientBracketNotation(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return trimmed;
        }

        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return "";

        String[] parts = inner.split(",");
        List<String> colors = new ArrayList<>();
        String directionParam = null;

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.isEmpty()) continue;

            if (i == parts.length - 1 && isDirectionToken(p)) {
                directionParam = p;
            } else {
                colors.add(p);
            }
        }

        if (colors.isEmpty()) {
            if (directionParam != null) {
                return directionParam;
            }
            return "";
        }

        if (colors.size() == 1 && directionParam == null) {
            return colors.get(0);
        }

        String cssDirection = resolveCssDirection(directionParam);
        return "linear-gradient(" + cssDirection + ", " + String.join(", ", colors) + ")";
    }

    private static boolean isDirectionToken(String token) {
        String lower = token.toLowerCase();
        if (lower.endsWith("%") || lower.endsWith("deg")) return true;
        if (lower.startsWith("to ")) return true;
        return switch (lower) {
            case "right", "left", "bottom", "top", "horizontal", "vertical", "diagonal", "down", "up" -> true;
            default -> false;
        };
    }

    private static String resolveCssDirection(String dir) {
        if (dir == null || dir.isBlank()) {
            return "to bottom right";
        }
        String lower = dir.toLowerCase().trim();

        if (lower.startsWith("to ")) {
            return lower;
        }

        if (lower.endsWith("%")) {
            try {
                double pct = Double.parseDouble(lower.replace("%", "").trim());
                if (pct <= 20.0) return "to right";
                if (pct <= 45.0) return "to bottom right";
                if (pct <= 70.0) return "to bottom";
                if (pct <= 90.0) return "to bottom left";
                return "to left";
            } catch (NumberFormatException ignored) {}
        }

        if (lower.endsWith("deg")) {
            try {
                double deg = Double.parseDouble(lower.replace("deg", "").trim()) % 360;
                if (deg < 0) deg += 360;
                if (deg >= 337.5 || deg < 22.5) return "to right";
                if (deg < 67.5) return "to bottom right";
                if (deg < 112.5) return "to bottom";
                if (deg < 157.5) return "to bottom left";
                if (deg < 202.5) return "to left";
                if (deg < 247.5) return "to top left";
                if (deg < 292.5) return "to top";
                return "to top right";
            } catch (NumberFormatException ignored) {}
        }

        return switch (lower) {
            case "right", "horizontal" -> "to right";
            case "bottom", "vertical", "down" -> "to bottom";
            case "left" -> "to left";
            case "top", "up" -> "to top";
            case "diagonal" -> "to bottom right";
            default -> "to bottom right";
        };
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
                    if (child instanceof org.commonmark.node.Text t) {
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
