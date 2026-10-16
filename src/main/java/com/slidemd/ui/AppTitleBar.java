package com.slidemd.ui;

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

public class AppTitleBar extends HBox {

    private final Label titleLabel;
    private final Button maxRestoreBtn;
    private final SVGPath maxIcon;
    private final SVGPath restoreIcon;

    private final Button prevBtn;
    private final Button nextBtn;
    private final SVGPath prevIcon;
    private final SVGPath nextIcon;
    private final Label counterLabel;
    private final HBox navGroup;

    private double dragOffsetX;
    private double dragOffsetY;

    public AppTitleBar(Stage stage, String initialTitle,
                       Runnable onNewFile, Runnable onOpenFile, Runnable onToggleEditor,
                       Runnable onPrevSlide, Runnable onNextSlide) {
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(38);
        setMinHeight(38);
        setMaxHeight(38);
        setPadding(new Insets(0, 0, 0, 14));

        // Antigravity dark theme: #161616
        setStyle("-fx-background-color: #161616;" +
                 "-fx-border-color: transparent transparent rgba(255, 255, 255, 0.06) transparent;" +
                 "-fx-border-width: 1px;");

        // 1. Left Section: Logo & Presentation Title
        Label logoLabel = new Label("⚡ slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        logoLabel.setTextFill(Color.web("#38bdf8"));

        Label separator = new Label("—");
        separator.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        separator.setTextFill(Color.web("#475569"));
        HBox.setMargin(separator, new Insets(0, 8, 0, 8));

        titleLabel = new Label(initialTitle != null ? initialTitle : "Início");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.web("#94a3b8"));
        titleLabel.setMaxWidth(200);

        HBox leftBox = new HBox(logoLabel, separator, titleLabel);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        // 2. Center Section: Centralized Modern Action & Navigation Dock
        HBox centerDock = new HBox(6);
        centerDock.setAlignment(Pos.CENTER);
        centerDock.setPadding(new Insets(3, 10, 3, 10));
        centerDock.setStyle("-fx-background-color: #1f1f1f;" +
                            "-fx-background-radius: 20px;" +
                            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                            "-fx-border-radius: 20px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 10, 0, 0, 2);");

        Button newBtn = createDockButton(createNewDocIcon(), "Nova Apresentação (Ctrl+N)", onNewFile);
        Button openBtn = createDockButton(createFolderIcon(), "Abrir Apresentação Existente (Ctrl+O)", onOpenFile);
        Button editorBtn = createDockButton(createEditorIcon(), "Editor Markdown (M / E)", onToggleEditor);

        Label dockDivider = new Label("|");
        dockDivider.setTextFill(Color.web("#475569"));
        dockDivider.setFont(Font.font("Segoe UI", 12));
        HBox.setMargin(dockDivider, new Insets(0, 4, 0, 4));

        prevIcon = createLeftChevron();
        prevBtn = createDockButton(prevIcon, "Slide Anterior (Seta Esquerda / Clique à Esquerda)", onPrevSlide);

        counterLabel = new Label("-- / --");
        counterLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11.5));
        counterLabel.setTextFill(Color.web("#cbd5e1"));
        HBox.setMargin(counterLabel, new Insets(0, 6, 0, 6));

        nextIcon = createRightChevron();
        nextBtn = createDockButton(nextIcon, "Próximo Slide (Seta Direita / Espaço / Clique à Direita)", onNextSlide);

        navGroup = new HBox(4, dockDivider, prevBtn, counterLabel, nextBtn);
        navGroup.setAlignment(Pos.CENTER);
        navGroup.setVisible(false);
        navGroup.setManaged(false);

        centerDock.getChildren().addAll(newBtn, openBtn, editorBtn, navGroup);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // 3. Right Section: Window Controls (Minimize, Fullscreen, Close)
        SVGPath minIcon = createMinIcon();
        maxIcon = createMaxIcon();
        restoreIcon = createRestoreIcon();
        SVGPath closeIcon = createCloseIcon();

        Button minimizeBtn = createWindowButton(minIcon, false, "Minimizar", () -> stage.setIconified(true));

        maxRestoreBtn = createWindowButton(maxIcon, false, "Expandir para Tela Cheia (F11)", () -> {
            stage.setFullScreen(!stage.isFullScreen());
        });

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

        HBox controls = new HBox(minimizeBtn, maxRestoreBtn, closeBtn);
        controls.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(leftBox, leftSpacer, centerDock, rightSpacer, controls);

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

    public void updateCounter(int currentSlide, int totalSlides) {
        if (totalSlides <= 0) {
            setNavigationVisible(false);
            return;
        }
        setNavigationVisible(true);
        counterLabel.setText(String.format("%02d / %02d", currentSlide + 1, totalSlides));
        prevBtn.setDisable(currentSlide <= 0);
        nextBtn.setDisable(currentSlide >= totalSlides - 1);
        prevIcon.setStroke(currentSlide <= 0 ? Color.web("#475569") : Color.web("#38bdf8"));
        nextIcon.setStroke(currentSlide >= totalSlides - 1 ? Color.web("#475569") : Color.web("#38bdf8"));
    }

    public void setNavigationVisible(boolean visible) {
        navGroup.setVisible(visible);
        navGroup.setManaged(visible);
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null && !title.isBlank() ? title : "Início");
    }

    private Button createDockButton(SVGPath icon, String tooltipText, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setPrefWidth(28);
        btn.setPrefHeight(26);
        btn.setMinHeight(26);
        btn.setMaxHeight(26);
        btn.setAlignment(Pos.CENTER);

        String baseStyle = "-fx-background-color: transparent;" +
                           "-fx-background-radius: 4px;" +
                           "-fx-cursor: hand;" +
                           "-fx-padding: 0;";

        String hoverStyle = "-fx-background-color: #2e2e2e;" +
                            "-fx-background-radius: 4px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 0;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            if (!btn.isDisabled()) icon.setStroke(Color.WHITE);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            if (!btn.isDisabled()) {
                icon.setStroke(icon == prevIcon || icon == nextIcon ? Color.web("#38bdf8") : Color.web("#94a3b8"));
            }
        });

        btn.setOnAction(e -> {
            if (action != null) action.run();
        });

        if (tooltipText != null) {
            Tooltip.install(btn, new Tooltip(tooltipText));
        }

        return btn;
    }

    private SVGPath createNewDocIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 2 1.5 L 6.5 1.5 L 9.5 4.5 L 9.5 11.5 L 2 11.5 Z M 6.5 1.5 L 6.5 4.5 L 9.5 4.5 M 4 7.5 L 7.5 7.5 M 5.75 5.75 L 5.75 9.25");
        path.setStroke(Color.web("#94a3b8"));
        path.setStrokeWidth(1.15);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createFolderIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 1.5 2.5 L 4.5 2.5 L 6 4 L 10.5 4 L 10.5 10.5 L 1.5 10.5 Z");
        path.setStroke(Color.web("#94a3b8"));
        path.setStrokeWidth(1.15);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createEditorIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 1.5 10.5 L 4 10.5 L 10.5 4 L 8 1.5 L 1.5 8 Z M 7 2.5 L 9.5 5");
        path.setStroke(Color.web("#94a3b8"));
        path.setStrokeWidth(1.15);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createLeftChevron() {
        SVGPath path = new SVGPath();
        path.setContent("M 6.5 2 L 2.5 6 L 6.5 10");
        path.setStroke(Color.web("#38bdf8"));
        path.setStrokeWidth(1.4);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createRightChevron() {
        SVGPath path = new SVGPath();
        path.setContent("M 3.5 2 L 7.5 6 L 3.5 10");
        path.setStroke(Color.web("#38bdf8"));
        path.setStrokeWidth(1.4);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
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
        btn.setPrefHeight(38);
        btn.setMinHeight(38);
        btn.setMaxHeight(38);
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
