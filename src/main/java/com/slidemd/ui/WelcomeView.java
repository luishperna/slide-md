package com.slidemd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class WelcomeView extends VBox {

    public WelcomeView(Runnable onNewPresentation, Runnable onOpenPresentation) {
        setAlignment(Pos.CENTER);
        setSpacing(32);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: radial-gradient(center 50% 40%, radius 70%, #111827 0%, #030712 100%);");

        // 1. Logo & App Title
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);

        Label logoLabel = new Label("⚡ slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 42));
        logoLabel.setTextFill(Color.web("#38bdf8"));
        logoLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(56, 189, 248, 0.45), 30, 0.2, 0, 0);");

        Label subtitleLabel = new Label("Apresentações Modernas, Rápidas e Minimalistas em Markdown");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 15));
        subtitleLabel.setTextFill(Color.web("#94a3b8"));

        headerBox.getChildren().addAll(logoLabel, subtitleLabel);

        // 2. Action Cards Container
        HBox cardsBox = new HBox(24);
        cardsBox.setAlignment(Pos.CENTER);

        VBox newCard = createActionCard(
                createNewDocIcon(),
                "Nova Apresentação",
                "Comece uma apresentação limpa do zero com editor ao vivo",
                "Ctrl + N",
                "#38bdf8",
                onNewPresentation
        );

        VBox openCard = createActionCard(
                createFolderIcon(),
                "Abrir Apresentação",
                "Escolha um arquivo Markdown (.md) salvo no computador",
                "Ctrl + O",
                "#fbbf24",
                onOpenPresentation
        );

        cardsBox.getChildren().addAll(newCard, openCard);

        // 3. Drag & Drop Helper
        Label dragHint = new Label("Ou arraste e solte um arquivo .md aqui nesta janela");
        dragHint.setFont(Font.font("Segoe UI", 12.5));
        dragHint.setTextFill(Color.web("#64748b"));
        dragHint.setStyle("-fx-border-color: rgba(255, 255, 255, 0.12);" +
                          "-fx-border-style: dashed;" +
                          "-fx-border-radius: 8px;" +
                          "-fx-padding: 8 24 8 24;");

        getChildren().addAll(headerBox, cardsBox, dragHint);
    }

    private VBox createActionCard(SVGPath icon, String title, String description, String badgeText, String accentColor, Runnable action) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(260, 190);
        card.setMaxSize(260, 190);
        card.setPadding(new Insets(24, 20, 24, 20));
        card.setCursor(Cursor.HAND);

        String baseStyle = "-fx-background-color: #161616;" +
                           "-fx-background-radius: 14px;" +
                           "-fx-border-color: rgba(255, 255, 255, 0.08);" +
                           "-fx-border-radius: 14px;" +
                           "-fx-border-width: 1px;" +
                           "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 18, 0, 0, 6);";

        String hoverStyle = "-fx-background-color: #1e2433;" +
                            "-fx-background-radius: 14px;" +
                            "-fx-border-color: " + accentColor + ";" +
                            "-fx-border-radius: 14px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-effect: dropshadow(gaussian, " + accentColor + ", 24, 0.35, 0, 4);";

        card.setStyle(baseStyle);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#f1f5f9"));

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.web("#94a3b8"));
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setWrapText(true);

        Label badge = new Label(badgeText);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        badge.setTextFill(Color.web(accentColor));
        badge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.06); -fx-background-radius: 6px; -fx-padding: 3 8 3 8;");

        card.getChildren().addAll(icon, titleLabel, descLabel, badge);

        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> {
            if (action != null) action.run();
        });

        return card;
    }

    private SVGPath createNewDocIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 4 2 L 14 2 L 20 8 L 20 22 L 4 22 Z M 14 2 L 14 8 L 20 8 M 8 15 L 16 15 M 12 11 L 12 19");
        path.setStroke(Color.web("#38bdf8"));
        path.setStrokeWidth(2.0);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createFolderIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3 5 L 9 5 L 12 8 L 21 8 L 21 19 L 3 19 Z");
        path.setStroke(Color.web("#fbbf24"));
        path.setStrokeWidth(2.0);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }
}
