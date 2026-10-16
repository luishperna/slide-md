package com.slidemd;

import com.slidemd.animation.TransitionManager;
import com.slidemd.model.Slide;
import com.slidemd.model.SlideDeck;
import com.slidemd.parser.MarkdownSlideParser;
import com.slidemd.ui.FloatingEditorWindow;
import com.slidemd.ui.HUDOverlay;
import com.slidemd.ui.SlideView;
import com.slidemd.video.FFmpegService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlideMdApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(SlideMdApp.class.getName());

    private final MarkdownSlideParser parser = new MarkdownSlideParser();
    private final FFmpegService ffmpegService = new FFmpegService();
    private final TransitionManager transitionManager = new TransitionManager();

    private Stage primaryStage;
    private StackPane rootLayout;
    private StackPane slideContainer;
    private HUDOverlay hudOverlay;
    private FloatingEditorWindow floatingEditor;

    private SlideDeck currentDeck;
    private int currentSlideIndex = 0;
    private SlideView currentSlideView;
    private File loadedFile;

    private static final String DEFAULT_PRESENTATION = """
            ---
            bg: linear-gradient(to bottom right, #090d16, #1e293b)
            align: center
            transition: slide
            ---
            
            # slide-md
            Apresentações Modernas e Rápidas em Java através de Markdown
            
            Pressione **Espaço** ou **Seta Direita** para avançar
            
            ---
            bg: #0f172a
            align: left
            transition: slide
            ---
            
            # Recursos Principais
            
            - **Ultra Rápido & Performático**: Renderização nativa em JavaFX a 60 FPS
            - **Transições Fluidas**: Animações suaves entre slides
            - **Editor Flutuante**: Pressione `M` ou clique em `MD` para editar ao vivo
            - **Edição de Vídeo**: Corte e junção instantâneos com FFmpeg
            
            ```markdown
            ---
            bg: #1e1e2e
            align: center
            ---
            # Meu Slide Incrível
            ```
            
            ---
            bg: #034f84
            align: center
            transition: fade
            ---
            
            # Personalização Simples
            
            > "Apresentações bonitas não precisam ser difíceis de criar."
            
            Cores de fundo, gradientes, imagens e alinhamento flexível.
            
            ---
            bg: #111827
            align: center
            transition: zoom
            ---
            
            # Suporte a Vídeo com FFmpeg
            
            Defina no topo do slide:
            `video: caminho/para/video.mp4`
            `cut: 00:05 - 00:25`
            
            O corte é feito sem re-encoding (instantâneo) e salvo em cache!
            """;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        rootLayout = new StackPane();
        rootLayout.setStyle("-fx-background-color: #000000;");

        slideContainer = new StackPane();
        slideContainer.setAlignment(Pos.CENTER);
        rootLayout.getChildren().add(slideContainer);

        // Setup Floating Editor
        floatingEditor = new FloatingEditorWindow(this::onEditorContentChanged, this::toggleEditor);
        StackPane.setAlignment(floatingEditor, Pos.CENTER);

        // Setup Minimal HUD
        hudOverlay = new HUDOverlay(this::prevSlide, this::nextSlide, this::toggleEditor, this::toggleFullscreen);
        StackPane.setAlignment(hudOverlay, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hudOverlay, new Insets(0, 0, 28, 0));

        rootLayout.getChildren().addAll(hudOverlay, floatingEditor);

        Scene scene = new Scene(rootLayout, 1280, 720);
        setupKeyHandlers(scene);
        hudOverlay.bindSceneActivity(scene);

        stage.setTitle("slide-md");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        // Load presentation from CLI argument or default demo
        loadInitialPresentation();

        stage.show();
    }

    private void loadInitialPresentation() {
        var args = getParameters().getRaw();
        if (!args.isEmpty()) {
            File targetFile = new File(args.get(0));
            if (targetFile.exists()) {
                loadFile(targetFile);
                return;
            }
        }

        // Check if demo.md exists in current working directory
        File localDemo = new File("demo.md");
        if (localDemo.exists()) {
            loadFile(localDemo);
        } else {
            loadDefaultDeck();
        }
    }

    public void loadFile(File file) {
        try {
            this.loadedFile = file;
            this.currentDeck = parser.parseFile(file);
            primaryStage.setTitle("slide-md — " + currentDeck.getTitle());
            floatingEditor.setMarkdownContent(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            currentSlideIndex = 0;
            displaySlide(currentSlideIndex, false, true);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar arquivo: " + file.getAbsolutePath(), e);
            loadDefaultDeck();
        }
    }

    private void loadDefaultDeck() {
        this.currentDeck = parser.parseContent(DEFAULT_PRESENTATION);
        primaryStage.setTitle("slide-md — Apresentação Demonstrativa");
        floatingEditor.setMarkdownContent(DEFAULT_PRESENTATION);
        currentSlideIndex = 0;
        displaySlide(currentSlideIndex, false, true);
    }

    private void onEditorContentChanged(String newMarkdown) {
        Platform.runLater(() -> {
            SlideDeck updatedDeck = parser.parseContent(newMarkdown);
            File baseDir = loadedFile != null ? loadedFile.getParentFile() : new File(".");
            updatedDeck.setSourceFile(loadedFile);
            this.currentDeck = updatedDeck;

            if (currentSlideIndex >= currentDeck.getSlideCount()) {
                currentSlideIndex = Math.max(0, currentDeck.getSlideCount() - 1);
            }
            displaySlide(currentSlideIndex, false, true);

            // Save to disk if loaded from a file
            if (loadedFile != null && loadedFile.exists()) {
                try {
                    Files.writeString(loadedFile.toPath(), newMarkdown, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOGGER.warning("Could not auto-save to file: " + e.getMessage());
                }
            }
        });
    }

    private void displaySlide(int index, boolean animated, boolean forward) {
        if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
        if (index < 0 || index >= currentDeck.getSlideCount()) return;

        Slide targetSlide = currentDeck.getSlide(index);
        File baseDir = loadedFile != null ? loadedFile.getParentFile() : new File(".");
        SlideView nextView = new SlideView(targetSlide, baseDir, ffmpegService, parser);

        hudOverlay.updateCounter(index, currentDeck.getSlideCount());

        if (!animated || currentSlideView == null) {
            if (currentSlideView != null) {
                currentSlideView.onSlideLeave();
                currentSlideView.dispose();
            }
            slideContainer.getChildren().setAll(nextView);
            nextView.onSlideEnter();
            currentSlideView = nextView;
        } else {
            SlideView oldView = currentSlideView;
            String transition = targetSlide.getTransition();
            transitionManager.transition(slideContainer, oldView, nextView, forward, transition, () -> {
                oldView.dispose();
                currentSlideView = nextView;
            });
        }
    }

    public void nextSlide() {
        if (transitionManager.isAnimating()) return;
        if (currentSlideIndex < currentDeck.getSlideCount() - 1) {
            currentSlideIndex++;
            displaySlide(currentSlideIndex, true, true);
        }
    }

    public void prevSlide() {
        if (transitionManager.isAnimating()) return;
        if (currentSlideIndex > 0) {
            currentSlideIndex--;
            displaySlide(currentSlideIndex, true, false);
        }
    }

    public void firstSlide() {
        if (currentSlideIndex != 0) {
            currentSlideIndex = 0;
            displaySlide(currentSlideIndex, true, false);
        }
    }

    public void lastSlide() {
        int last = currentDeck.getSlideCount() - 1;
        if (currentSlideIndex != last) {
            currentSlideIndex = last;
            displaySlide(currentSlideIndex, true, true);
        }
    }

    public void toggleEditor() {
        floatingEditor.toggleWindow();
    }

    public void toggleFullscreen() {
        primaryStage.setFullScreen(!primaryStage.isFullScreen());
    }

    public void reloadPresentation() {
        if (loadedFile != null && loadedFile.exists()) {
            loadFile(loadedFile);
        }
    }

    private void setupKeyHandlers(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // When editor is focused, only handle Esc and Ctrl/Cmd shortcuts
            boolean editorFocused = floatingEditor.isVisible() && floatingEditor.lookup(".text-area") != null && floatingEditor.lookup(".text-area").isFocused();

            if (event.getCode() == KeyCode.ESCAPE) {
                if (floatingEditor.isVisible()) {
                    floatingEditor.hideWindow();
                    event.consume();
                } else if (primaryStage.isFullScreen()) {
                    primaryStage.setFullScreen(false);
                    event.consume();
                }
                return;
            }

            if (editorFocused) {
                // Allow regular typing in editor
                return;
            }

            switch (event.getCode()) {
                case RIGHT, SPACE, PAGE_DOWN, DOWN, ENTER -> {
                    nextSlide();
                    event.consume();
                }
                case LEFT, PAGE_UP, UP, BACK_SPACE -> {
                    prevSlide();
                    event.consume();
                }
                case HOME -> {
                    firstSlide();
                    event.consume();
                }
                case END -> {
                    lastSlide();
                    event.consume();
                }
                case F, F11 -> {
                    toggleFullscreen();
                    event.consume();
                }
                case M, E -> {
                    toggleEditor();
                    event.consume();
                }
                case R -> {
                    reloadPresentation();
                    event.consume();
                }
                default -> {}
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
