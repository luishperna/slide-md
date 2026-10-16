package com.slidemd;

import com.slidemd.animation.TransitionManager;
import com.slidemd.model.Slide;
import com.slidemd.model.SlideDeck;
import com.slidemd.parser.MarkdownSlideParser;
import com.slidemd.ui.AppTitleBar;
import com.slidemd.ui.FloatingEditorWindow;
import com.slidemd.ui.SlideEditorHelper;
import com.slidemd.ui.SlideView;
import com.slidemd.ui.WelcomeView;
import com.slidemd.ui.WindowResizeHelper;
import com.slidemd.video.FFmpegService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlideMdApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(SlideMdApp.class.getName());

    private final MarkdownSlideParser parser = new MarkdownSlideParser();
    private final FFmpegService ffmpegService = new FFmpegService();
    private final TransitionManager transitionManager = new TransitionManager();

    private Stage primaryStage;
    private AppTitleBar appTitleBar;
    private StackPane rootLayout;
    private StackPane slideContainer;
    private FloatingEditorWindow floatingEditor;

    private SlideDeck currentDeck;
    private int currentSlideIndex = 0;
    private SlideView currentSlideView;
    private File loadedFile;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreenExitHint("");

        BorderPane mainContainer = new BorderPane();
        mainContainer.setStyle("-fx-background-color: #101010;");

        // Custom Top Title Bar with Centralized Dock and Navigation
        appTitleBar = new AppTitleBar(stage, null,
                this::createNewPresentation, this::openFileDialog, this::toggleEditor,
                this::prevSlide, this::nextSlide);
        mainContainer.setTop(appTitleBar);

        rootLayout = new StackPane();
        rootLayout.setStyle("-fx-background-color: #101010;");
        mainContainer.setCenter(rootLayout);

        // Slide Container with Click Navigation (left half = back, right half = next)
        slideContainer = new StackPane();
        slideContainer.setAlignment(Pos.CENTER);
        slideContainer.setOnMouseClicked(event -> {
            if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
            if (event.getButton() != MouseButton.PRIMARY) return;

            double width = slideContainer.getWidth();
            if (event.getX() < width / 2.0) {
                prevSlide();
            } else {
                nextSlide();
            }
        });
        rootLayout.getChildren().add(slideContainer);

        // Setup Standalone Floating Editor Window (opens outside presentation with two-way sync)
        floatingEditor = new FloatingEditorWindow(stage, this::onEditorContentChanged, this::savePresentation, this::onEditorSlideNavigated);

        Scene scene = new Scene(mainContainer, 1280, 720);
        setupKeyHandlers(scene);
        setupDragAndDrop(scene);
        WindowResizeHelper.addResizeListener(stage, scene);

        // Hide title bar when entering full screen, show when windowed
        stage.fullScreenProperty().addListener((obs, oldVal, isFull) -> {
            appTitleBar.setVisible(!isFull);
            appTitleBar.setManaged(!isFull);
        });

        // In full screen, reveal title bar if mouse hovers at the top edge
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isFullScreen()) {
                if (e.getY() <= 40) {
                    if (!appTitleBar.isVisible()) {
                        appTitleBar.setVisible(true);
                        appTitleBar.setManaged(true);
                    }
                } else if (e.getY() > 65) {
                    if (appTitleBar.isVisible()) {
                        appTitleBar.setVisible(false);
                        appTitleBar.setManaged(false);
                    }
                }
            }
        });

        stage.setTitle("slide-md");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);

        // Open Welcome Screen by default, or load CLI argument file
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
        showWelcomeScreen();
    }

    public void showWelcomeScreen() {
        this.currentDeck = null;
        this.loadedFile = null;
        primaryStage.setTitle("slide-md");
        if (appTitleBar != null) {
            appTitleBar.setTitle(null);
            appTitleBar.setNavigationVisible(false);
        }
        if (currentSlideView != null) {
            currentSlideView.onSlideLeave();
            currentSlideView.dispose();
            currentSlideView = null;
        }
        WelcomeView welcomeView = new WelcomeView(this::createNewPresentation, this::openFileDialog);
        slideContainer.getChildren().setAll(welcomeView);
        slideContainer.setCursor(Cursor.DEFAULT);
    }

    public void loadFile(File file) {
        try {
            this.loadedFile = file;
            this.currentDeck = parser.parseFile(file);
            primaryStage.setTitle("slide-md — " + currentDeck.getTitle());
            if (appTitleBar != null) {
                appTitleBar.setTitle(currentDeck.getTitle());
                appTitleBar.setNavigationVisible(true);
            }
            floatingEditor.setMarkdownContent(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            currentSlideIndex = 0;
            slideContainer.setCursor(Cursor.HAND);
            displaySlide(currentSlideIndex, false, true);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao carregar arquivo: " + file.getAbsolutePath(), e);
            showWelcomeScreen();
        }
    }

    private void onEditorSlideNavigated(int slideIndex) {
        if (currentDeck != null && slideIndex >= 0 && slideIndex < currentDeck.getSlideCount()) {
            if (slideIndex != currentSlideIndex) {
                currentSlideIndex = slideIndex;
                displaySlide(currentSlideIndex, false, true);
            }
        }
    }

    private void onEditorContentChanged(String newMarkdown) {
        Platform.runLater(() -> {
            SlideDeck updatedDeck = parser.parseContent(newMarkdown);
            File baseDir = loadedFile != null ? loadedFile.getParentFile() : new File(".");
            updatedDeck.setSourceFile(loadedFile);
            this.currentDeck = updatedDeck;

            int editingSlide = SlideEditorHelper.getSlideIndexAtCaret(newMarkdown, floatingEditor.getCaretPosition());
            if (editingSlide >= 0 && editingSlide < currentDeck.getSlideCount()) {
                currentSlideIndex = editingSlide;
            } else if (currentSlideIndex >= currentDeck.getSlideCount()) {
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

        appTitleBar.updateCounter(index, currentDeck.getSlideCount());

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
        if (currentDeck != null && currentSlideIndex < currentDeck.getSlideCount() - 1) {
            currentSlideIndex++;
            displaySlide(currentSlideIndex, true, true);
            if (floatingEditor != null && floatingEditor.isVisible()) {
                floatingEditor.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void prevSlide() {
        if (transitionManager.isAnimating()) return;
        if (currentDeck != null && currentSlideIndex > 0) {
            currentSlideIndex--;
            displaySlide(currentSlideIndex, true, false);
            if (floatingEditor != null && floatingEditor.isVisible()) {
                floatingEditor.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void firstSlide() {
        if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
        if (currentSlideIndex != 0) {
            currentSlideIndex = 0;
            displaySlide(currentSlideIndex, true, false);
            if (floatingEditor != null && floatingEditor.isVisible()) {
                floatingEditor.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void lastSlide() {
        if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
        int last = currentDeck.getSlideCount() - 1;
        if (currentSlideIndex != last) {
            currentSlideIndex = last;
            displaySlide(currentSlideIndex, true, true);
            if (floatingEditor != null && floatingEditor.isVisible()) {
                floatingEditor.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void toggleEditor() {
        floatingEditor.toggleWindow(currentSlideIndex);
    }

    public void toggleFullscreen() {
        primaryStage.setFullScreen(!primaryStage.isFullScreen());
    }

    public void reloadPresentation() {
        if (loadedFile != null && loadedFile.exists()) {
            loadFile(loadedFile);
        }
    }

    public void createNewPresentation() {
        String newTemplate = """
                ---
                bg: linear-gradient(to bottom right, #090d16, #1e293b)
                align: center
                transition: morph
                ---

                # Nova Apresentação
                Edite seu conteúdo em tempo real

                Pressione **M** para abrir o editor

                ---
                bg: #0f172a
                align: left

                ## Tópicos Principais
                - Primeiro ponto importante
                - Segundo ponto importante
                """;
        this.loadedFile = null;
        this.currentDeck = parser.parseContent(newTemplate);
        primaryStage.setTitle("slide-md — Nova Apresentação");
        if (appTitleBar != null) {
            appTitleBar.setTitle("Nova Apresentação");
            appTitleBar.setNavigationVisible(true);
        }
        floatingEditor.setMarkdownContent(newTemplate);
        currentSlideIndex = 0;
        slideContainer.setCursor(Cursor.HAND);
        displaySlide(0, false, true);
        floatingEditor.showWindow();
    }

    public void savePresentation() {
        if (loadedFile != null && loadedFile.exists()) {
            try {
                Files.writeString(loadedFile.toPath(), floatingEditor.getMarkdownContent(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.warning("Erro ao salvar arquivo: " + e.getMessage());
            }
        } else {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar Apresentação Markdown");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivo Markdown (*.md)", "*.md"));
            fileChooser.setInitialFileName("apresentacao.md");
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try {
                    Files.writeString(file.toPath(), floatingEditor.getMarkdownContent(), StandardCharsets.UTF_8);
                    loadFile(file);
                } catch (IOException e) {
                    LOGGER.severe("Erro ao salvar novo arquivo: " + e.getMessage());
                }
            }
        }
    }

    public void openFileDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Apresentação Markdown");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Arquivos Markdown (*.md, *.markdown, *.txt)", "*.md", "*.markdown", "*.txt"),
            new FileChooser.ExtensionFilter("Todos os Arquivos (*.*)", "*.*")
        );
        if (loadedFile != null && loadedFile.getParentFile() != null && loadedFile.getParentFile().exists()) {
            fileChooser.setInitialDirectory(loadedFile.getParentFile());
        } else {
            fileChooser.setInitialDirectory(new File("."));
        }
        File selected = fileChooser.showOpenDialog(primaryStage);
        if (selected != null) {
            loadFile(selected);
        }
    }

    private void setupDragAndDrop(Scene scene) {
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> files = db.getFiles();
                for (File f : files) {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt")) {
                        loadFile(f);
                        success = true;
                        break;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void setupKeyHandlers(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Global shortcuts: Nova (Ctrl+N), Salvar (Ctrl+S), Abrir (Ctrl+O)
            if (event.isShortcutDown() && event.getCode() == KeyCode.N) {
                createNewPresentation();
                event.consume();
                return;
            }
            if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
                savePresentation();
                event.consume();
                return;
            }
            if (event.isShortcutDown() && event.getCode() == KeyCode.O) {
                openFileDialog();
                event.consume();
                return;
            }

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
