package com.slidemd.ui;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Card flutuante estilo VS Code para autocompletar comandos slash (/),
 * diretivas e valores com documentação completa sem abreviações.
 */
public class DirectiveAutocompletePopup {

    public record SuggestionItem(
            String insertText,
            String displayText,
            String typeBadge,
            String description,
            String exampleText,
            Color badgeColor
    ) {}

    private final Popup popup;
    private final VBox itemListContainer;
    private final Consumer<SuggestionItem> onItemSelected;
    private final List<SuggestionItem> currentItems = new ArrayList<>();
    private final List<VBox> itemNodes = new ArrayList<>();
    private int selectedIndex = 0;

    // Sugestões de Diretivas e Comandos Slash (/)
    public static final List<SuggestionItem> ALL_COMMANDS_AND_DIRECTIVES = List.of(
            new SuggestionItem(
                    "background: ",
                    "background:",
                    "DIRETIVA",
                    "Gradiente [#1e293b, #0f172a, 45%], cor ou imagem",
                    "background: [#1e293b, #0f172a, 45%]",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "\n\n---\n\n# Novo Slide\n",
                    "/slide",
                    "COMANDO",
                    "Novo slide delimitado por '---'",
                    "---",
                    Color.web("#c084fc")
            ),
            new SuggestionItem(
                    "alignment: ",
                    "alignment:",
                    "DIRETIVA",
                    "Alinhamento do conteúdo: center, left, right, top ou down",
                    "alignment: center",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "transition: ",
                    "transition:",
                    "DIRETIVA",
                    "Efeito de transição: morph, slide, fade ou zoom",
                    "transition: morph",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "video: ",
                    "video:",
                    "DIRETIVA",
                    "Vídeo MP4 com corte inline [...] e junção por vírgula",
                    "video: ./demo.mp4 [00:10-00:45]",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "sound: ",
                    "sound:",
                    "DIRETIVA",
                    "Áudio de fundo contínuo (MP3, WAV, AAC)",
                    "sound: ./assets/theme.mp3",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "image: ",
                    "image:",
                    "DIRETIVA",
                    "Imagem do slide ou inline (JPG, PNG, WebP)",
                    "image: ./assets/foto.jpg",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "color: ",
                    "color:",
                    "DIRETIVA",
                    "Cor temática dos títulos e marcadores (#ef4444, #38bdf8, etc)",
                    "color: #ef4444",
                    Color.web("#60a5fa")
            ),
            new SuggestionItem(
                    "```java\n// Código aqui\n```\n",
                    "/code",
                    "BLOCO",
                    "Bloco de código com destaque sintático",
                    "```java ... ```",
                    Color.web("#fbbf24")
            ),
            new SuggestionItem(
                    "> \"Citação aqui\"\n",
                    "/quote",
                    "BLOCO",
                    "Bloco de citação estilizado em destaque",
                    "> citação",
                    Color.web("#34d399")
            )
    );

    // Sugestões de Valores para alignment:
    public static final List<SuggestionItem> ALIGN_VALUES = List.of(
            new SuggestionItem("center", "center", "VALOR", "Conteúdo centralizado no slide", "alignment: center", Color.web("#34d399")),
            new SuggestionItem("left", "left", "VALOR", "Conteúdo alinhado à esquerda", "alignment: left", Color.web("#34d399")),
            new SuggestionItem("right", "right", "VALOR", "Conteúdo alinhado à direita", "alignment: right", Color.web("#34d399")),
            new SuggestionItem("top", "top", "VALOR", "Conteúdo alinhado no topo", "alignment: top", Color.web("#34d399")),
            new SuggestionItem("down", "down", "VALOR", "Conteúdo alinhado na base", "alignment: down", Color.web("#34d399"))
    );

    // Sugestões de Valores para transition:
    public static final List<SuggestionItem> TRANSITION_VALUES = List.of(
            new SuggestionItem("morph", "morph", "VALOR", "Efeito Transformar fluido a 60 FPS", "transition: morph", Color.web("#34d399")),
            new SuggestionItem("slide", "slide", "VALOR", "Deslizamento cinematográfico suave", "transition: slide", Color.web("#34d399")),
            new SuggestionItem("fade", "fade", "VALOR", "Dissolvência suave entre slides", "transition: fade", Color.web("#34d399")),
            new SuggestionItem("zoom", "zoom", "VALOR", "Aproximação sutil de câmera", "transition: zoom", Color.web("#34d399"))
    );

    private Scene registeredScene = null;
    private Window registeredWindow = null;
    private final ChangeListener<Boolean> windowFocusListener;
    private final EventHandler<MouseEvent> outsideClickHandler;

    public DirectiveAutocompletePopup(Consumer<SuggestionItem> onItemSelected) {
        this.onItemSelected = onItemSelected;
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

            // Se o clique foi fora do popup
            if (x < px || x > px + pw || y < py || y > py + ph) {
                hide();
                // CRÍTICO: NÃO consome o evento! Deixa fluir para mover o cursor no editor
            }
        };

        VBox root = new VBox();
        root.setPrefWidth(380);
        root.setMaxWidth(380);
        root.setMaxHeight(260);
        root.setStyle("-fx-background-color: #161618;" +
                      "-fx-background-radius: 8px;" +
                      "-fx-border-color: #27272a;" +
                      "-fx-border-radius: 8px;" +
                      "-fx-border-width: 1px;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.65), 18, 0.25, 0, 6);");

