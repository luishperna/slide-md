package com.slidemd.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

public class FloatingEditorWindow {

    private final Stage editorStage;
    private final CodeArea codeArea;
    private final Label statusLabel;
    private final Label currentSlideIndicator;
    private final Consumer<String> onContentChanged;
    private final Runnable onSave;
    private final PauseTransition debounceTimer;
    private final PauseTransition highlightTimer;
    private final PauseTransition assistTimer;

    private final SlideColorPickerPopup colorPickerPopup;
    private final DirectiveAutocompletePopup autocompletePopup;

    private final SVGPath maxIcon;
    private final SVGPath restoreIcon;
    private final Button maxRestoreBtn;

    private boolean isProgrammaticChange = false;
    private boolean stagePositioned = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private static final Pattern PATTERN = Pattern.compile(
            "(?<DIVIDER>^\\s*---\\s*$)"
            + "|(?<HEADER>^\\s*#{1,6}\\s+.*$)"
            + "|(?<DIRECTIVE>^\\s*(bg|align|transition|video|cut|join|loop|autoplay|slide\\s+\\d+):.*$)"
            + "|(?<QUOTE>^\\s*>.*$)"
            + "|(?<LIST>^\\s*([-*+]|\\d+\\.)\\s+)"
            + "|(?<CODE>`[^`\\n]+`|```[\\s\\S]*?```)"
            + "|(?<BOLD>\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__)",
            Pattern.MULTILINE
    );

    private final Consumer<Integer> onSlideNavigated;
    private boolean isNavigatingProgrammatically = false;
    private int lastNavigatedSlideIndex = -1;

    public FloatingEditorWindow(Stage ownerStage, Consumer<String> onContentChanged, Runnable onSave) {
        this(ownerStage, onContentChanged, onSave, null);
    }

