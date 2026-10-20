package com.slidemd.ui;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Consumer;

/**
 * Card flutuante para seleção de cor do slide (background).
 * Inspirado na paleta padrão de temas visuais do Windows/Office:
 * - Linha 1: 12 cores de destaque vibrantes.
 * - Linha 2: 12 tons de escala de cinza (do branco ao preto).
 * - Grade: Matriz de tonalidades organizadas em 12 colunas harmônicas (do escuro ao claro).
 * Fundo preto puro e sem opções de personalização externa.
 */
public class SlideColorPickerPopup {

    private final Popup popup;
    private final Consumer<String> onColorSelected;

    // 1. Linha de Tons de Cinza (do tom mais alto/escuro ao mais claro)
    private static final List<String> GRAYSCALE = List.of(
            "#000000", "#141414", "#262626", "#383838",
            "#4d4d4d", "#666666", "#808080", "#999999",
            "#b3b3b3", "#cccccc", "#e6e6e6", "#ffffff"
    );

    // 2. Matriz de Cores Temáticas (12 Colunas x 8 Linhas de Tonalidades do tom mais alto ao mais claro)
    private static final String[][] COLOR_MATRIX = {
            // Linha 1 (Tons mais altos / escuros)
            {"#05252b", "#051535", "#0a1033", "#1a0833", "#2b052b", "#330505", "#331505", "#332505", "#283305", "#05330a", "#05331e", "#0d1117"},
            // Linha 2
            {"#093d47", "#092359", "#121c54", "#2d0f54", "#470947", "#540909", "#542309", "#543d09", "#425409", "#095411", "#095432", "#161b22"},
            // Linha 3
            {"#0d5a69", "#0f388a", "#1d2d82", "#471882", "#6e0e6e", "#820f0f", "#82370f", "#825f0f", "#66820f", "#0f821b", "#0f824e", "#21262d"},
            // Linha 4
            {"#127b8f", "#1753c4", "#2b41b8", "#6424b5", "#9c149c", "#b51616", "#b54e16", "#b58516", "#8fb516", "#16b527", "#16b56d", "#30363d"},
            // Linha 5 (Médios / Puros)
            {"#179db8", "#2d6ee6", "#4d65db", "#8644db", "#c728c7", "#db3535", "#db6f35", "#dba935", "#b5db35", "#35db49", "#35db8f", "#484f58"},
            // Linha 6
            {"#4cbcd1", "#5c92f0", "#7a8de5", "#a46fea", "#d957d9", "#e66565", "#e69165", "#e6c165", "#c9e665", "#65e675", "#65e6ab", "#6e7681"},
            // Linha 7
            {"#8ad7e5", "#94b9f7", "#abc0f2", "#c9a9f4", "#e88ee8", "#f09999", "#f0b799", "#f0d899", "#dfef99", "#99f0a5", "#99f0cb", "#8b949e"},
            // Linha 8 (Pastéis / Claros)
            {"#cceef4", "#cfe0fc", "#d6e1fa", "#e7d8fb", "#f6ccf6", "#f9cccc", "#f9ded0", "#f9edcf", "#f2f9cf", "#d0f9d6", "#d0f9e9", "#c9d1d9"}
    };

    private static final double CELL_SIZE = 18.0;

    private Scene registeredScene = null;
    private Window registeredWindow = null;
    private final ChangeListener<Boolean> windowFocusListener;
    private final EventHandler<MouseEvent> outsideClickHandler;

    public SlideColorPickerPopup(Consumer<String> onColorSelected) {
        this.onColorSelected = onColorSelected;
        this.popup = new Popup();
        this.popup.setAutoHide(false);
        this.popup.setHideOnEscape(true);
        this.popup.setOnHidden(e -> cleanupListeners());

        this.windowFocusListener = (obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                hide();
            }
        };

        this.outsideClickHandler = event -> {
            if (!popup.isShowing()) return;

            double x = event.getScreenX();
            double y = event.getScreenY();

            double px = popup.getX();
            double py = popup.getY();
            double pw = popup.getWidth();
            double ph = popup.getHeight();

            // Se o clique foi fora do card de cores
            if (x < px || x > px + pw || y < py || y > py + ph) {
                hide();
                // CRÍTICO: NÃO consome o evento!
                // Deixa o evento propagar normalmente para mover o cursor no editor ou clicar em botões no primeiro clique
            }
        };

        VBox root = new VBox(5);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #121214;" +
                      "-fx-background-radius: 6px;" +
                      "-fx-border-color: #2b2b30;" +
                      "-fx-border-radius: 6px;" +
                      "-fx-border-width: 1px;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 20, 0.3, 0, 4);");

        // 1. Linha de Escala de Cinza (12 quadradinhos perfeitos de 18x18)
        HBox grayRow = new HBox(2.5);
        grayRow.setAlignment(Pos.CENTER);
        for (String hex : GRAYSCALE) {
            grayRow.getChildren().add(createColorCell(hex, CELL_SIZE));
        }

        // 2. Grade Principal de Cores (12 Colunas x 8 Linhas de 18x18)
        GridPane grid = new GridPane();
        grid.setHgap(2.5);
        grid.setVgap(2.5);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < COLOR_MATRIX.length; r++) {
            for (int c = 0; c < COLOR_MATRIX[r].length; c++) {
                String hex = COLOR_MATRIX[r][c];
                grid.add(createColorCell(hex, CELL_SIZE), c, r);
            }
        }

        root.getChildren().addAll(grayRow, grid);
        popup.getContent().add(root);
    }

    private Button createColorCell(String hex, double size) {
        Button btn = new Button();
        btn.setPrefSize(size, size);
        btn.setMinSize(size, size);
        btn.setMaxSize(size, size);

        String baseStyle = "-fx-background-color: " + hex + ";" +
                           "-fx-background-radius: 2.5px;" +
                           "-fx-border-color: rgba(255, 255, 255, 0.12);" +
                           "-fx-border-radius: 2.5px;" +
                           "-fx-border-width: 0.5px;" +
                           "-fx-cursor: hand;" +
                           "-fx-padding: 0;";

        String hoverStyle = "-fx-background-color: " + hex + ";" +
                            "-fx-background-radius: 2.5px;" +
                            "-fx-border-color: #ffffff;" +
                            "-fx-border-radius: 2.5px;" +
                            "-fx-border-width: 1.5px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 0;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        btn.setOnAction(e -> selectColor(hex));

        Tooltip.install(btn, new Tooltip(hex));
        return btn;
    }

    private void selectColor(String hex) {
        hide();
        if (onColorSelected != null) {
            onColorSelected.accept(hex);
        }
    }

    public void show(Window ownerWindow, double screenX, double screenY) {
        if (popup.isShowing()) {
            popup.hide();
        }
        cleanupListeners();

        popup.show(ownerWindow, screenX, screenY);

        if (ownerWindow != null) {
            registeredWindow = ownerWindow;
            ownerWindow.focusedProperty().addListener(windowFocusListener);
            if (ownerWindow.getScene() != null) {
                registeredScene = ownerWindow.getScene();
                registeredScene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
            }
        }
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
        cleanupListeners();
    }

    private void cleanupListeners() {
        if (registeredScene != null) {
            registeredScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
            registeredScene = null;
        }
        if (registeredWindow != null) {
            registeredWindow.focusedProperty().removeListener(windowFocusListener);
            registeredWindow = null;
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }
}

