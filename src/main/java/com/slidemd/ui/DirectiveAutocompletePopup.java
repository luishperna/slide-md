package com.slidemd.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * Card flutuante para autocompletar diretivas de slides e valores suportados.
 */
public class DirectiveAutocompletePopup {

    public record SuggestionItem(String insertText, String displayText, String description) {}

    private final Popup popup;
    private final VBox itemListContainer;
    private final Consumer<SuggestionItem> onItemSelected;

    public static final List<SuggestionItem> DIRECTIVE_SUGGESTIONS = List.of(
            new SuggestionItem("bg: ", "bg:", "Cor sólida (#hex, rgb), gradiente ou imagem de fundo"),
            new SuggestionItem("align: ", "align:", "Alinhamento do conteúdo (center, left, right)"),
            new SuggestionItem("transition: ", "transition:", "Efeito de transição (slide, fade, zoom)"),
            new SuggestionItem("video: ", "video:", "Caminho do arquivo de vídeo (.mp4)"),
            new SuggestionItem("cut: ", "cut:", "Intervalo de corte no formato MM:SS - MM:SS"),
            new SuggestionItem("loop: true", "loop:", "Repetir vídeo automaticamente (true / false)"),
            new SuggestionItem("autoplay: true", "autoplay:", "Iniciar reprodução de vídeo (true / false)")
    );

    public static final List<SuggestionItem> ALIGN_VALUES = List.of(
            new SuggestionItem("center", "center", "Conteúdo centralizado no slide"),
            new SuggestionItem("left", "left", "Conteúdo alinhado à esquerda"),
            new SuggestionItem("right", "right", "Conteúdo alinhado à direita")
    );

    public static final List<SuggestionItem> TRANSITION_VALUES = List.of(
            new SuggestionItem("slide", "slide", "Deslizamento cinematográfico lateral"),
            new SuggestionItem("fade", "fade", "Transição suave de opacidade (crossfade)"),
            new SuggestionItem("zoom", "zoom", "Aproximação sutil com profundidade")
    );

    public static final List<SuggestionItem> BOOLEAN_VALUES = List.of(
            new SuggestionItem("true", "true", "Ativado"),
            new SuggestionItem("false", "false", "Desativado")
    );

    public DirectiveAutocompletePopup(Consumer<SuggestionItem> onItemSelected) {
        this.onItemSelected = onItemSelected;
        this.popup = new Popup();
        this.popup.setAutoHide(true);
        this.popup.setHideOnEscape(true);

        VBox root = new VBox(4);
        root.setPadding(new Insets(6, 6, 6, 6));
        root.setPrefWidth(280);
        root.setStyle("-fx-background-color: #181818;" +
                      "-fx-background-radius: 8px;" +
                      "-fx-border-color: #2a2a2a;" +
                      "-fx-border-radius: 8px;" +
                      "-fx-border-width: 1px;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 18, 0.25, 0, 4);");

        itemListContainer = new VBox(2);
        root.getChildren().add(itemListContainer);
        popup.getContent().add(root);
    }

    public void setSuggestions(List<SuggestionItem> items) {
        itemListContainer.getChildren().clear();
        if (items == null || items.isEmpty()) {
            return;
        }

        for (SuggestionItem item : items) {
            Button btn = new Button();
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(5, 8, 5, 8));

            Label name = new Label(item.displayText());
            name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11.5));
            name.setTextFill(Color.web("#38bdf8"));

            Label desc = new Label(item.description());
            desc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10.5));
            desc.setTextFill(Color.web("#94a3b8"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(8, name, spacer, desc);
            row.setAlignment(Pos.CENTER_LEFT);
            btn.setGraphic(row);

            String base = "-fx-background-color: transparent; -fx-background-radius: 4px; -fx-cursor: hand;";
            String hover = "-fx-background-color: #262626; -fx-background-radius: 4px; -fx-cursor: hand;";

            btn.setStyle(base);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e -> btn.setStyle(base));
            btn.setOnAction(e -> {
                popup.hide();
                if (onItemSelected != null) {
                    onItemSelected.accept(item);
                }
            });

            itemListContainer.getChildren().add(btn);
        }
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
}
