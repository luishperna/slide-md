package com.slidemd.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Painel lateral de edição Markdown integrado na mesma janela principal (estilo sidebar do VS Code).
 * Oferece sincronização bidirecional em tempo real com aceleração por hardware,
 * autocompletar contextual e paleta inteligente de cores.
 */
public class SideEditorPanel {

    private final BorderPane panelNode;
    private final CodeArea codeArea;
    private final Label statusLabel;
    private final Label currentSlideIndicator;

    private final Consumer<String> onContentChanged;
    private final Runnable onSave;
    private final Consumer<Integer> onSlideNavigated;
    private final Runnable onCloseRequest;

    private final PauseTransition debounceTimer;
    private final PauseTransition highlightTimer;
    private final PauseTransition assistTimer;

    private final SlideColorPickerPopup colorPickerPopup;
    private final DirectiveAutocompletePopup autocompletePopup;

    private boolean isProgrammaticChange = false;
    private boolean isNavigatingProgrammatically = false;
    private int lastNavigatedSlideIndex = -1;

    private static final Pattern PATTERN = Pattern.compile(
            "(?<DIVIDER>^[ \\t]*---[ \\t]*$)"
            + "|(?<COMMENT><!--[\\s\\S]*?-->)"
            + "|(?<DIRECTIVE>^(?<DIRKEY>[ \\t]*(?:background|alignment|transition|video|sound|image|color|slide\\s+\\d+):)(?<DIRVAL>[^\\n]*))"
            + "|(?<HEADER>^[ \\t]*#{1,6}[ \\t]+.*$)"
            + "|(?<QUOTE>^[ \\t]*>.*$)"
            + "|(?<LIST>^[ \\t]*([-*+]|\\d+\\.)[ \\t]+)"
            + "|(?<CODE>`[^`\\n]+`|```[\\s\\S]*?```)"
            + "|(?<BOLD>\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__)"
            + "|(?<ITALIC>(?<!\\*)\\*[^*\\n\\r]+\\*(?!\\*)|(?<!_)_[^_\\n\\r]+_(?!_))",
            Pattern.MULTILINE
    );

