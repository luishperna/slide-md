package com.slidemd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * Card flutuante interativo para seleção de cor ou gradiente de fundo (bg) do slide.
 */
public class SlideColorPickerPopup {

    private final Popup popup;
    private final Consumer<String> onColorSelected;

    private static final record ColorPreset(String name, String hex) {}

    private static final List<ColorPreset> PRESETS = List.of(
            new ColorPreset("Obsidian", "#090d16"),
            new ColorPreset("Slate Dark", "#0f172a"),
            new ColorPreset("Slate Midnight", "#1e293b"),
            new ColorPreset("Zinc Dark", "#18181b"),
            new ColorPreset("Gray Deep", "#111827"),
            new ColorPreset("Pure Black", "#000000"),
            new ColorPreset("Deep Blue", "#1e3a8a"),
            new ColorPreset("Emerald Dark", "#064e3b"),
            new ColorPreset("Purple Indigo", "#581c87"),
            new ColorPreset("Bronze Auburn", "#7c2d12"),
            new ColorPreset("Rose Deep", "#831843"),
            new ColorPreset("Studio Dark", "#1e1e1e")
    );

    private static final record GradientPreset(String name, String css) {}

    private static final List<GradientPreset> GRADIENTS = List.of(
            new GradientPreset("Obsidian & Slate", "linear-gradient(to bottom right, #090d16, #1e293b)"),
            new GradientPreset("Midnight Blue", "linear-gradient(to bottom right, #0f172a, #1e3a8a)"),
            new GradientPreset("Emerald Deep", "linear-gradient(to bottom right, #111827, #064e3b)"),
            new GradientPreset("Purple Night", "linear-gradient(to bottom right, #18181b, #581c87)")
    );

    public SlideColorPickerPopup(Consumer<String> onColorSelected) {
        this.onColorSelected = onColorSelected;
        this.popup = new Popup();
        this.popup.setAutoHide(true);
        this.popup.setHideOnEscape(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(12, 14, 14, 14));
        root.setPrefWidth(300);
        root.setStyle("-fx-background-color: #181818;" +
                      "-fx-background-radius: 8px;" +
                      "-fx-border-color: #2a2a2a;" +
                      "-fx-border-radius: 8px;" +
                      "-fx-border-width: 1px;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 18, 0.25, 0, 4);");

        // Header: Título e Botão Fechar
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🎨 Fundo do Slide (bg)");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#ffffff"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888888; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4; -fx-background-radius: 3px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888888; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4;"));
        closeBtn.setOnAction(e -> popup.hide());

        header.getChildren().addAll(title, spacer, closeBtn);

        // Seção: Cores Sólidas Populares
        Label presetsTitle = new Label("Cores Sólidas Populares");
        presetsTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
        presetsTitle.setTextFill(Color.web("#a3a3a3"));

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER_LEFT);

        int col = 0;
        int row = 0;
        for (ColorPreset cp : PRESETS) {
            Button btn = createColorButton(cp.hex, cp.name + " (" + cp.hex + ")");
            grid.add(btn, col, row);
            col++;
            if (col >= 6) {
                col = 0;
                row++;
            }
        }

        // Seção: Gradientes Modernos
        Label gradientTitle = new Label("Gradientes Elegantes");
        gradientTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
        gradientTitle.setTextFill(Color.web("#a3a3a3"));

        VBox gradList = new VBox(4);
        for (GradientPreset gp : GRADIENTS) {
            Button gradBtn = createGradientButton(gp.name, gp.css);
            gradList.getChildren().add(gradBtn);
        }

        // Seção: Seletor de Cor Customizado (ColorPicker nativo)
        HBox customRow = new HBox(8);
        customRow.setAlignment(Pos.CENTER_LEFT);

        Label customLabel = new Label("Personalizada:");
        customLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
        customLabel.setTextFill(Color.web("#a3a3a3"));

        ColorPicker colorPicker = new ColorPicker(Color.web("#1e293b"));
        colorPicker.setStyle("-fx-background-color: #262626; -fx-color-label-visible: false; -fx-cursor: hand; -fx-background-radius: 4px;");
        colorPicker.setOnAction(e -> {
            Color c = colorPicker.getValue();
            if (c != null) {
                String hex = String.format("#%02x%02x%02x",
                        (int) Math.round(c.getRed() * 255),
                        (int) Math.round(c.getGreen() * 255),
                        (int) Math.round(c.getBlue() * 255));
                selectColor(hex);
            }
        });

        customRow.getChildren().addAll(customLabel, colorPicker);

        root.getChildren().addAll(header, presetsTitle, grid, gradientTitle, gradList, customRow);
        popup.getContent().add(root);
    }

    private Button createColorButton(String hex, String tooltip) {
        Button btn = new Button();
        btn.setPrefSize(34, 28);
        btn.setMinSize(34, 28);
        btn.setMaxSize(34, 28);

        String baseStyle = "-fx-background-color: " + hex + ";" +
                           "-fx-background-radius: 4px;" +
                           "-fx-border-color: #383838;" +
                           "-fx-border-radius: 4px;" +
                           "-fx-border-width: 1px;" +
                           "-fx-cursor: hand;";

        String hoverStyle = "-fx-background-color: " + hex + ";" +
                            "-fx-background-radius: 4px;" +
                            "-fx-border-color: #ffffff;" +
                            "-fx-border-radius: 4px;" +
                            "-fx-border-width: 1.5px;" +
                            "-fx-cursor: hand;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        btn.setOnAction(e -> selectColor(hex));
        Tooltip.install(btn, new Tooltip(tooltip));
        return btn;
    }

    private Button createGradientButton(String name, String css) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(3, 8, 3, 8));

        Rectangle preview = new Rectangle(24, 14);
        preview.setArcWidth(4);
        preview.setArcHeight(4);
        preview.setStyle("-fx-fill: " + css + "; -fx-stroke: #404040; -fx-stroke-width: 0.5px;");

        Label label = new Label(name);
        label.setFont(Font.font("Segoe UI", 11));
        label.setTextFill(Color.web("#e5e5e5"));

        HBox content = new HBox(8, preview, label);
        content.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(content);

        String base = "-fx-background-color: #222222; -fx-background-radius: 4px; -fx-cursor: hand; -fx-border-color: transparent;";
        String hover = "-fx-background-color: #2c2c2c; -fx-background-radius: 4px; -fx-cursor: hand; -fx-border-color: #38bdf8; -fx-border-radius: 4px; -fx-border-width: 1px;";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> selectColor(css));
        return btn;
    }

    private void selectColor(String value) {
        popup.hide();
        if (onColorSelected != null) {
            onColorSelected.accept(value);
        }
    }

    public void show(Stage ownerStage, double screenX, double screenY) {
        if (popup.isShowing()) {
            popup.hide();
        }
        popup.show(ownerStage, screenX, screenY);
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }
}
