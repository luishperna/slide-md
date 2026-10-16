package com.slidemd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;

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
                    "bg: ",
                    "bg:",
                    "DIRETIVA",
                    "Define a cor de fundo (#hex, rgb, nome) ou gradiente elegante do slide",
                    "bg: #1e293b",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "\n\n---\n\n# Novo Slide\n",
                    "/slide",
                    "COMANDO",
                    "Insere um novo slide delimitado por '---' com título inicial",
                    "---",
                    Color.web("#c084fc")
            ),
            new SuggestionItem(
                    "align: ",
                    "align:",
                    "DIRETIVA",
                    "Define o alinhamento de texto e conteúdo na tela (center, left ou right)",
                    "align: center",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "transition: ",
                    "transition:",
                    "DIRETIVA",
                    "Efeito de transição de entrada: morph (Transformar), slide, fade ou zoom",
                    "transition: morph",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "video: ",
                    "video:",
                    "DIRETIVA",
                    "Caminho do arquivo de vídeo MP4 com reprodução nativa sem dependências",
                    "video: ./assets/demo.mp4",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "cut: ",
                    "cut:",
                    "DIRETIVA",
                    "Corte virtual de vídeo por tempo (MM:SS - MM:SS) com reprodução instantânea",
                    "cut: 00:05 - 00:25",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "loop: true",
                    "loop:",
                    "DIRETIVA",
                    "Repete a reprodução do vídeo continuamente em loop (true ou false)",
                    "loop: true",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "autoplay: true",
                    "autoplay:",
                    "DIRETIVA",
                    "Inicia a reprodução do vídeo automaticamente ao entrar no slide",
                    "autoplay: true",
                    Color.web("#38bdf8")
            ),
            new SuggestionItem(
                    "```java\n// Código aqui\n```\n",
                    "/code",
                    "BLOCO",
                    "Insere um bloco de código com formatação e destaque sintático",
                    "```java ... ```",
                    Color.web("#fbbf24")
            ),
            new SuggestionItem(
                    "> \"Citação aqui\"\n",
                    "/quote",
                    "BLOCO",
                    "Insere um bloco de citação estilizado (blockquote com barra lateral)",
                    "> citação",
                    Color.web("#34d399")
            )
    );

    // Sugestões de Valores para align:
    public static final List<SuggestionItem> ALIGN_VALUES = List.of(
            new SuggestionItem("center", "center", "VALOR", "Conteúdo centralizado horizontal e verticalmente no slide", "align: center", Color.web("#34d399")),
            new SuggestionItem("left", "left", "VALOR", "Conteúdo alinhado à esquerda com margem elegante", "align: left", Color.web("#34d399")),
            new SuggestionItem("right", "right", "VALOR", "Conteúdo alinhado à margem direita da tela", "align: right", Color.web("#34d399"))
    );

    // Sugestões de Valores para transition:
    public static final List<SuggestionItem> TRANSITION_VALUES = List.of(
            new SuggestionItem("morph", "morph", "VALOR", "Efeito Transformar do PowerPoint com fluidez a 60 FPS", "transition: morph", Color.web("#c084fc")),
            new SuggestionItem("slide", "slide", "VALOR", "Deslizamento cinematográfico lateral acelerado por hardware", "transition: slide", Color.web("#34d399")),
            new SuggestionItem("fade", "fade", "VALOR", "Transição suave de opacidade entre slides (crossfade)", "transition: fade", Color.web("#34d399")),
            new SuggestionItem("zoom", "zoom", "VALOR", "Aproximação sutil de câmera com sensação de profundidade", "transition: zoom", Color.web("#34d399"))
    );

    // Sugestões de Valores Booleanos (loop, autoplay)
    public static final List<SuggestionItem> BOOLEAN_VALUES = List.of(
            new SuggestionItem("true", "true", "VALOR", "Ativa o recurso no slide atual", "true", Color.web("#34d399")),
            new SuggestionItem("false", "false", "VALOR", "Desativa o recurso no slide atual", "false", Color.web("#f87171"))
    );

    public DirectiveAutocompletePopup(Consumer<SuggestionItem> onItemSelected) {
        this.onItemSelected = onItemSelected;
        this.popup = new Popup();
        this.popup.setAutoHide(true);
        this.popup.setHideOnEscape(true);

        VBox root = new VBox();
        root.setPrefWidth(470);
        root.setMaxWidth(470);
        root.setMaxHeight(320);
        root.setStyle("-fx-background-color: #1e1e1e;" +
                      "-fx-background-radius: 6px;" +
                      "-fx-border-color: #3c3c3c;" +
                      "-fx-border-radius: 6px;" +
                      "-fx-border-width: 1px;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.75), 18, 0.3, 0, 4);");

        // Cabeçalho estilo VS Code
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(6, 12, 6, 12));
        header.setStyle("-fx-background-color: #252526;" +
                        "-fx-border-color: transparent transparent #2d2d2d transparent;" +
                        "-fx-border-width: 1px;" +
                        "-fx-background-radius: 6px 6px 0 0;");

        Label headerTitle = new Label("Sugestões");
        headerTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        headerTitle.setTextFill(Color.web("#cccccc"));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label headerHints = new Label("↑↓ Navegar  •  Enter / Tab Inserir  •  Esc Fechar");
        headerHints.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        headerHints.setTextFill(Color.web("#858585"));

        header.getChildren().addAll(headerTitle, headerSpacer, headerHints);

        // Container de Itens com ScrollPane
        itemListContainer = new VBox(1);
        itemListContainer.setPadding(new Insets(4, 4, 4, 4));

        ScrollPane scrollPane = new ScrollPane(itemListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxHeight(270);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: transparent; -fx-padding: 0;");

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

            VBox rowContainer = new VBox(3);
            rowContainer.setPadding(new Insets(6, 10, 6, 10));
            rowContainer.setStyle("-fx-background-radius: 4px; -fx-cursor: hand;");

            // Linha Superior: Badge + Nome + Exemplo
            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(item.typeBadge());
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            String badgeHex = toHex(item.badgeColor());
            badge.setStyle("-fx-text-fill: " + badgeHex + ";" +
                           "-fx-background-color: rgba(255, 255, 255, 0.08);" +
                           "-fx-background-radius: 3px;" +
                           "-fx-padding: 1 5 1 5;");

            Label name = new Label(item.displayText());
            name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            name.setTextFill(Color.web("#ffffff"));

            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);

            Label example = new Label(item.exampleText());
            example.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            example.setTextFill(Color.web("#858585"));

            topRow.getChildren().addAll(badge, name, topSpacer, example);

            // Linha Inferior: Descrição Completa Sem Abreviações (WrapText)
            Label desc = new Label(item.description());
            desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11.5));
            desc.setTextFill(Color.web("#b3b3b3"));
            desc.setWrapText(true);
            desc.setMaxWidth(440);

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
                node.setStyle("-fx-background-color: #04395e;" +
                              "-fx-border-color: transparent transparent transparent #007acc;" +
                              "-fx-border-width: 0 0 0 3px;" +
                              "-fx-background-radius: 0 4px 4px 0;" +
                              "-fx-cursor: hand;");
            } else {
                node.setStyle("-fx-background-color: transparent;" +
                              "-fx-border-color: transparent;" +
                              "-fx-border-width: 0 0 0 3px;" +
                              "-fx-background-radius: 4px;" +
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

    public void show(Stage ownerStage, double screenX, double screenY) {
        if (popup.isShowing()) {
            popup.hide();
        }
        if (!itemListContainer.getChildren().isEmpty()) {
            popup.show(ownerStage, screenX, screenY);
        }
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public int getItemCount() {
        return currentItems.size();
    }
}