    public SideEditorPanel(Consumer<String> onContentChanged, Runnable onSave,
                           Consumer<Integer> onSlideNavigated, Runnable onCloseRequest) {
        this.onContentChanged = onContentChanged;
        this.onSave = onSave;
        this.onSlideNavigated = onSlideNavigated;
        this.onCloseRequest = onCloseRequest;

        this.panelNode = new BorderPane();
        panelNode.getStyleClass().add("side-editor-panel");
        panelNode.setStyle("-fx-background-color: #141416;");
        panelNode.setMinSize(0, 0);
        panelNode.setPrefWidth(460);

        // CodeArea setup com números de linha
        this.codeArea = new CodeArea();
        codeArea.setMinSize(0, 0);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        // Carrega CSS de realce sintático Markdown
        try {
            var cssUrl = getClass().getResource("/css/markdown-editor.css");
            if (cssUrl != null) {
                codeArea.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}

        // Timer de debounce para Live Preview em tempo real
        debounceTimer = new PauseTransition(Duration.millis(250));
        debounceTimer.setOnFinished(e -> {
            if (!isProgrammaticChange && this.onContentChanged != null) {
                this.onContentChanged.accept(codeArea.getText());
            }
        });

        // Timer para destaque sintático com baixa latência
        highlightTimer = new PauseTransition(Duration.millis(40));
        highlightTimer.setOnFinished(e -> {
            Platform.runLater(() -> {
                try {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                } catch (Exception ignored) {}
            });
        });

        // Timer de assistência contextual
        assistTimer = new PauseTransition(Duration.millis(120));
        assistTimer.setOnFinished(e -> checkContextualAssistance());

        // Popups inteligentes de cor e autocomplete
        colorPickerPopup = new SlideColorPickerPopup(this::applyColorToEditor);
        autocompletePopup = new DirectiveAutocompletePopup(this::applySuggestionToEditor);

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            updateStats();
            if (!isProgrammaticChange) {
                debounceTimer.playFromStart();
                highlightTimer.playFromStart();
                assistTimer.playFromStart();
            }
        });

        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            updateStats();
            checkSlideNavigated();
            if (!isProgrammaticChange) {
                assistTimer.playFromStart();
            }
        });

        codeArea.setOnMouseClicked(event -> {
            if (!isProgrammaticChange) {
                checkContextualAssistance();
            }
        });

        // Tratamento de teclas dentro do editor
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (autocompletePopup.isShowing()) {
                if (event.getCode() == KeyCode.DOWN) {
                    autocompletePopup.selectNext();
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.UP) {
                    autocompletePopup.selectPrevious();
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                    autocompletePopup.commitSelected();
                    event.consume();
                    return;
                }
            }

            if (event.getCode() == KeyCode.SPACE && event.isControlDown()) {
                openDirectivesManual();
                event.consume();
                return;
            }

            if (event.getCode() == KeyCode.ESCAPE) {
                if (colorPickerPopup.isShowing()) {
                    colorPickerPopup.hide();
                    event.consume();
                    return;
                }
                if (autocompletePopup.isShowing()) {
                    autocompletePopup.hide();
                    event.consume();
                    return;
                }
                if (onCloseRequest != null) {
                    onCloseRequest.run();
                    event.consume();
                    return;
                }
            }
        });

        // 1. Cabeçalho Lateral Estilo VS Code Sidebar (#161618)
        HBox topHeader = new HBox(6);
        topHeader.setAlignment(Pos.CENTER_LEFT);
        topHeader.setPrefHeight(38);
        topHeader.setMinHeight(38);
        topHeader.setMaxHeight(38);
        topHeader.setPadding(new Insets(0, 8, 0, 12));
        topHeader.setStyle("-fx-background-color: #161618; -fx-border-color: transparent transparent #262626 transparent; -fx-border-width: 1px;");

        Label sidebarTitle = new Label("EDITOR MARKDOWN");
        sidebarTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        sidebarTitle.setTextFill(Color.web("#cbd5e1"));

        currentSlideIndicator = new Label("");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button addSlideBtn = createIconButton(createAddSlideIcon(), "Slide", "Inserir novo slide delimitado por '---' após o atual", this::addNewSlide);
        Button removeSlideBtn = createIconButton(createRemoveSlideIcon(), "Slide", "Remover o slide onde o cursor está posicionado", this::removeCurrentSlide);
        Button directivesBtn = createIconButton(createDirectivesIcon(), "Diretivas", "Inserir diretivas suportadas (transition, video, sound...)", this::openDirectivesManual);
        Button saveBtn = createIconButton(createSaveIcon(), "Salvar", "Salvar apresentação no disco (Ctrl+S)", () -> {
            if (this.onSave != null) this.onSave.run();
        });

        Button closeBtn = new Button();
        SVGPath arrowIcon = createCollapseArrowIcon();
        closeBtn.setGraphic(arrowIcon);
        closeBtn.setPrefWidth(28);
        closeBtn.setPrefHeight(26);
        closeBtn.setAlignment(Pos.CENTER);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 4px;");
        closeBtn.setOnMouseEntered(e -> {
            closeBtn.setStyle("-fx-background-color: #262626; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 4px;");
            arrowIcon.setStroke(Color.WHITE);
        });
        closeBtn.setOnMouseExited(e -> {
            closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 4px;");
            arrowIcon.setStroke(Color.web("#a1a1aa"));
        });
        closeBtn.setOnAction(e -> {
            if (this.onCloseRequest != null) this.onCloseRequest.run();
        });
        Tooltip.install(closeBtn, new Tooltip("Minimizar editor (Esc)"));

        topHeader.getChildren().addAll(sidebarTitle, headerSpacer, addSlideBtn, removeSlideBtn, directivesBtn, saveBtn, closeBtn);
        panelNode.setTop(topHeader);

        // 3. ScrollPane com CodeArea (#121316)
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle("-fx-background-color: #121316;");
        scrollPane.setMinHeight(0);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        panelNode.setCenter(scrollPane);

        // 4. Rodapé (#161618)
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMinHeight(28);
        footer.setPrefHeight(28);
        footer.setMaxHeight(28);
        footer.setPadding(new Insets(4, 12, 5, 12));
        footer.setStyle("-fx-background-color: #161618; -fx-border-color: #262626 transparent transparent transparent; -fx-border-width: 1px;");

        statusLabel = new Label("0 slides");
        statusLabel.setFont(Font.font("Segoe UI", 11));
        statusLabel.setTextFill(Color.web("#94a3b8"));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label hintsLabel = new Label("Ctrl+Space: Completar • Esc: Fechar");
        hintsLabel.setFont(Font.font("Segoe UI", 10.5));
        hintsLabel.setTextFill(Color.web("#64748b"));

        footer.getChildren().addAll(statusLabel, footerSpacer, hintsLabel);
        panelNode.setBottom(footer);
    }

    private Button createIconButton(Node icon, String text, String tooltipText, Runnable action) {
        Button btn = new Button();
        if (icon != null && text != null && !text.isEmpty()) {
            btn.setGraphic(icon);
            btn.setText(text);
            btn.setGraphicTextGap(5);
        } else if (icon != null) {
            btn.setGraphic(icon);
        } else {
            btn.setText(text);
        }
        btn.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 11));

        String normalStyle = "-fx-background-color: #202024; -fx-text-fill: #e4e4e7; -fx-cursor: hand; "
                + "-fx-background-radius: 5px; -fx-padding: 3 8 3 8; -fx-border-color: #2f2f35; "
                + "-fx-border-radius: 5px; -fx-border-width: 1px;";
        String hoverStyle = "-fx-background-color: #2c2c34; -fx-text-fill: #ffffff; -fx-cursor: hand; "
                + "-fx-background-radius: 5px; -fx-padding: 3 8 3 8; -fx-border-color: #52525b; "
                + "-fx-border-radius: 5px; -fx-border-width: 1px;";

        btn.setStyle(normalStyle);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            if (icon instanceof SVGPath svg) {
                svg.setStroke(Color.WHITE);
            }
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(normalStyle);
            if (icon instanceof SVGPath svg) {
                Object origColor = svg.getUserData();
                if (origColor instanceof Color c) {
                    svg.setStroke(c);
                } else {
                    svg.setStroke(Color.web("#a1a1aa"));
                }
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

    private SVGPath createAddSlideIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 1.5 6.5 L 11.5 6.5 M 6.5 1.5 L 6.5 11.5");
        path.setStroke(Color.WHITE);
        path.setUserData(Color.WHITE);
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createRemoveSlideIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 1.5 6.5 L 11.5 6.5");
        path.setStroke(Color.WHITE);
        path.setUserData(Color.WHITE);
        path.setStrokeWidth(1.8);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createDirectivesIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 6.5 1 L 2 7.5 L 5.5 7.5 L 4.5 13 L 10 5.5 L 6.5 5.5 Z");
        path.setStroke(Color.WHITE);
        path.setUserData(Color.WHITE);
        path.setStrokeWidth(1.15);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setFill(Color.web("rgba(255, 255, 255, 0.15)"));
        return path;
    }

    private SVGPath createSaveIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 2 2 L 9.5 2 L 12 4.5 L 12 12.5 L 2 12.5 Z M 4 2 L 4 5 L 8 5 L 8 2 M 4 8 L 10 8 L 10 12.5 L 4 12.5 Z");
        path.setStroke(Color.WHITE);
        path.setUserData(Color.WHITE);
        path.setStrokeWidth(1.15);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createCollapseArrowIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 3.5 2 L 7.5 6 L 3.5 10");
        path.setStroke(Color.web("#a1a1aa"));
        path.setStrokeWidth(1.5);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    public BorderPane getPanelNode() {
        return panelNode;
    }

    public boolean isFocused() {
        return codeArea.isFocused();
    }

    public void onOpened(int initialSlideIndex) {
        updateStats();
        Platform.runLater(() -> {
            try {
                codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
            } catch (Exception ignored) {}
            codeArea.requestFocus();
            if (initialSlideIndex >= 0) {
                scrollToSlide(initialSlideIndex);
            }
            // Segunda passada encadeada no pulso seguinte para sincronização perfeita após layout do VirtualizedScrollPane
            Platform.runLater(() -> {
                if (initialSlideIndex >= 0) {
                    scrollToSlide(initialSlideIndex);
                }
            });
        });
    }

    public void onClosed() {
        colorPickerPopup.hide();
        autocompletePopup.hide();
    }

    private void addNewSlide() {
        int caret = codeArea.getCaretPosition();
        String currentText = codeArea.getText();
        String updated = SlideEditorHelper.insertSlideAfterCaret(currentText, caret);
        codeArea.replaceText(updated);
        codeArea.requestFocus();
        if (onContentChanged != null) {
            onContentChanged.accept(updated);
        }
    }

    private void removeCurrentSlide() {
        int caret = codeArea.getCaretPosition();
        String currentText = codeArea.getText();
        String updated = SlideEditorHelper.removeSlideAtCaret(currentText, caret);
        codeArea.replaceText(updated);
        codeArea.requestFocus();
        if (onContentChanged != null) {
            onContentChanged.accept(updated);
        }
    }

    private void openDirectivesManual() {
        int currentParagraph = codeArea.getCurrentParagraph();
        String lineText = codeArea.getParagraph(currentParagraph).getText();
        int col = codeArea.getCaretColumn();
        String textBeforeCaret = lineText.substring(0, Math.min(col, lineText.length()));
        int tokenStart = Math.max(textBeforeCaret.lastIndexOf(' '), textBeforeCaret.lastIndexOf('\t')) + 1;
        String token = textBeforeCaret.substring(tokenStart).trim();

        if (!token.isEmpty()) {
            filterAndShowSuggestions(DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES, token);
        } else {
            showAutocompleteAtCaret(DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES);
        }
    }

    private void checkContextualAssistance() {
        if (!panelNode.isVisible()) return;

        try {
            int currentParagraph = codeArea.getCurrentParagraph();
            String lineText = codeArea.getParagraph(currentParagraph).getText();
            int col = codeArea.getCaretColumn();
            String textBeforeCaret = lineText.substring(0, Math.min(col, lineText.length()));

            int tokenStart = Math.max(textBeforeCaret.lastIndexOf(' '), textBeforeCaret.lastIndexOf('\t')) + 1;
            String token = textBeforeCaret.substring(tokenStart).trim();

            // 1. Se a linha já contém diretiva com ':', verifica se o cursor está APÓS o ':'
            int colonIdx = lineText.indexOf(':');
            if (colonIdx != -1) {
                String dirName = lineText.substring(0, colonIdx).trim().toLowerCase();
                // O card de cores deve aparecer em background: e color:, e SOMENTE se o cursor/mouse estiver após o ':'
                if (dirName.equals("background") || dirName.equals("bg") || dirName.equals("color")) {
                    if (col > colonIdx) {
                        autocompletePopup.hide();
                        showColorPickerAtCaret();
                        return;
                    } else {
                        colorPickerPopup.hide();
                    }
                } else if (col > colonIdx) {
                    colorPickerPopup.hide();
                    String valQuery = textBeforeCaret.substring(colonIdx + 1).trim().toLowerCase();
                    if (dirName.equals("alignment")) {
                        filterAndShowSuggestions(DirectiveAutocompletePopup.ALIGN_VALUES, valQuery);
                        return;
                    } else if (dirName.equals("transition")) {
                        filterAndShowSuggestions(DirectiveAutocompletePopup.TRANSITION_VALUES, valQuery);
                        return;
                    }
                }
            } else {
                colorPickerPopup.hide();
            }

            // 2. Comandos slash estilo VS Code / Notion (iniciados por '/')
            if (token.startsWith("/")) {
                colorPickerPopup.hide();
                String slashQuery = token.substring(1).toLowerCase().trim();
                filterAndShowSuggestions(DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES, slashQuery);
                return;
            }

            // 3. Digitação de diretivas no início da linha (ex: "backg", "col", "ali", "tran", "vid", "sou")
            if (!token.isEmpty() && (colonIdx == -1 || col <= colonIdx)) {
                colorPickerPopup.hide();
                filterAndShowSuggestions(DirectiveAutocompletePopup.ALL_COMMANDS_AND_DIRECTIVES, token);
                return;
            }

            // 4. Fechar popups se não há gatilho
            colorPickerPopup.hide();
            autocompletePopup.hide();

        } catch (Exception ignored) {}
    }

    private void filterAndShowSuggestions(java.util.List<DirectiveAutocompletePopup.SuggestionItem> list, String query) {
        if (query == null) query = "";
        String q = query.toLowerCase().trim();
        if (q.startsWith("/")) q = q.substring(1);
        if (q.endsWith(":")) q = q.substring(0, q.length() - 1);
        final String cleanQ = q;

        java.util.List<DirectiveAutocompletePopup.SuggestionItem> filtered = list.stream()
                .filter(it -> {
                    if (cleanQ.isEmpty()) return true;
                    String disp = it.displayText().toLowerCase();
                    String ins = it.insertText().toLowerCase();
                    String desc = it.description().toLowerCase();
                    return disp.contains(cleanQ) || ins.contains(cleanQ) || desc.contains(cleanQ);
                })
                .sorted((a, b) -> {
                    if (cleanQ.isEmpty()) return 0;
                    String aClean = a.displayText().toLowerCase().replace(":", "").trim();
                    String bClean = b.displayText().toLowerCase().replace(":", "").trim();
                    boolean aStarts = aClean.startsWith(cleanQ);
                    boolean bStarts = bClean.startsWith(cleanQ);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return 0;
                })
                .toList();

        if (!filtered.isEmpty()) {
            showAutocompleteAtCaret(filtered);
        } else {
            autocompletePopup.hide();
        }
    }

    private void showColorPickerAtCaret() {
        Window window = getWindow();
        if (window == null) return;
        double[] coords = getColorPickerCoordinates();
        colorPickerPopup.show(window, coords[0], coords[1]);
    }

    private double[] getColorPickerCoordinates() {
        Window window = getWindow();
        double screenX = window != null ? window.getX() + 200 : 200;
        double screenY = window != null ? window.getY() + 150 : 150;

        try {
            Optional<Bounds> optBounds = codeArea.getCharacterBoundsOnScreen(codeArea.getCaretPosition(), codeArea.getCaretPosition());
            double caretY = optBounds.map(Bounds::getMinY).orElse(screenY);

            Bounds panelBounds = panelNode.localToScreen(panelNode.getBoundsInLocal());
            if (panelBounds != null) {
                double editorLeft = panelBounds.getMinX();
                double cardWidth = 265.0;

                // Se houver espaço à esquerda do painel lateral (na área da apresentação):
                // abre como um flyout lateral flutuante, deixando o editor 100% visível e livre para cliques!
                if (editorLeft - cardWidth - 10 > 10) {
                    screenX = editorLeft - cardWidth - 10;
                    screenY = caretY - 10;
                } else {
                    // Fallback se a janela estiver muito compacta
                    if (optBounds.isPresent()) {
                        Bounds b = optBounds.get();
                        screenX = b.getMinX();
                        screenY = b.getMaxY() + 6;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Garante que o card cabe perfeitamente na tela visível
        try {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            if (screenX < bounds.getMinX() + 10) {
                screenX = bounds.getMinX() + 10;
            }
            if (screenX + 270 > bounds.getMaxX()) {
                screenX = bounds.getMaxX() - 280;
            }
            if (screenY + 230 > bounds.getMaxY()) {
                screenY = bounds.getMaxY() - 240;
            }
            if (screenY < bounds.getMinY() + 10) {
                screenY = bounds.getMinY() + 10;
            }
        } catch (Exception ignored) {}

        return new double[] { screenX, screenY };
    }

    private void showAutocompleteAtCaret(java.util.List<DirectiveAutocompletePopup.SuggestionItem> items) {
        Window window = getWindow();
        if (window == null) return;
        autocompletePopup.setSuggestions(items);
        double[] coords = getCaretScreenCoordinates();
        autocompletePopup.show(window, coords[0], coords[1]);
    }

    private Window getWindow() {
        return panelNode.getScene() != null ? panelNode.getScene().getWindow() : null;
    }

    private double[] getCaretScreenCoordinates() {
        Window window = getWindow();
        double screenX = window != null ? window.getX() + 200 : 200;
        double screenY = window != null ? window.getY() + 150 : 150;

        try {
            Optional<Bounds> optBounds = codeArea.getCharacterBoundsOnScreen(codeArea.getCaretPosition(), codeArea.getCaretPosition());
            if (optBounds.isPresent()) {
                Bounds b = optBounds.get();
                screenX = b.getMinX();
                screenY = b.getMaxY() + 6;
            }
        } catch (Exception ignored) {}

        // Garante que o popup cabe na tela visível
        try {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            if (screenX + 460 > bounds.getMaxX()) {
                screenX = bounds.getMaxX() - 470;
            }
            if (screenY + 280 > bounds.getMaxY()) {
                screenY = screenY - 290;
            }
        } catch (Exception ignored) {}

        return new double[] { screenX, screenY };
    }

    private void applyColorToEditor(String colorValue) {
        int currentParagraph = codeArea.getCurrentParagraph();
        String line = codeArea.getParagraph(currentParagraph).getText();

        String trimmed = line.trim().toLowerCase();
        int colonIdx = line.indexOf(':');
        if (colonIdx != -1) {
            String dir = line.substring(0, colonIdx).trim().toLowerCase();
            if (dir.equals("color")) {
                int start = codeArea.getAbsolutePosition(currentParagraph, 0);
                int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
                String newText = "color: " + colorValue;
                codeArea.replaceText(start, end, newText);
                codeArea.moveTo(start + newText.length());
            } else if (dir.equals("background") || dir.equals("bg")) {
                int start = codeArea.getAbsolutePosition(currentParagraph, 0);
                int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
                String newText = "background: " + colorValue;
                codeArea.replaceText(start, end, newText);
                codeArea.moveTo(start + newText.length());
            } else {
                int caret = codeArea.getCaretPosition();
                String currentText = codeArea.getText();
                String updated = SlideEditorHelper.applyBackgroundToCurrentSlide(currentText, caret, colorValue);
                codeArea.replaceText(updated);
            }
        } else if (trimmed.startsWith("color:")) {
            int start = codeArea.getAbsolutePosition(currentParagraph, 0);
            int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
            String newText = "color: " + colorValue;
            codeArea.replaceText(start, end, newText);
            codeArea.moveTo(start + newText.length());
        } else if (trimmed.startsWith("background:") || trimmed.startsWith("bg:")) {
            int start = codeArea.getAbsolutePosition(currentParagraph, 0);
            int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
            String newText = "background: " + colorValue;
            codeArea.replaceText(start, end, newText);
            codeArea.moveTo(start + newText.length());
        } else {
            int caret = codeArea.getCaretPosition();
            String currentText = codeArea.getText();
            String updated = SlideEditorHelper.applyBackgroundToCurrentSlide(currentText, caret, colorValue);
            codeArea.replaceText(updated);
        }
        codeArea.requestFocus();
        if (onContentChanged != null) {
            onContentChanged.accept(codeArea.getText());
        }
    }

    private void applySuggestionToEditor(DirectiveAutocompletePopup.SuggestionItem item) {
        int currentParagraph = codeArea.getCurrentParagraph();
        String lineText = codeArea.getParagraph(currentParagraph).getText();
        int col = codeArea.getCaretColumn();
        String textBeforeCaret = lineText.substring(0, Math.min(col, lineText.length()));

        if (item.typeBadge().equals("VALOR") && lineText.contains(":")) {
            int colonIdx = lineText.indexOf(':');
            int start = codeArea.getAbsolutePosition(currentParagraph, colonIdx + 1);
            int end = codeArea.getAbsolutePosition(currentParagraph, lineText.length());
            codeArea.replaceText(start, end, " " + item.insertText());
        } else {
            int tokenStart = Math.max(textBeforeCaret.lastIndexOf(' '), textBeforeCaret.lastIndexOf('\t')) + 1;
            String token = textBeforeCaret.substring(tokenStart);

            int caret = codeArea.getCaretPosition();
            int replaceStart = caret - token.length();
            int replaceEnd = caret;

            codeArea.replaceText(replaceStart, replaceEnd, item.insertText());

            if (item.insertText().equals("background: ") || item.insertText().equals("bg: ") || item.insertText().equals("color: ")) {
                showColorPickerAtCaret();
                return;
            }
        }

        codeArea.requestFocus();
        if (onContentChanged != null) {
            onContentChanged.accept(codeArea.getText());
        }
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        if (text == null || text.isEmpty()) {
            return StyleSpans.singleton(Collections.emptyList(), 0);
        }
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            if (matcher.group("DIRECTIVE") != null) {
                int keyStart = matcher.start("DIRKEY");
                int keyEnd = matcher.end("DIRKEY");
                int valStart = matcher.start("DIRVAL");
                int valEnd = matcher.end("DIRVAL");

                if (keyStart > lastKwEnd) {
                    spansBuilder.add(Collections.emptyList(), keyStart - lastKwEnd);
                }
                if (keyEnd > keyStart) {
                    spansBuilder.add(Collections.singleton("md-directive-key"), keyEnd - keyStart);
                }
                if (valEnd > valStart) {
                    spansBuilder.add(Collections.singleton("md-directive-val"), valEnd - valStart);
                }
                lastKwEnd = valEnd;
                continue;
            }

            String styleClass =
                    matcher.group("DIVIDER") != null ? "md-divider" :
                    matcher.group("COMMENT") != null ? "md-comment" :
                    matcher.group("HEADER") != null ? "md-header" :
                    matcher.group("QUOTE") != null ? "md-quote" :
                    matcher.group("LIST") != null ? "md-list" :
                    matcher.group("CODE") != null ? "md-code" :
                    matcher.group("BOLD") != null ? "md-bold" :
                    matcher.group("ITALIC") != null ? "md-italic" : null;

            if (matcher.start() > lastKwEnd) {
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            }
            int matchLen = matcher.end() - matcher.start();
            if (matchLen > 0) {
                spansBuilder.add(Collections.singleton(styleClass), matchLen);
            }
            lastKwEnd = matcher.end();
        }
        if (text.length() > lastKwEnd) {
            spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        }
        return spansBuilder.create();
    }

    public void setMarkdownContent(String content) {
        isProgrammaticChange = true;
        codeArea.replaceText(content != null ? content : "");
        isProgrammaticChange = false;
        try {
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
        } catch (Exception ignored) {}
        updateStats();
    }

    public String getMarkdownContent() {
        return codeArea.getText();
    }

    private void updateStats() {
        String text = codeArea.getText();
        if (text.isBlank()) {
            statusLabel.setText("0 slides");
            currentSlideIndicator.setText("Slide 0 / 0");
            return;
        }
        int caret = codeArea.getCaretPosition();
        int currentSlide = SlideEditorHelper.getSlideIndexAtCaret(text, caret) + 1;
        int totalSlides = SlideEditorHelper.findSlideRanges(text).size();

        currentSlideIndicator.setText(String.format("Slide %02d / %02d", currentSlide, totalSlides));
        statusLabel.setText(totalSlides + " slide" + (totalSlides > 1 ? "s" : ""));
    }

    public int getCaretPosition() {
        return codeArea.getCaretPosition();
    }

    private void checkSlideNavigated() {
        if (isNavigatingProgrammatically) return;
        String text = codeArea.getText();
        if (text.isBlank()) return;
        int caret = codeArea.getCaretPosition();
        int currentSlide = SlideEditorHelper.getSlideIndexAtCaret(text, caret);
        if (currentSlide != lastNavigatedSlideIndex) {
            lastNavigatedSlideIndex = currentSlide;
            if (onSlideNavigated != null) {
                onSlideNavigated.accept(currentSlide);
            }
        }
    }

    public void scrollToSlide(int slideIndex) {
        if (slideIndex < 0) return;
        String text = codeArea.getText();
        if (text.isBlank()) return;
        int caret = SlideEditorHelper.getSlideStartCaret(text, slideIndex);
        int targetParagraph = SlideEditorHelper.getSlideStartParagraph(text, slideIndex);

        isNavigatingProgrammatically = true;
        lastNavigatedSlideIndex = slideIndex;
        try {
            codeArea.moveTo(caret);
            if (targetParagraph >= 0 && targetParagraph < codeArea.getParagraphs().size()) {
                codeArea.showParagraphAtTop(targetParagraph);
            }
            codeArea.requestFollowCaret();
            updateStats();
        } finally {
            isNavigatingProgrammatically = false;
        }
    }
}