        // Carrega CSS de scrollbar do editor no popup
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                try {
                    var cssUrl = getClass().getResource("/css/markdown-editor.css");
                    if (cssUrl != null) {
                        newScene.getStylesheets().add(cssUrl.toExternalForm());
                    }
                } catch (Exception ignored) {}
            }
        });

        // Cabeçalho Minimalista
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(26);
        header.setMinHeight(26);
        header.setPadding(new Insets(0, 10, 0, 10));
        header.setStyle("-fx-background-color: #1c1c20;" +
                        "-fx-border-color: transparent transparent #26262a transparent;" +
                        "-fx-border-width: 1px;" +
                        "-fx-background-radius: 8px 8px 0 0;");

        Label headerTitle = new Label("SUGESTÕES");
        headerTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        headerTitle.setTextFill(Color.web("#71717a"));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label headerHints = new Label("↵ ou Tab inserir  •  Esc fechar");
        headerHints.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9.5));
        headerHints.setTextFill(Color.web("#52525b"));

        header.getChildren().addAll(headerTitle, headerSpacer, headerHints);

        // Container de Itens com ScrollPane
        itemListContainer = new VBox(2);
        itemListContainer.setPadding(new Insets(3, 3, 3, 3));

        ScrollPane scrollPane = new ScrollPane(itemListContainer);
        scrollPane.getStyleClass().add("autocomplete-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(230);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

        root.getChildren().addAll(header, scrollPane);
        popup.getContent().add(root);
    }

    public void setSuggestions(List<SuggestionItem> items) {
        currentItems.clear();
        itemNodes.clear();
        itemListContainer.getChildren().clear();
        selectedIndex = 0;

        if (items == null || items.isEmpty()) {
            return;
        }

        currentItems.addAll(items);

        for (int i = 0; i < currentItems.size(); i++) {
            final int index = i;
            SuggestionItem item = currentItems.get(i);

            VBox rowContainer = new VBox(2);
            rowContainer.setPadding(new Insets(5, 8, 5, 8));
            rowContainer.setStyle("-fx-background-radius: 5px; -fx-cursor: hand;");

            // Linha Superior: Badge + Nome + Exemplo
            HBox topRow = new HBox(6);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(item.typeBadge());
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8.5));
            String badgeHex = toHex(item.badgeColor());
            badge.setStyle("-fx-text-fill: " + badgeHex + ";" +
                           "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                           "-fx-background-radius: 3px;" +
                           "-fx-padding: 1 4 1 4;");

            Label name = new Label(item.displayText());
            name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            name.setTextFill(Color.web("#f4f4f5"));

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label example = new Label(item.exampleText());
            example.setFont(Font.font("Consolas", FontWeight.NORMAL, 10.5));
            example.setTextFill(Color.web("#71717a"));

            topRow.getChildren().addAll(badge, name, topSpacer, example);

            // Linha Inferior: Descrição Objetiva
            Label desc = new Label(item.description());
            desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
            desc.setTextFill(Color.web("#a1a1aa"));
            desc.setMaxWidth(360);

            rowContainer.getChildren().addAll(topRow, desc);

            rowContainer.setOnMouseEntered(e -> {
                selectedIndex = index;
                updateSelectionHighlight();
            });

            rowContainer.setOnMouseClicked(e -> commitSelected());

            itemNodes.add(rowContainer);
            itemListContainer.getChildren().add(rowContainer);
        }

        updateSelectionHighlight();
    }

    public void selectNext() {
        if (currentItems.isEmpty()) return;
        selectedIndex = (selectedIndex + 1) % currentItems.size();
        updateSelectionHighlight();
    }

    public void selectPrevious() {
        if (currentItems.isEmpty()) return;
        selectedIndex = (selectedIndex - 1 + currentItems.size()) % currentItems.size();
        updateSelectionHighlight();
    }

    public void commitSelected() {
        if (currentItems.isEmpty()) return;
        if (selectedIndex >= 0 && selectedIndex < currentItems.size()) {
            SuggestionItem selected = currentItems.get(selectedIndex);
            popup.hide();
            if (onItemSelected != null) {
                onItemSelected.accept(selected);
            }
        }
    }

    private void updateSelectionHighlight() {
        for (int i = 0; i < itemNodes.size(); i++) {
            VBox node = itemNodes.get(i);
            if (i == selectedIndex) {
                node.setStyle("-fx-background-color: #27272a;" +
                              "-fx-border-color: transparent transparent transparent #3b82f6;" +
                              "-fx-border-width: 0 0 0 3px;" +
                              "-fx-background-radius: 0 5px 5px 0;" +
                              "-fx-cursor: hand;");
            } else {
                node.setStyle("-fx-background-color: transparent;" +
                              "-fx-border-color: transparent;" +
                              "-fx-border-width: 0 0 0 3px;" +
                              "-fx-background-radius: 5px;" +
                              "-fx-cursor: hand;");
            }
        }
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    public void show(javafx.stage.Window ownerWindow, double screenX, double screenY) {
        if (popup.isShowing()) {
            popup.hide();
        }
        cleanupListeners();

        if (!itemListContainer.getChildren().isEmpty()) {
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

    public int getItemCount() {
        return currentItems.size();
    }
}
