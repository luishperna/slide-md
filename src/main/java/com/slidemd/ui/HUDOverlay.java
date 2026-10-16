package com.slidemd.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class HUDOverlay extends HBox {

    private final Label counterLabel;
    private final Button prevBtn;
    private final Button nextBtn;
    private final Button editorBtn;
    private final Button fullscreenBtn;

    private final PauseTransition hideTimer;

    public HUDOverlay(Runnable onPrev, Runnable onNext, Runnable onToggleEditor, Runnable onToggleFullscreen) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(6, 14, 6, 14));

        setStyle("-fx-background-color: rgba(15, 23, 42, 0.75);" +
                 "-fx-background-radius: 30px;" +
                 "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                 "-fx-border-radius: 30px;" +
                 "-fx-border-width: 1px;" +
                 "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 15, 0, 0, 4);");

        // Slide Counter
        counterLabel = new Label("01 / 01");
        counterLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        counterLabel.setTextFill(Color.web("#94a3b8"));

        // Navigation Buttons
        prevBtn = createIconButton("‹", "Slide Anterior (Seta Esquerda / Backspace)", onPrev);
        nextBtn = createIconButton("›", "Próximo Slide (Seta Direita / Espaço)", onNext);

        // Editor button
        editorBtn = createIconButton("MD", "Abrir/Fechar Editor Markdown (M)", onToggleEditor);
        editorBtn.setStyle("-fx-background-color: rgba(56, 189, 248, 0.2); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");

        // Fullscreen button
        fullscreenBtn = createIconButton("⛶", "Tela Cheia (F / F11)", onToggleFullscreen);

        getChildren().addAll(prevBtn, counterLabel, nextBtn, editorBtn, fullscreenBtn);

        // Auto-hide timer
        hideTimer = new PauseTransition(Duration.seconds(2.5));
        hideTimer.setOnFinished(e -> fadeOut());
    }

    private Button createIconButton(String text, String tooltipText, Runnable action) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #ffffff; -fx-background-radius: 14px; -fx-cursor: hand; -fx-padding: 2 10 2 10;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.22); -fx-text-fill: #ffffff; -fx-background-radius: 14px; -fx-cursor: hand; -fx-padding: 2 10 2 10;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #ffffff; -fx-background-radius: 14px; -fx-cursor: hand; -fx-padding: 2 10 2 10;"));
        btn.setOnAction(e -> {
            if (action != null) action.run();
        });
        if (tooltipText != null) {
            Tooltip.install(btn, new Tooltip(tooltipText));
        }
        return btn;
    }

    public void updateCounter(int currentIndex, int totalCount) {
        String currentStr = String.format("%02d", currentIndex + 1);
        String totalStr = String.format("%02d", Math.max(totalCount, 1));
        counterLabel.setText(currentStr + " / " + totalStr);

        prevBtn.setDisable(currentIndex <= 0);
        nextBtn.setDisable(currentIndex >= totalCount - 1);
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
