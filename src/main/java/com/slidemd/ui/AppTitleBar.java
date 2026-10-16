package com.slidemd.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AppTitleBar extends HBox {

    private final Label titleLabel;
    private final Button maxRestoreBtn;
    private final SVGPath maxIcon;
    private final SVGPath restoreIcon;
    private final HBox controls;

    private double dragOffsetX;
    private double dragOffsetY;

    public AppTitleBar(Stage stage, String initialTitle) {
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(34);
        setMinHeight(34);
        setMaxHeight(34);
        setPadding(new Insets(0, 0, 0, 14));

        // Antigravity dark theme: #161616
        setStyle("-fx-background-color: #161616;" +
                 "-fx-border-color: transparent transparent rgba(255, 255, 255, 0.06) transparent;" +
                 "-fx-border-width: 1px;");

        // 1. App Logo / Name
        Label logoLabel = new Label("⚡ slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        logoLabel.setTextFill(Color.web("#38bdf8"));

        Label separator = new Label("—");
        separator.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        separator.setTextFill(Color.web("#475569"));
        HBox.setMargin(separator, new Insets(0, 8, 0, 8));

        titleLabel = new Label(initialTitle != null ? initialTitle : "Apresentação");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.web("#94a3b8"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2. Vector SVG Icons matching Antigravity / Spotify sleek style
        SVGPath minIcon = createMinIcon();
        maxIcon = createMaxIcon();
        restoreIcon = createRestoreIcon();
        SVGPath closeIcon = createCloseIcon();

        Button minimizeBtn = createWindowButton(minIcon, false, "Minimizar", () -> stage.setIconified(true));

        maxRestoreBtn = createWindowButton(maxIcon, false, "Expandir para Tela Cheia (F11)", () -> {
            stage.setFullScreen(!stage.isFullScreen());
        });

        // Handle dual icons for maxRestoreBtn
        maxRestoreBtn.setOnMouseEntered(e -> {
            maxRestoreBtn.setStyle("-fx-background-color: #262626; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;");
            maxIcon.setStroke(Color.WHITE);
            restoreIcon.setStroke(Color.WHITE);
        });
        maxRestoreBtn.setOnMouseExited(e -> {
            maxRestoreBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;");
            maxIcon.setStroke(Color.web("#cccccc"));
            restoreIcon.setStroke(Color.web("#cccccc"));
        });

        Button closeBtn = createWindowButton(closeIcon, true, "Fechar", stage::close);

        controls = new HBox(minimizeBtn, maxRestoreBtn, closeBtn);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setOpacity(0.0); // Hidden by default, shown when mouse enters title bar

        getChildren().addAll(logoLabel, separator, titleLabel, spacer, controls);

        // Show window control icons only when mouse is inside the title bar
        setOnMouseEntered(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(140), controls);
            ft.setToValue(1.0);
            ft.play();
        });

        setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), controls);
            ft.setToValue(0.0);
            ft.play();
        });

        // Window dragging logic (only when not fullscreen)
        setOnMousePressed(e -> {
            if (!stage.isFullScreen()) {
                dragOffsetX = e.getScreenX() - stage.getX();
                dragOffsetY = e.getScreenY() - stage.getY();
            }
        });

        setOnMouseDragged(e -> {
            if (!stage.isFullScreen()) {
                stage.setX(e.getScreenX() - dragOffsetX);
                stage.setY(e.getScreenY() - dragOffsetY);
            }
        });

        // Double click to expand to fullscreen / restore
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });

        // Listen for fullscreen state changes to update the icon
        stage.fullScreenProperty().addListener((obs, oldVal, isFull) -> updateMaxRestoreIcon(isFull));
    }

    private SVGPath createMinIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0 5 L 10 5");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.2);
        path.setStrokeLineCap(StrokeLineCap.BUTT);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createMaxIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0.5 0.5 L 9.5 0.5 L 9.5 9.5 L 0.5 9.5 Z");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.1);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createRestoreIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 2.5 0.5 L 9.5 0.5 L 9.5 7.5 L 7.5 7.5 M 7.5 2.5 L 0.5 2.5 L 0.5 9.5 L 7.5 9.5 Z");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.1);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createCloseIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0.5 0.5 L 9.5 9.5 M 9.5 0.5 L 0.5 9.5");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.2);
        path.setStrokeLineCap(StrokeLineCap.BUTT);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private Button createWindowButton(SVGPath icon, boolean isClose, String tooltipText, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setPrefWidth(46);
        btn.setPrefHeight(34);
        btn.setMinHeight(34);
        btn.setMaxHeight(34);
        btn.setAlignment(Pos.CENTER);

        String baseStyle = "-fx-background-color: transparent;" +
                           "-fx-background-radius: 0;" +
                           "-fx-cursor: hand;" +
                           "-fx-padding: 0;";

        String hoverStyle = isClose
            ? "-fx-background-color: #e81123; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;"
            : "-fx-background-color: #262626; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            icon.setStroke(Color.WHITE);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            icon.setStroke(Color.web("#cccccc"));
        });

        btn.setOnAction(e -> {
            if (action != null) action.run();
        });

        if (tooltipText != null) {
            Tooltip.install(btn, new Tooltip(tooltipText));
        }

        return btn;
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null ? title : "Apresentação");
    }

    private void updateMaxRestoreIcon(boolean isFullScreen) {
        if (isFullScreen) {
            maxRestoreBtn.setGraphic(restoreIcon);
            Tooltip.install(maxRestoreBtn, new Tooltip("Sair da Tela Cheia (Esc / F11)"));
        } else {
            maxRestoreBtn.setGraphic(maxIcon);
            Tooltip.install(maxRestoreBtn, new Tooltip("Expandir para Tela Cheia (F11)"));
        }
    }
}