    public FloatingEditorWindow(Stage ownerStage, Consumer<String> onContentChanged, Runnable onSave, Consumer<Integer> onSlideNavigated) {
        this.onContentChanged = onContentChanged;
        this.onSave = onSave;
        this.onSlideNavigated = onSlideNavigated;
        this.editorStage = new Stage();

        if (ownerStage != null) {
            editorStage.initOwner(ownerStage);
        }

        editorStage.initStyle(StageStyle.UNDECORATED);
        editorStage.setTitle("slide-md — Editor Markdown");
        editorStage.setMinWidth(520);
        editorStage.setMinHeight(400);

        // CodeArea setup with line numbers
        this.codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("code-area");

        // Debounce timer for real-time live preview update
        debounceTimer = new PauseTransition(Duration.millis(250));
        debounceTimer.setOnFinished(e -> {
            if (!isProgrammaticChange && this.onContentChanged != null) {
                this.onContentChanged.accept(codeArea.getText());
            }
        });

        // Fast syntax highlight timer
        highlightTimer = new PauseTransition(Duration.millis(40));
        highlightTimer.setOnFinished(e -> {
            Platform.runLater(() -> {
                try {
                    codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
                } catch (Exception ignored) {}
            });
        });

        // Timer de assistência para detectar digitação de "bg:", "align:", etc.
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
        });

        // 1. Barra Superior Customizada (#161616 - Idêntica à estética do App)
        HBox topTitleBar = new HBox();
        topTitleBar.setAlignment(Pos.CENTER_LEFT);
        topTitleBar.setPrefHeight(38);
        topTitleBar.setMinHeight(38);
        topTitleBar.setMaxHeight(38);
        topTitleBar.setPadding(new Insets(0, 0, 0, 14));
        topTitleBar.setStyle("-fx-background-color: #161616;" +
                             "-fx-border-color: transparent transparent #262626 transparent;" +
                             "-fx-border-width: 1px;");

        Label logoLabel = new Label("📝 slide-md");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12.5));
        logoLabel.setTextFill(Color.web("#ffffff"));

        Label separator = new Label("—");
        separator.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        separator.setTextFill(Color.web("#404040"));
        HBox.setMargin(separator, new Insets(0, 8, 0, 8));

        Label windowTitle = new Label("Editor Markdown");
        windowTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        windowTitle.setTextFill(Color.web("#a3a3a3"));

        HBox titleLeftBox = new HBox(logoLabel, separator, windowTitle);
        titleLeftBox.setAlignment(Pos.CENTER_LEFT);

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        // Controles de Janela (Minimizar, Maximizar/Restaurar, Fechar)
        SVGPath minIcon = createMinIcon();
        maxIcon = createMaxIcon();
        restoreIcon = createRestoreIcon();
        SVGPath closeIcon = createCloseIcon();

        Button minimizeBtn = createWindowButton(minIcon, false, "Minimizar", () -> editorStage.setIconified(true));
        maxRestoreBtn = createWindowButton(maxIcon, false, "Maximizar", () -> {
            editorStage.setMaximized(!editorStage.isMaximized());
        });
        Button closeBtn = createWindowButton(closeIcon, true, "Fechar (Esc)", this::hideWindow);

        editorStage.maximizedProperty().addListener((obs, oldVal, isMax) -> {
            if (isMax) {
                maxRestoreBtn.setGraphic(restoreIcon);
                Tooltip.install(maxRestoreBtn, new Tooltip("Restaurar Tamanho"));
            } else {
                maxRestoreBtn.setGraphic(maxIcon);
                Tooltip.install(maxRestoreBtn, new Tooltip("Maximizar"));
            }
        });

        HBox windowControls = new HBox(minimizeBtn, maxRestoreBtn, closeBtn);
        windowControls.setAlignment(Pos.CENTER_RIGHT);

        topTitleBar.getChildren().addAll(titleLeftBox, titleSpacer, windowControls);

        // Drag da janela pelo topo
        topTitleBar.setOnMousePressed(e -> {
            if (!editorStage.isMaximized()) {
                dragOffsetX = e.getScreenX() - editorStage.getX();
                dragOffsetY = e.getScreenY() - editorStage.getY();
            }
        });
        topTitleBar.setOnMouseDragged(e -> {
            if (!editorStage.isMaximized()) {
                editorStage.setX(e.getScreenX() - dragOffsetX);
                editorStage.setY(e.getScreenY() - dragOffsetY);
            }
        });
        topTitleBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editorStage.setMaximized(!editorStage.isMaximized());
            }
        });

        // 2. Toolbar de Facilitadores (#181818)
        HBox actionToolbar = new HBox(8);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);
        actionToolbar.setPadding(new Insets(6, 14, 6, 14));
        actionToolbar.setStyle("-fx-background-color: #181818;" +
                               "-fx-border-color: transparent transparent #262626 transparent;" +
                               "-fx-border-width: 1px;");

        Button addSlideBtn = createActionButton("+ Adicionar Slide", "Adicionar novo slide delimitado por --- após o slide atual", this::addNewSlide);
        Button removeSlideBtn = createActionButton("- Remover Slide", "Remover o slide atual onde o cursor está posicionado", this::removeCurrentSlide);
        Button colorPickerBtn = createActionButton("🎨 Cor de Fundo (bg)", "Abrir seletor de cor ou gradiente para o slide atual", this::openColorPickerManual);
        Button directivesBtn = createActionButton("⚡ Inserir Diretiva", "Menu de diretivas suportadas (align, transition, video, loop...)", this::openDirectivesManual);

        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);

        currentSlideIndicator = new Label("Slide 1 / 1");
        currentSlideIndicator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        currentSlideIndicator.setTextFill(Color.web("#a3a3a3"));
        HBox.setMargin(currentSlideIndicator, new Insets(0, 10, 0, 0));

        Button saveBtn = new Button("💾 Salvar");
        saveBtn.setStyle("-fx-background-color: #262626; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-border-color: #383838; -fx-border-radius: 4px;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-border-color: #ffffff; -fx-border-radius: 4px;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #262626; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 12 4 12; -fx-font-size: 11.5px; -fx-font-weight: bold; -fx-border-color: #383838; -fx-border-radius: 4px;"));
        saveBtn.setOnAction(e -> {
            if (this.onSave != null) this.onSave.run();
        });
        Tooltip.install(saveBtn, new Tooltip("Salvar apresentação (Ctrl+S)"));

        actionToolbar.getChildren().addAll(addSlideBtn, removeSlideBtn, colorPickerBtn, directivesBtn, toolbarSpacer, currentSlideIndicator, saveBtn);

        // 3. Editor em ScrollPane (#1c1c1c)
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.setStyle("-fx-background-color: #1c1c1c;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 4. Rodapé / Barra de Status (#161616)
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(5, 14, 6, 14));
        footer.setStyle("-fx-background-color: #161616;" +
                        "-fx-border-color: #262626 transparent transparent transparent;" +
                        "-fx-border-width: 1px;");

        statusLabel = new Label("0 slides detectados");
        statusLabel.setFont(Font.font("Segoe UI", 11.5));
        statusLabel.setTextFill(Color.web("#94a3b8"));

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label hintsLabel = new Label("Ctrl+Space: Autocomplete  •  Ctrl+S: Salvar  •  Esc: Fechar");
        hintsLabel.setFont(Font.font("Segoe UI", 11));
        hintsLabel.setTextFill(Color.web("#64748b"));

        footer.getChildren().addAll(statusLabel, footerSpacer, hintsLabel);

        VBox topHeaderContainer = new VBox(topTitleBar, actionToolbar);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1c1c1c; -fx-border-color: #262626; -fx-border-width: 1px;");
        root.setTop(topHeaderContainer);
        root.setCenter(scrollPane);
        root.setBottom(footer);

        Scene scene = new Scene(root, 760, 600);

        // Suporte a redimensionamento por arrasto em bordas e cantos
        WindowResizeHelper.addResizeListener(editorStage, scene);

        // Carrega CSS de realce sintático Markdown
        try {
            var cssUrl = getClass().getResource("/css/markdown-editor.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}

        // Atalhos de teclado: Esc fecha popups primeiro, depois janela. Ctrl+S salva. Ctrl+Space abre autocomplete.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (colorPickerPopup.isShowing()) {
                    colorPickerPopup.hide();
                    event.consume();
                } else if (autocompletePopup.isShowing()) {
                    autocompletePopup.hide();
                    event.consume();
                } else {
                    hideWindow();
                    event.consume();
                }
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
                if (this.onSave != null) this.onSave.run();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.SPACE) {
                openDirectivesManual();
                event.consume();
            }
        });

        editorStage.setScene(scene);
        editorStage.setOnCloseRequest(e -> {
            e.consume();
            hideWindow();
        });
    }

    private Button createActionButton(String text, String tooltip, Runnable action) {
        Button btn = new Button(text);
        String base = "-fx-background-color: #262626; -fx-text-fill: #e5e5e5; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 10 4 10; -fx-font-size: 11.5px;";
        String hover = "-fx-background-color: #333333; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 4 10 4 10; -fx-font-size: 11.5px;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        btn.setOnAction(e -> {
            if (action != null) action.run();
            codeArea.requestFocus();
        });
        if (tooltip != null) {
            Tooltip.install(btn, new Tooltip(tooltip));
        }
        return btn;
    }

    private Button createWindowButton(SVGPath icon, boolean isClose, String tooltipText, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setPrefWidth(46);
        btn.setPrefHeight(38);
        btn.setMinHeight(38);
        btn.setMaxHeight(38);
        btn.setAlignment(Pos.CENTER);

        String baseStyle = "-fx-background-color: transparent; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;";
        String hoverStyle = isClose
            ? "-fx-background-color: #e81123; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;"
            : "-fx-background-color: #262626; -fx-background-radius: 0; -fx-cursor: hand; -fx-padding: 0;";

        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> {
            btn.setStyle(hoverStyle);
            icon.setStroke(Color.WHITE);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            icon.setStroke(Color.web("#cccccc"));
        });

        btn.setOnAction(e -> {
            if (action != null) action.run();
        });

        if (tooltipText != null) {
            Tooltip.install(btn, new Tooltip(tooltipText));
        }

        return btn;
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

    private void openColorPickerManual() {
        showColorPickerAtCaret();
    }

    private void openDirectivesManual() {
        showAutocompleteAtCaret(DirectiveAutocompletePopup.DIRECTIVE_SUGGESTIONS);
    }

    private void checkContextualAssistance() {
        if (!editorStage.isShowing()) return;

        try {
            int currentParagraph = codeArea.getCurrentParagraph();
            String lineText = codeArea.getParagraph(currentParagraph).getText();
            int col = codeArea.getCaretColumn();
            String prefix = lineText.substring(0, Math.min(col, lineText.length())).trim().toLowerCase();

            // 1. Detecta se digitou "bg:" para abrir o card de seleção de cores
            if (prefix.equals("bg:") || prefix.startsWith("bg: ")) {
                showColorPickerAtCaret();
                return;
            }

            // 2. Detecta se digitou "align:"
            if (prefix.equals("align:") || prefix.equals("align: ")) {
                colorPickerPopup.hide();
                showAutocompleteAtCaret(DirectiveAutocompletePopup.ALIGN_VALUES);
                return;
            }

            // 3. Detecta se digitou "transition:"
            if (prefix.equals("transition:") || prefix.equals("transition: ")) {
                colorPickerPopup.hide();
                showAutocompleteAtCaret(DirectiveAutocompletePopup.TRANSITION_VALUES);
                return;
            }

            // 4. Detecta se digitou "loop:" ou "autoplay:"
            if (prefix.equals("loop:") || prefix.equals("loop: ") || prefix.equals("autoplay:") || prefix.equals("autoplay: ")) {
                colorPickerPopup.hide();
                showAutocompleteAtCaret(DirectiveAutocompletePopup.BOOLEAN_VALUES);
                return;
            }
        } catch (Exception ignored) {}
    }

    private void showColorPickerAtCaret() {
        double[] coords = getCaretScreenCoordinates();
        colorPickerPopup.show(editorStage, coords[0], coords[1]);
    }

    private void showAutocompleteAtCaret(java.util.List<DirectiveAutocompletePopup.SuggestionItem> items) {
        autocompletePopup.setSuggestions(items);
        double[] coords = getCaretScreenCoordinates();
        autocompletePopup.show(editorStage, coords[0], coords[1]);
    }

    private double[] getCaretScreenCoordinates() {
        double screenX = editorStage.getX() + 70;
        double screenY = editorStage.getY() + 110;

        try {
            Optional<Bounds> optBounds = codeArea.getCharacterBoundsOnScreen(codeArea.getCaretPosition(), codeArea.getCaretPosition());
            if (optBounds.isPresent()) {
                Bounds b = optBounds.get();
                screenX = b.getMinX();
                screenY = b.getMaxY() + 6;
            }
        } catch (Exception ignored) {}

        // Garante que o card cabe na tela visível
        try {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            if (screenX + 310 > bounds.getMaxX()) {
                screenX = bounds.getMaxX() - 320;
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

        if (line.trim().toLowerCase().startsWith("bg:")) {
            // Substitui o valor da linha atual
            int start = codeArea.getAbsolutePosition(currentParagraph, 0);
            int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
            codeArea.replaceText(start, end, "bg: " + colorValue);
        } else {
            // Aplica no slide atual via helper
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
        String line = codeArea.getParagraph(currentParagraph).getText();

        if (item.insertText().equals("bg: ")) {
            codeArea.insertText(codeArea.getCaretPosition(), "bg: ");
            showColorPickerAtCaret();
            return;
        }

        if (line.trim().contains(":")) {
            // Substitui valor após os dois pontos
            int colonIdx = line.indexOf(':');
            int start = codeArea.getAbsolutePosition(currentParagraph, colonIdx + 1);
            int end = codeArea.getAbsolutePosition(currentParagraph, line.length());
            codeArea.replaceText(start, end, " " + item.insertText());
        } else {
            codeArea.insertText(codeArea.getCaretPosition(), item.insertText());
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
            String styleClass =
                    matcher.group("DIVIDER") != null ? "md-divider" :
                    matcher.group("HEADER") != null ? "md-header" :
                    matcher.group("DIRECTIVE") != null ? "md-directive" :
                    matcher.group("QUOTE") != null ? "md-quote" :
                    matcher.group("LIST") != null ? "md-list" :
                    matcher.group("CODE") != null ? "md-code" :
                    matcher.group("BOLD") != null ? "md-bold" : null;

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
            statusLabel.setText("0 slides detectados");
            currentSlideIndicator.setText("Slide 0 / 0");
            return;
        }
        int caret = codeArea.getCaretPosition();
        int currentSlide = SlideEditorHelper.getSlideIndexAtCaret(text, caret) + 1;
        int totalSlides = SlideEditorHelper.findSlideRanges(text).size();

        currentSlideIndicator.setText(String.format("Slide %02d / %02d", currentSlide, totalSlides));
        statusLabel.setText(totalSlides + " slide" + (totalSlides > 1 ? "s" : "") + " detectados");
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

        isNavigatingProgrammatically = true;
        lastNavigatedSlideIndex = slideIndex;
        try {
            codeArea.moveTo(caret);
            codeArea.requestFollowCaret();
            updateStats();
        } finally {
            isNavigatingProgrammatically = false;
        }
    }

    public void showWindow() {
        showWindow(-1);
    }

    public void showWindow(int initialSlideIndex) {
        if (!editorStage.isShowing()) {
            if (!stagePositioned && editorStage.getOwner() instanceof Stage owner && owner.isShowing()) {
                editorStage.setX(owner.getX() + 60);
                editorStage.setY(owner.getY() + 40);
                stagePositioned = true;
            }
            editorStage.show();
        }
        editorStage.toFront();
        if (initialSlideIndex >= 0) {
            scrollToSlide(initialSlideIndex);
        }
        codeArea.requestFocus();
    }

    public void hideWindow() {
        colorPickerPopup.hide();
        autocompletePopup.hide();
        editorStage.hide();
    }

    public void toggleWindow() {
        toggleWindow(-1);
    }

    public void toggleWindow(int initialSlideIndex) {
        if (editorStage.isShowing()) {
            hideWindow();
        } else {
            showWindow(initialSlideIndex);
        }
    }

    public boolean isVisible() {
        return editorStage.isShowing();
    }

    public Stage getStage() {
        return editorStage;
    }

    private SVGPath createMinIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0 5 L 10 5");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.2);
        path.setStrokeLineCap(StrokeLineCap.BUTT);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createMaxIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0.5 0.5 L 9.5 0.5 L 9.5 9.5 L 0.5 9.5 Z");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.1);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createRestoreIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 2.5 0.5 L 9.5 0.5 L 9.5 7.5 L 7.5 7.5 M 7.5 2.5 L 0.5 2.5 L 0.5 9.5 L 7.5 9.5 Z");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.1);
        path.setFill(Color.TRANSPARENT);
        return path;
    }

    private SVGPath createCloseIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M 0.5 0.5 L 9.5 9.5 M 9.5 0.5 L 0.5 9.5");
        path.setStroke(Color.web("#cccccc"));
        path.setStrokeWidth(1.2);
        path.setStrokeLineCap(StrokeLineCap.BUTT);
        path.setFill(Color.TRANSPARENT);
        return path;
    }
}
