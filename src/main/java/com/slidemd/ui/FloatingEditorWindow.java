package com.slidemd.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.function.Consumer;

public class FloatingEditorWindow extends VBox {

    private final TextArea editorArea;
    private final Label statusLabel;
    private final Consumer<String> onContentChanged;
    private final PauseTransition debounceTimer;

    private double dragStartX;
    private double dragStartY;

    public FloatingEditorWindow(Consumer<String> onContentChanged, Runnable onClose) {
        this.onContentChanged = onContentChanged;
        this.editorArea = new TextArea();

        // Debounce timer for real-time live preview update
        debounceTimer = new PauseTransition(Duration.millis(250));
        debounceTimer.setOnFinished(e -> {
            if (this.onContentChanged != null) {
                this.onContentChanged.accept(editorArea.getText());
            }
        });

        // Setup container styling (Antigravity Dark: #1c1c1c with sleek borders)
        setMaxSize(580, 520);
        setPrefSize(560, 480);
        setStyle("-fx-background-color: #1c1c1c;" +
                 "-fx-background-radius: 12px;" +
                 "-fx-border-color: rgba(255, 255, 255, 0.12);" +
                 "-fx-border-radius: 12px;" +
                 "-fx-border-width: 1px;" +
                 "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.65), 30, 0, 0, 10);");

        // 1. Header (Draggable, #161616)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 16));
        header.setStyle("-fx-background-color: #161616;" +
                        "-fx-background-radius: 12px 12px 0 0;" +
                        "-fx-border-color: transparent transparent rgba(255, 255, 255, 0.08) transparent;" +
                        "-fx-border-width: 1px;");

        Label title = new Label("Editor Markdown");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#e2e8f0"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #e81123; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 2 8 2 8;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 2 8 2 8;"));
        closeBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        header.getChildren().addAll(title, spacer, closeBtn);

        // Make window draggable via header
        header.setOnMousePressed(e -> {
            dragStartX = e.getSceneX() - getTranslateX();
            dragStartY = e.getSceneY() - getTranslateY();
        });
        header.setOnMouseDragged(e -> {
            setTranslateX(e.getSceneX() - dragStartX);
            setTranslateY(e.getSceneY() - dragStartY);
        });

        // 2. Editor TextArea (#1c1c1c background)
        editorArea.setWrapText(true);
        editorArea.setFont(Font.font("Consolas", 13.5));
        editorArea.setStyle("-fx-control-inner-background: #1c1c1c;" +
                            "-fx-background-color: #1c1c1c;" +
                            "-fx-text-fill: #f1f5f9;" +
                            "-fx-highlight-fill: #2563eb;" +
                            "-fx-highlight-text-fill: #ffffff;" +
                            "-fx-focus-color: transparent;" +
                            "-fx-faint-focus-color: transparent;" +
                            "-fx-background-insets: 0;");
        VBox.setVgrow(editorArea, Priority.ALWAYS);

        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            debounceTimer.playFromStart();
            updateStats();
        });

        // 3. Footer / Status bar (#161616)
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 14, 8, 14));
        footer.setStyle("-fx-background-color: #161616;" +
                        "-fx-background-radius: 0 0 12px 12px;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.06) transparent transparent transparent;" +
                        "-fx-border-width: 1px;");

        statusLabel = new Label("Tempo Real • Separe slides com '---'");
        statusLabel.setFont(Font.font("Segoe UI", 11.5));
        statusLabel.setTextFill(Color.web("#64748b"));

        footer.getChildren().add(statusLabel);

        getChildren().addAll(header, editorArea, footer);

        // Initially hidden
        setVisible(false);
        setOpacity(0.0);
    }

    public void setMarkdownContent(String content) {
        editorArea.setText(content);
        updateStats();
    }

    public String getMarkdownContent() {
        return editorArea.getText();
    }

    private void updateStats() {
        int slideCount = editorArea.getText().split("(?m)^\\s*---\\s*$").length;
        statusLabel.setText(slideCount + " slide" + (slideCount > 1 ? "s" : "") + " detectados • Atualização em tempo real");
    }

    public void showWindow() {
        if (!isVisible()) {
            setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(180), this);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
            editorArea.requestFocus();
        }
    }

    public void hideWindow() {
        if (isVisible()) {
            FadeTransition ft = new FadeTransition(Duration.millis(150), this);
            ft.setFromValue(getOpacity());
            ft.setToValue(0.0);
            ft.setOnFinished(e -> setVisible(false));
            ft.play();
        }
    }

    public void toggleWindow() {
        if (isVisible()) {
            hideWindow();
        } else {
            showWindow();
        }
    }
}
