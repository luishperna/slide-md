package com.slidemd.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingEditorWindow {

    private final Stage editorStage;
    private final CodeArea codeArea;
    private final Label statusLabel;
    private final Consumer<String> onContentChanged;
    private final Runnable onSave;
    private final PauseTransition debounceTimer;
    private final PauseTransition highlightTimer;

    private boolean isProgrammaticChange = false;
    private boolean stagePositioned = false;

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

    public FloatingEditorWindow(Stage ownerStage, Consumer<String> onContentChanged, Runnable onSave) {
        this.onContentChanged = onContentChanged;
        this.onSave = onSave;
        this.editorStage = new Stage();

        if (ownerStage != null) {
            editorStage.initOwner(ownerStage);
        }

        editorStage.setTitle("slide-md — Editor Markdown");
        editorStage.setMinWidth(480);
        editorStage.setMinHeight(380);

        // CodeArea setup with line numbers
        this.codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        // Debounce timer for real-time live preview update
        debounceTimer = new PauseTransition(Duration.millis(250));
        debounceTimer.setOnFinished(e -> {
            if (!isProgrammaticChange && this.onContentChanged != null) {
                this.onContentChanged.accept(codeArea.getText());
            }
        });

        // Fast syntax highlight timer
        highlightTimer = new PauseTransition(Duration.millis(40));
        highlightTimer.setOnFinished(e -> {
            Platform.runLater(() -> {
                try {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                } catch (Exception ignored) {}
            });
        });

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            updateStats();
            if (!isProgrammaticChange) {
                debounceTimer.playFromStart();
                highlightTimer.playFromStart();
            }
        });

        // 1. Top Toolbar (#161616)
        HBox topToolbar = new HBox(10);
        topToolbar.setAlignment(Pos.CENTER_LEFT);
        topToolbar.setPadding(new Insets(8, 14, 8, 14));
        topToolbar.setStyle("-fx-background-color: #161616;" +
                            "-fx-border-color: transparent transparent rgba(255, 255, 255, 0.08) transparent;" +
                            "-fx-border-width: 1px;");

        Label title = new Label("⚡ Editor Markdown");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#38bdf8"));

        Label liveBadge = new Label("● Ao Vivo");
        liveBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        liveBadge.setTextFill(Color.web("#34d399"));
        liveBadge.setStyle("-fx-background-color: rgba(52, 211, 153, 0.15); -fx-background-radius: 8px; -fx-padding: 2 8 2 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newSlideBtn = new Button("+ Novo Slide");
        newSlideBtn.setStyle("-fx-background-color: #262626; -fx-text-fill: #e2e8f0; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 10 4 10; -fx-font-size: 11.5px;");
        newSlideBtn.setOnMouseEntered(e -> newSlideBtn.setStyle("-fx-background-color: #38bdf8; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 10 4 10; -fx-font-size: 11.5px;"));
        newSlideBtn.setOnMouseExited(e -> newSlideBtn.setStyle("-fx-background-color: #262626; -fx-text-fill: #e2e8f0; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 10 4 10; -fx-font-size: 11.5px;"));
        newSlideBtn.setOnAction(e -> {
            insertTextAtCaret("\n\n---\n\n# Novo Título\n");
            codeArea.requestFocus();
        });
        Tooltip.install(newSlideBtn, new Tooltip("Inserir divisor de slide (---)"));

        Button saveBtn = new Button("💾 Salvar");
        saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold;"));
        saveBtn.setOnAction(e -> {
            if (this.onSave != null) this.onSave.run();
        });
        Tooltip.install(saveBtn, new Tooltip("Salvar apresentação (Ctrl+S)"));

        topToolbar.getChildren().addAll(title, liveBadge, spacer, newSlideBtn, saveBtn);

        // 2. Editor in ScrollPane (#1c1c1c)
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle("-fx-background-color: #1c1c1c;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 3. Footer / Status bar (#161616)
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(5, 14, 6, 14));
        footer.setStyle("-fx-background-color: #161616;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.06) transparent transparent transparent;" +
                        "-fx-border-width: 1px;");

        statusLabel = new Label("0 slides detectados • Atualização em tempo real");
        statusLabel.setFont(Font.font("Segoe UI", 11.5));
        statusLabel.setTextFill(Color.web("#94a3b8"));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label hintsLabel = new Label("Ctrl+S: Salvar  •  Esc: Fechar Janela");
        hintsLabel.setFont(Font.font("Segoe UI", 11));
        hintsLabel.setTextFill(Color.web("#64748b"));

        footer.getChildren().addAll(statusLabel, footerSpacer, hintsLabel);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1c1c1c;");
        root.setTop(topToolbar);
        root.setCenter(scrollPane);
        root.setBottom(footer);

        Scene scene = new Scene(root, 720, 600);

        // Load custom Markdown CSS
        try {
            var cssUrl = getClass().getResource("/css/markdown-editor.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}

        // Handle Escape to hide, Ctrl+S to save
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideWindow();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
                if (this.onSave != null) this.onSave.run();
                event.consume();
            }
        });

        editorStage.setScene(scene);

        // Hide on window close request instead of terminating
        editorStage.setOnCloseRequest(e -> {
            e.consume();
            hideWindow();
        });
    }

    private void insertTextAtCaret(String textToInsert) {
        int caret = codeArea.getCaretPosition();
        codeArea.insertText(caret, textToInsert);
    }

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

    public void setMarkdownContent(String content) {
        isProgrammaticChange = true;
        codeArea.replaceText(content != null ? content : "");
        isProgrammaticChange = false;
        try {
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
        } catch (Exception ignored) {}
        updateStats();
    }

    public String getMarkdownContent() {
        return codeArea.getText();
    }

    private void updateStats() {
        String text = codeArea.getText();
        if (text.isBlank()) {
            statusLabel.setText("0 slides detectados • Atualização em tempo real");
            return;
        }
        int slideCount = text.split("(?m)^\\s*---\\s*$").length;
        statusLabel.setText(slideCount + " slide" + (slideCount > 1 ? "s" : "") + " detectados • Atualização em tempo real");
    }

    public void showWindow() {
        if (!editorStage.isShowing()) {
            if (!stagePositioned && editorStage.getOwner() instanceof Stage owner && owner.isShowing()) {
                editorStage.setX(owner.getX() + 60);
                editorStage.setY(owner.getY() + 40);
                stagePositioned = true;
            }
            editorStage.show();
        }
        editorStage.toFront();
        codeArea.requestFocus();
    }

    public void hideWindow() {
        editorStage.hide();
    }

    public void toggleWindow() {
        if (editorStage.isShowing()) {
            hideWindow();
        } else {
            showWindow();
        }
    }

    public boolean isVisible() {
        return editorStage.isShowing();
    }

    public Stage getStage() {
        return editorStage;
    }
}
