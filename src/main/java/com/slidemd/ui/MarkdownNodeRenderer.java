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

    public MarkdownNodeRenderer(File baseDir, Alignment alignment) {
        this.baseDir = baseDir;
        this.alignment = alignment != null ? alignment : Alignment.CENTER;
    }

    public List<Node> renderNodes(org.commonmark.node.Node root) {
        List<Node> fxNodes = new ArrayList<>();
        org.commonmark.node.Node current = root.getFirstChild();

        while (current != null) {
            Node fxNode = renderBlock(current);
            if (fxNode != null) {
                fxNodes.add(fxNode);
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

        int level = heading.getLevel();
        double fontSize = switch (level) {
            case 1 -> 48.0;
            case 2 -> 34.0;
            case 3 -> 26.0;
            default -> 22.0;
        };

        String color = switch (level) {
            case 1 -> "#ffffff";
            case 2 -> "#94a3b8";
            case 3 -> "#38bdf8";
            default -> "#cbd5e1";
        };

        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        label.setStyle("-fx-text-fill: " + color + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 2);");

        StringBuilder sb = new StringBuilder();
        extractText(heading, sb);
        label.setText(sb.toString().trim());

        VBox.setMargin(label, new Insets(0, 0, level == 1 ? 16 : 10, 0));
        return label;
    }

    private Node renderParagraph(Paragraph paragraph) {
        // Check if paragraph contains only an image
        if (paragraph.getFirstChild() instanceof org.commonmark.node.Image imgNode && paragraph.getFirstChild().getNext() == null) {
            return renderImage(imgNode);
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

        org.commonmark.node.Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem item) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.TOP_LEFT);

                Label bullet = new Label("•");
                bullet.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
                bullet.setStyle("-fx-text-fill: #38bdf8;");

                TextFlow flow = new TextFlow();
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

                row.getChildren().addAll(bullet, flow);
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

        int num = list.getStartNumber();
        org.commonmark.node.Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem item) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.TOP_LEFT);

                Label numLabel = new Label(num + ".");
                numLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
                numLabel.setStyle("-fx-text-fill: #38bdf8;");
                num++;

                TextFlow flow = new TextFlow();
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

                row.getChildren().addAll(numLabel, flow);
                vbox.getChildren().add(row);
            }
            child = child.getNext();
        }

        VBox.setMargin(vbox, new Insets(8, 0, 14, 0));
        return vbox;
    }

    private Node renderBlockQuote(BlockQuote blockQuote) {
        HBox box = new HBox(14);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 8px; -fx-padding: 12px 18px;");

        Region bar = new Region();
        bar.setPrefWidth(4);
        bar.setStyle("-fx-background-color: #38bdf8; -fx-background-radius: 4px;");

        TextFlow flow = new TextFlow();
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

        box.getChildren().addAll(bar, flow);
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
            Text fxText = new Text(text.getLiteral());
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
            case CENTER -> Pos.CENTER;
        };
    }

    private javafx.scene.text.TextAlignment toTextAlignment(Alignment align) {
        return switch (align) {
            case LEFT -> javafx.scene.text.TextAlignment.LEFT;
            case RIGHT -> javafx.scene.text.TextAlignment.RIGHT;
            case CENTER -> javafx.scene.text.TextAlignment.CENTER;
        };
    }
}
