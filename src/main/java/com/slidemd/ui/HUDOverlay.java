package com.slidemd.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class HUDOverlay extends HBox {

    private final Button prevBtn;
    private final Button nextBtn;
    private final Button editorBtn;
    private final Button openBtn;
    private final Button fullscreenBtn;

    private final PauseTransition hideTimer;

    public HUDOverlay(Runnable onPrev, Runnable onNext, Runnable onToggleEditor, Runnable onOpenFile, Runnable onToggleFullscreen) {
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(6, 18, 6, 18));

        // Ensure HUD stays as a compact floating card at bottom-center (never stretch vertically)
        setMaxHeight(Region.USE_PREF_SIZE);
        setMaxWidth(Region.USE_PREF_SIZE);

        // Modern Frosted Glass Card with Rounded Pill Shape
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.45);" +
                 "-fx-background-radius: 40px;" +
                 "-fx-border-color: rgba(255, 255, 255, 0.16);" +
                 "-fx-border-radius: 40px;" +
                 "-fx-border-width: 1px;" +
                 "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 25, 0, 0, 8);");

        // Transparent buttons with colored icons only
        prevBtn = createIconButton("‹", "#38bdf8", 20, "Slide Anterior (Seta Esquerda / Espaço)", onPrev);
        nextBtn = createIconButton("›", "#38bdf8", 20, "Próximo Slide (Seta Direita)", onNext);
        editorBtn = createIconButton("MD", "#a78bfa", 12.5, "Editor Markdown Flutuante (M)", onToggleEditor);
        openBtn = createIconButton("📂", "#fbbf24", 15, "Abrir Arquivo Markdown (Ctrl+O)", onOpenFile);
        fullscreenBtn = createIconButton("⛶", "#f1f5f9", 14, "Tela Cheia (F / F11)", onToggleFullscreen);

        // Card contains strictly the modern action buttons
        getChildren().addAll(prevBtn, nextBtn, editorBtn, openBtn, fullscreenBtn);

        // Auto-hide timer
        hideTimer = new PauseTransition(Duration.seconds(2.5));
        hideTimer.setOnFinished(e -> fadeOut());
    }

    private Button createIconButton(String text, String iconColor, double fontSize, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));

        String baseStyle = "-fx-background-color: transparent;" +
                           "-fx-text-fill: " + iconColor + ";" +
                           "-fx-cursor: hand;" +
                           "-fx-padding: 3 8 3 8;";

        String hoverStyle = "-fx-background-color: transparent;" +
                            "-fx-text-fill: #ffffff;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 3 8 3 8;" +
                            "-fx-effect: dropshadow(gaussian, " + iconColor + ", 12, 0.4, 0, 0);";

        btn.setStyle(baseStyle);

        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            btn.setScaleX(1.18);
            btn.setScaleY(1.18);
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });

        btn.setOnAction(e -> {
            if (action != null) action.run();
        });

        if (tooltipText != null) {
            Tooltip.install(btn, new Tooltip(tooltipText));
        }

        return btn;
    }

    public void updateCounter(int currentIndex, int totalCount) {
        prevBtn.setDisable(currentIndex <= 0);
        nextBtn.setDisable(currentIndex >= totalCount - 1);

        String counterInfo = String.format("Slide %d de %d", currentIndex + 1, Math.max(totalCount, 1));
        Tooltip.install(prevBtn, new Tooltip("Slide Anterior (" + counterInfo + ")"));
        Tooltip.install(nextBtn, new Tooltip("Próximo Slide (" + counterInfo + ")"));
    }

    public void bindSceneActivity(Scene scene) {
        scene.setOnMouseMoved(e -> {
            fadeIn();
            scene.setCursor(Cursor.DEFAULT);
            hideTimer.playFromStart();
        });
    }

    public void fadeIn() {
        if (getOpacity() < 1.0) {
            FadeTransition ft = new FadeTransition(Duration.millis(200), this);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public void fadeOut() {
        if (getOpacity() > 0.0) {
            FadeTransition ft = new FadeTransition(Duration.millis(400), this);
            ft.setToValue(0.0);
            ft.play();
        }
    }
}
