package com.slidemd.ui;

import com.slidemd.model.Alignment;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.commonmark.node.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MarkdownNodeRenderer {

    private final File baseDir;
    private final Alignment alignment;
    private final Color primaryColor;
    private final boolean isWhiteDefault;

    public MarkdownNodeRenderer(File baseDir, Alignment alignment) {
        this(baseDir, alignment, null);
    }

    public MarkdownNodeRenderer(File baseDir, Alignment alignment, String colorDirective) {
        this.baseDir = baseDir;
        this.alignment = alignment != null ? alignment : Alignment.CENTER;

        Color parsed = null;
        if (colorDirective != null && !colorDirective.isBlank()) {
            try {
                parsed = Color.web(colorDirective.trim());
            } catch (Exception ignored) {}
        }

        if (parsed != null) {
            this.primaryColor = parsed;
            this.isWhiteDefault = (parsed.getRed() > 0.95 && parsed.getGreen() > 0.95 && parsed.getBlue() > 0.95);
        } else {
            this.primaryColor = Color.WHITE;
            this.isWhiteDefault = true;
        }
    }

    public List<Node> renderNodes(org.commonmark.node.Node root) {
        List<Node> fxNodes = new ArrayList<>();
        org.commonmark.node.Node current = root.getFirstChild();

        while (current != null) {
            if (current instanceof Paragraph paragraph && containsVideoPlaceholder(paragraph)) {
                renderParagraphWithVideo(paragraph, fxNodes);
            } else {
                Node fxNode = renderBlock(current);
                if (fxNode != null) {
                    fxNodes.add(fxNode);
                }
            }
            current = current.getNext();
        }

        return fxNodes;
    }

    private Node renderBlock(org.commonmark.node.Node block) {
        if (block instanceof Heading heading) {
            return renderHeading(heading);
        } else if (block instanceof Paragraph paragraph) {
            return renderParagraph(paragraph);
        } else if (block instanceof BulletList bulletList) {
            return renderBulletList(bulletList);
        } else if (block instanceof OrderedList orderedList) {
            return renderOrderedList(orderedList);
        } else if (block instanceof BlockQuote blockQuote) {
            return renderBlockQuote(blockQuote);
        } else if (block instanceof FencedCodeBlock fencedCodeBlock) {
            return renderCodeBlock(fencedCodeBlock.getLiteral());
        } else if (block instanceof IndentedCodeBlock indentedCodeBlock) {
            return renderCodeBlock(indentedCodeBlock.getLiteral());
        } else if (block instanceof ThematicBreak) {
            Region hr = new Region();
            hr.setPrefHeight(2);
            hr.setMaxWidth(200);
            hr.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 2px;");
            return hr;
        }
        return null;
    }

    private Node renderHeading(Heading heading) {
        Label label = new Label();
        label.setWrapText(true);
        label.setAlignment(toPos(alignment));
        label.setTextAlignment(toTextAlignment(alignment));
        label.setMaxWidth(Double.MAX_VALUE);

        int level = heading.getLevel();
        double fontSize = switch (level) {
            case 1 -> 48.0;
            case 2 -> 34.0;
            case 3 -> 26.0;
            case 4 -> 22.0;
            case 5 -> 18.0;
            default -> 16.0;
        };

        String color = getHeadingColor(level);

        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        label.setStyle("-fx-text-fill: " + color + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 2);");

        StringBuilder sb = new StringBuilder();
        extractText(heading, sb);
        label.setText(sb.toString().trim());

        VBox.setMargin(label, new Insets(0, 0, level == 1 ? 16 : 10, 0));
        return label;
    }

    private String getHeadingColor(int level) {
        if (isWhiteDefault) {
            return switch (level) {
                case 1 -> "#ffffff";
                case 2 -> "#e2e8f0";
                case 3 -> "#cbd5e1";
                case 4 -> "#94a3b8";
                case 5 -> "#64748b";
                default -> "#475569";
            };
        }

        // Se tiver cor principal configurada (ex: vermelho), vai clareando conforme a quantidade de #
        double lightenFraction = switch (level) {
            case 1 -> 0.0;
            case 2 -> 0.20;
            case 3 -> 0.40;
            case 4 -> 0.60;
            case 5 -> 0.75;
            default -> 0.85;
        };

        Color interpolated = primaryColor.interpolate(Color.WHITE, lightenFraction);
        return toHexString(interpolated);
    }

    private String toHexString(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static class VideoPlaceholder extends Region {
    }

    private Node renderParagraph(Paragraph paragraph) {
        // Check if paragraph contains only an image
        if (paragraph.getFirstChild() instanceof org.commonmark.node.Image imgNode && paragraph.getFirstChild().getNext() == null) {
            return renderImage(imgNode);
        }

        // Check if paragraph contains video placeholder marker
        if (paragraph.getFirstChild() instanceof org.commonmark.node.Text textNode && paragraph.getFirstChild().getNext() == null) {
            if ("@[video]".equals(textNode.getLiteral().trim())) {
                return new VideoPlaceholder();
            }
        }

        TextFlow flow = new TextFlow();
        flow.setTextAlignment(toTextAlignment(alignment));

        org.commonmark.node.Node child = paragraph.getFirstChild();
        while (child != null) {
            renderInline(child, flow, false, false);
            child = child.getNext();
        }

        VBox.setMargin(flow, new Insets(4, 0, 8, 0));
        return flow;
    }

    private boolean containsVideoPlaceholder(Paragraph paragraph) {
        org.commonmark.node.Node child = paragraph.getFirstChild();
        while (child != null) {
            if (child instanceof org.commonmark.node.Text text && text.getLiteral().contains("@[video]")) {
                return true;
            }
            child = child.getNext();
        }
        return false;
    }

    private void renderParagraphWithVideo(Paragraph paragraph, List<Node> outList) {
        TextFlow beforeFlow = new TextFlow();
        beforeFlow.setTextAlignment(toTextAlignment(alignment));

        TextFlow afterFlow = new TextFlow();
        afterFlow.setTextAlignment(toTextAlignment(alignment));

        boolean foundVideo = false;
        org.commonmark.node.Node child = paragraph.getFirstChild();
        while (child != null) {
            if (child instanceof org.commonmark.node.Text text && text.getLiteral().contains("@[video]")) {
                foundVideo = true;
                String remaining = text.getLiteral().replace("@[video]", "").trim();
                if (!remaining.isEmpty()) {
                    Text fxText = new Text(remaining);
                    fxText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 22));
                    fxText.setFill(Color.web("#e2e8f0"));
                    afterFlow.getChildren().add(fxText);
                }
            } else {
                if (!foundVideo) {
                    renderInline(child, beforeFlow, false, false);
                } else {
                    renderInline(child, afterFlow, false, false);
                }
            }
            child = child.getNext();
        }

        if (!beforeFlow.getChildren().isEmpty()) {
            VBox.setMargin(beforeFlow, new Insets(4, 0, 8, 0));
            outList.add(beforeFlow);
        }

        outList.add(new VideoPlaceholder());

        if (!afterFlow.getChildren().isEmpty()) {
            VBox.setMargin(afterFlow, new Insets(4, 0, 8, 0));
            outList.add(afterFlow);
        }
    }

    private Node renderImage(org.commonmark.node.Image imgNode) {
        String dest = imgNode.getDestination();
        try {
            File imgFile = new File(dest);
            if (!imgFile.isAbsolute() && baseDir != null) {
                imgFile = new File(baseDir, dest);
            }

            Image fxImg;
            if (imgFile.exists()) {
                fxImg = new Image(imgFile.toURI().toString());
            } else if (dest.startsWith("http://") || dest.startsWith("https://")) {
                fxImg = new Image(dest, true);
            } else {
                return new Label("[Imagem não encontrada: " + dest + "]");
            }

            ImageView iv = new ImageView(fxImg);
            iv.setPreserveRatio(true);
            iv.setFitWidth(650);
            iv.setFitHeight(400);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 6);");
            return iv;
        } catch (Exception e) {
            return new Label("[Erro ao carregar imagem: " + dest + "]");
        }
    }

    private Node renderBulletList(BulletList list) {
        VBox vbox = new VBox(10);
        vbox.setAlignment(toPos(alignment));

        Pos rowPos = switch (alignment) {
            case RIGHT -> Pos.TOP_RIGHT;
            case CENTER -> Pos.CENTER;
            default -> Pos.TOP_LEFT;
        };

        org.commonmark.node.Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem item) {
                HBox row = new HBox(12);
                row.setAlignment(rowPos);

                Label bullet = new Label("•");
                bullet.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
                bullet.setStyle("-fx-text-fill: " + toHexString(primaryColor) + ";");

                TextFlow flow = new TextFlow();
                flow.setTextAlignment(toTextAlignment(alignment));
                org.commonmark.node.Node itemChild = item.getFirstChild();
                while (itemChild != null) {
                    if (itemChild instanceof Paragraph p) {
                        org.commonmark.node.Node in = p.getFirstChild();
                        while (in != null) {
                            renderInline(in, flow, false, false);
                            in = in.getNext();
                        }
                    } else {
                        renderInline(itemChild, flow, false, false);
                    }
                    itemChild = itemChild.getNext();
                }

                if (alignment == Alignment.RIGHT) {
                    row.getChildren().addAll(flow, bullet);
                } else {
                    row.getChildren().addAll(bullet, flow);
                }
                vbox.getChildren().add(row);
            }
            child = child.getNext();
        }

        VBox.setMargin(vbox, new Insets(8, 0, 14, 0));
        return vbox;
    }

    private Node renderOrderedList(OrderedList list) {
        VBox vbox = new VBox(10);
        vbox.setAlignment(toPos(alignment));

        Pos rowPos = switch (alignment) {
            case RIGHT -> Pos.TOP_RIGHT;
            case CENTER -> Pos.CENTER;
            default -> Pos.TOP_LEFT;
        };

        int num = list.getStartNumber();
        org.commonmark.node.Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem item) {
                HBox row = new HBox(10);
                row.setAlignment(rowPos);

                Label numLabel = new Label(num + ".");
                numLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
                numLabel.setStyle("-fx-text-fill: " + toHexString(primaryColor) + ";");
                num++;

                TextFlow flow = new TextFlow();
                flow.setTextAlignment(toTextAlignment(alignment));
                org.commonmark.node.Node itemChild = item.getFirstChild();
                while (itemChild != null) {
                    if (itemChild instanceof Paragraph p) {
                        org.commonmark.node.Node in = p.getFirstChild();
                        while (in != null) {
                            renderInline(in, flow, false, false);
                            in = in.getNext();
                        }
                    } else {
                        renderInline(itemChild, flow, false, false);
                    }
                    itemChild = itemChild.getNext();
                }

                if (alignment == Alignment.RIGHT) {
                    row.getChildren().addAll(flow, numLabel);
                } else {
                    row.getChildren().addAll(numLabel, flow);
                }
                vbox.getChildren().add(row);
            }
            child = child.getNext();
        }

        VBox.setMargin(vbox, new Insets(8, 0, 14, 0));
        return vbox;
    }

    private Node renderBlockQuote(BlockQuote blockQuote) {
        HBox box = new HBox(14);
        Pos boxPos = switch (alignment) {
            case RIGHT -> Pos.CENTER_RIGHT;
            case CENTER -> Pos.CENTER;
            default -> Pos.CENTER_LEFT;
        };
        box.setAlignment(boxPos);
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 8px; -fx-padding: 12px 18px;");

        Region bar = new Region();
        bar.setPrefWidth(4);
        bar.setStyle("-fx-background-color: " + toHexString(primaryColor) + "; -fx-background-radius: 4px;");

        TextFlow flow = new TextFlow();
        flow.setTextAlignment(toTextAlignment(alignment));
        org.commonmark.node.Node child = blockQuote.getFirstChild();
        while (child != null) {
            if (child instanceof Paragraph p) {
                org.commonmark.node.Node in = p.getFirstChild();
                while (in != null) {
                    renderInline(in, flow, false, true);
                    in = in.getNext();
                }
            }
            child = child.getNext();
        }

        if (alignment == Alignment.RIGHT) {
            box.getChildren().addAll(flow, bar);
        } else {
            box.getChildren().addAll(bar, flow);
        }
        VBox.setMargin(box, new Insets(10, 0, 10, 0));
        return box;
    }

    private Node renderCodeBlock(String code) {
        VBox box = new VBox();
        box.setStyle("-fx-background-color: #0b0f19; -fx-background-radius: 10px; -fx-padding: 16px; -fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 10px;");
        box.setMaxWidth(800);

        Label label = new Label(code.trim());
        label.setFont(Font.font("Consolas", 18));
        label.setStyle("-fx-text-fill: #38bdf8;");
        label.setWrapText(true);

        box.getChildren().add(label);
        VBox.setMargin(box, new Insets(12, 0, 12, 0));
        return box;
    }

    private void renderInline(org.commonmark.node.Node node, TextFlow flow, boolean bold, boolean italic) {
        if (node instanceof org.commonmark.node.Text text) {
            String literal = text.getLiteral();
            if (literal.contains("@[video]")) {
                literal = literal.replace("@[video]", "").trim();
                if (literal.isEmpty()) return;
            }
            Text fxText = new Text(literal);
            fxText.setFont(Font.font("Segoe UI",
                    bold ? FontWeight.BOLD : FontWeight.NORMAL,
                    italic ? FontPosture.ITALIC : FontPosture.REGULAR,
                    22));
            fxText.setFill(Color.web("#e2e8f0"));
            flow.getChildren().add(fxText);
        } else if (node instanceof StrongEmphasis strong) {
            org.commonmark.node.Node child = strong.getFirstChild();
            while (child != null) {
                renderInline(child, flow, true, italic);
                child = child.getNext();
            }
        } else if (node instanceof Emphasis emphasis) {
            org.commonmark.node.Node child = emphasis.getFirstChild();
            while (child != null) {
                renderInline(child, flow, bold, true);
                child = child.getNext();
            }
        } else if (node instanceof Code code) {
            Text fxText = new Text(" " + code.getLiteral() + " ");
            fxText.setFont(Font.font("Consolas", FontWeight.MEDIUM, 20));
            fxText.setFill(Color.web("#38bdf8"));
            flow.getChildren().add(fxText);
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            flow.getChildren().add(new Text("\n"));
        } else if (node instanceof Link link) {
            Text linkText = new Text();
            StringBuilder sb = new StringBuilder();
            extractText(link, sb);
            linkText.setText(sb.toString());
            linkText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
            linkText.setFill(Color.web("#60a5fa"));
            linkText.setUnderline(true);
            flow.getChildren().add(linkText);
        }
    }

    private void extractText(org.commonmark.node.Node node, StringBuilder sb) {
        org.commonmark.node.Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof org.commonmark.node.Text t) {
                sb.append(t.getLiteral());
            } else if (child instanceof Code c) {
                sb.append(c.getLiteral());
            } else {
                extractText(child, sb);
            }
            child = child.getNext();
        }
    }

    private Pos toPos(Alignment align) {
        return switch (align) {
            case LEFT -> Pos.CENTER_LEFT;
            case RIGHT -> Pos.CENTER_RIGHT;
            case TOP -> Pos.TOP_CENTER;
            case DOWN -> Pos.BOTTOM_CENTER;
            case CENTER -> Pos.CENTER;
        };
    }

    private javafx.scene.text.TextAlignment toTextAlignment(Alignment align) {
        return switch (align) {
            case LEFT -> javafx.scene.text.TextAlignment.LEFT;
            case RIGHT -> javafx.scene.text.TextAlignment.RIGHT;
            case CENTER, TOP, DOWN -> javafx.scene.text.TextAlignment.CENTER;
        };
    }
}
