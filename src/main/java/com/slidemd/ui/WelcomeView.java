package com.slidemd.ui;

import com.slidemd.model.RecentFilesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class WelcomeView extends VBox {

    public WelcomeView(Runnable onNewPresentation, Runnable onOpenPresentation) {
        this(onNewPresentation, onOpenPresentation, null);
    }

    public WelcomeView(Runnable onNewPresentation, Runnable onOpenPresentation, Consumer<File> onOpenRecentPresentation) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(24, 40, 24, 40));
        // Pure monochrome #101010 background as requested
        setStyle("-fx-background-color: #101010;");

        // 1. Logo & App Title (Monochrome White & Slate)
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label logoLabel = new Label("⚡ slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 36));
        logoLabel.setTextFill(Color.web("#ffffff"));
        logoLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.15), 18, 0.1, 0, 0);");

        Label subtitleLabel = new Label("Apresentações modernas, rápidas e fáceis em Markdown");
        subtitleLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13.5));
        subtitleLabel.setTextFill(Color.web("#737373"));

        headerBox.getChildren().addAll(logoLabel, subtitleLabel);

        // 2. Action Cards Container (Monochrome Dark & Gray Shades)
        HBox cardsBox = new HBox(20);
        cardsBox.setAlignment(Pos.CENTER);

        VBox newCard = createActionCard(
                createNewDocIcon(),
                "Nova Apresentação",
                "Comece uma apresentação limpa do zero com o editor Markdown",
                "Ctrl + N",
                onNewPresentation
        );

        VBox openCard = createActionCard(
                createFolderIcon(),
                "Abrir Apresentação",
                "Escolha um arquivo Markdown (.md) salvo no computador",
                "Ctrl + O",
                onOpenPresentation
        );
        cardsBox.getChildren().addAll(newCard, openCard);

        // 3. Histórico de Apresentações Recentes
        List<File> recents = RecentFilesManager.getRecentFiles();
        VBox recentsBox = new VBox(8);
        recentsBox.setAlignment(Pos.CENTER);
        recentsBox.setMaxWidth(540);

        if (onOpenRecentPresentation != null && !recents.isEmpty()) {
            HBox recentHeader = new HBox(8);
            recentHeader.setAlignment(Pos.CENTER_LEFT);
            recentHeader.setMaxWidth(540);

            Label recentTitle = new Label("APRESENTAÇÕES RECENTES");
            recentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
            recentTitle.setTextFill(Color.web("#808080"));

            Region recentSpacer = new Region();
            HBox.setHgrow(recentSpacer, Priority.ALWAYS);

            recentHeader.getChildren().addAll(recentTitle, recentSpacer);

            VBox recentItemsList = new VBox(5);
            recentItemsList.setMaxWidth(540);

            for (File file : recents.stream().limit(3).toList()) {
                HBox row = createRecentFileRow(file, onOpenRecentPresentation, () -> {
                    RecentFilesManager.removeRecentFile(file);
                    recentItemsList.getChildren().removeIf(node -> node.getUserData() == file);
                    if (recentItemsList.getChildren().isEmpty()) {
                        recentsBox.setVisible(false);
                        recentsBox.setManaged(false);
                    }
                });
                row.setUserData(file);
                recentItemsList.getChildren().add(row);
            }

            recentsBox.getChildren().addAll(recentHeader, recentItemsList);
        } else {
            recentsBox.setVisible(false);
            recentsBox.setManaged(false);
        }

        // 4. Drag & Drop Helper
        Label dragHint = new Label("Ou arraste e solte um arquivo .md aqui nesta janela");
        dragHint.setFont(Font.font("Segoe UI", 12));
        dragHint.setTextFill(Color.web("#737373"));
        dragHint.setStyle("-fx-border-color: #262626;" +
                          "-fx-border-style: dashed;" +
                          "-fx-border-radius: 8px;" +
                          "-fx-padding: 6 20 6 20;");

        getChildren().addAll(headerBox, cardsBox, recentsBox, dragHint);
    }

    private VBox createActionCard(SVGPath icon, String title, String description, String badgeText, Runnable action) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(260, 190);
        card.setMaxSize(260, 190);
        card.setPadding(new Insets(24, 20, 24, 20));
        card.setCursor(Cursor.HAND);

        // Sleek monochrome styling without colored borders
        String baseStyle = "-fx-background-color: #161616;" +
                           "-fx-background-radius: 12px;" +
                           "-fx-border-color: #262626;" +
                           "-fx-border-radius: 12px;" +
                           "-fx-border-width: 1px;" +
                           "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 16, 0, 0, 4);";

        String hoverStyle = "-fx-background-color: #1c1c1c;" +
                            "-fx-background-radius: 12px;" +
                            "-fx-border-color: #525252;" +
                            "-fx-border-radius: 12px;" +
                            "-fx-border-width: 1px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.08), 20, 0, 0, 2);";

        card.setStyle(baseStyle);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15.5));
        titleLabel.setTextFill(Color.web("#ffffff"));

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.web("#a3a3a3"));
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setWrapText(true);

        Label badge = new Label(badgeText);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10.5));
        badge.setTextFill(Color.web("#d4d4d4"));
        badge.setStyle("-fx-background-color: #262626; -fx-background-radius: 6px; -fx-padding: 3 8 3 8;");

        card.getChildren().addAll(icon, titleLabel, descLabel, badge);

        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
            card.setScaleX(1.025);
            card.setScaleY(1.025);
            icon.setStroke(Color.WHITE);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            icon.setStroke(Color.web("#e5e5e5"));
        });

        card.setOnMouseClicked(e -> {
            if (action != null) action.run();
        });

        return card;
    }

    private SVGPath createNewDocIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 4 2 L 14 2 L 20 8 L 20 22 L 4 22 Z M 14 2 L 14 8 L 20 8 M 8 15 L 16 15 M 12 11 L 12 19");
        path.setStroke(Color.web("#e5e5e5"));
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createFolderIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3 5 L 9 5 L 12 8 L 21 8 L 21 19 L 3 19 Z");
        path.setStroke(Color.web("#e5e5e5"));
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private HBox createRecentFileRow(File file, Consumer<File> onOpen, Runnable onRemove) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));
        row.setPrefWidth(540);
        row.setMaxWidth(540);
        row.setCursor(Cursor.HAND);

        String baseStyle = "-fx-background-color: #161616;" +
                           "-fx-background-radius: 8px;" +
                           "-fx-border-color: #242424;" +
                           "-fx-border-radius: 8px;" +
                           "-fx-border-width: 1px;";

        String hoverStyle = "-fx-background-color: #1e1e1e;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-border-color: #ffffff;" +
                            "-fx-border-radius: 8px;" +
                            "-fx-border-width: 1px;";

        row.setStyle(baseStyle);

        SVGPath fileIcon = createRecentDocIcon();

        VBox textInfo = new VBox(2);
        Label nameLabel = new Label(file.getName());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        nameLabel.setTextFill(Color.web("#ffffff"));

        String parentPath = file.getParent() != null ? file.getParent() : file.getAbsolutePath();
        Label pathLabel = new Label(parentPath);
        pathLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
        pathLabel.setTextFill(Color.web("#737373"));
        pathLabel.setMaxWidth(400);

        textInfo.getChildren().addAll(nameLabel, pathLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-font-size: 11px;");
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-font-size: 11px; -fx-background-radius: 4px;"));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #555555; -fx-cursor: hand; -fx-padding: 2 6 2 6; -fx-font-size: 11px;"));
        removeBtn.setOnAction(e -> {
            e.consume();
            if (onRemove != null) onRemove.run();
        });

        row.getChildren().addAll(fileIcon, textInfo, spacer, removeBtn);

        row.setOnMouseEntered(e -> {
            row.setStyle(hoverStyle);
            row.setScaleX(1.008);
            row.setScaleY(1.008);
            fileIcon.setStroke(Color.WHITE);
        });

        row.setOnMouseExited(e -> {
            row.setStyle(baseStyle);
            row.setScaleX(1.0);
            row.setScaleY(1.0);
            fileIcon.setStroke(Color.web("#808080"));
        });

        row.setOnMouseClicked(e -> {
            if (onOpen != null) onOpen.accept(file);
        });

        return row;
    }

    private SVGPath createRecentDocIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 2 2 L 8 2 L 12 6 L 12 16 L 2 16 Z M 8 2 L 8 6 L 12 6 M 4 10 L 10 10 M 4 13 L 8 13");
        path.setStroke(Color.web("#808080"));
        path.setStrokeWidth(1.2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }
}
