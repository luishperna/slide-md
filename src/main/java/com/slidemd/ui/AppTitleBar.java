package com.slidemd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class AppTitleBar extends HBox {

    private final Label titleLabel;
    private final Button maxRestoreBtn;
    private double dragOffsetX;
    private double dragOffsetY;

    public AppTitleBar(Stage stage, String initialTitle) {
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(34);
        setMinHeight(34);
        setMaxHeight(34);
        setPadding(new Insets(0, 0, 0, 12));

        // Background #2d2d2d as requested by user
        setStyle("-fx-background-color: #2d2d2d;" +
                 "-fx-border-color: transparent transparent rgba(255, 255, 255, 0.08) transparent;" +
                 "-fx-border-width: 1px;");

        // 1. App Logo / Name
        Label logoLabel = new Label("⚡ slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        logoLabel.setTextFill(Color.web("#38bdf8"));

        Label separator = new Label("—");
        separator.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        separator.setTextFill(Color.web("#64748b"));
        HBox.setMargin(separator, new Insets(0, 6, 0, 6));

        titleLabel = new Label(initialTitle != null ? initialTitle : "Apresentação");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.web("#cbd5e1"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Window Control Buttons (Minimize, Maximize, Close)
        Button minimizeBtn = createWindowButton("—", false, () -> stage.setIconified(true));
        maxRestoreBtn = createWindowButton("□", false, () -> {
            stage.setMaximized(!stage.isMaximized());
            updateMaxRestoreIcon(stage.isMaximized());
        });
        Button closeBtn = createWindowButton("✕", true, stage::close);

        HBox controls = new HBox(minimizeBtn, maxRestoreBtn, closeBtn);
        controls.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(logoLabel, separator, titleLabel, spacer, controls);

        // Window dragging logic
        setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        setOnMouseDragged(e -> {
            if (!stage.isMaximized() && !stage.isFullScreen()) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });

        // Double click to maximize / restore
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
                updateMaxRestoreIcon(stage.isMaximized());
            }
        });

        // Listen for maximize state changes
        stage.maximizedProperty().addListener((obs, oldVal, isMax) -> updateMaxRestoreIcon(isMax));
    }

    private Button createWindowButton(String text, boolean isClose, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        btn.setPrefWidth(44);
        btn.setPrefHeight(34);
        btn.setMinHeight(34);
        btn.setMaxHeight(34);

        String baseStyle = "-fx-background-color: transparent;" +
                           "-fx-text-fill: #94a3b8;" +
                           "-fx-background-radius: 0;" +
                           "-fx-cursor: hand;" +
                           "-fx-padding: 0;";

        String hoverStyle = isClose
            ? "-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;"
            : "-fx-background-color: #3e3e3e; -fx-text-fill: #ffffff; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));

        btn.setOnAction(e -> {
            if (action != null) action.run();
        });

        return btn;
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null ? title : "Apresentação");
    }

    private void updateMaxRestoreIcon(boolean isMaximized) {
        maxRestoreBtn.setText(isMaximized ? "❐" : "□");
    }
}
