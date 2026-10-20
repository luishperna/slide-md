package com.slidemd;

import com.slidemd.animation.TransitionManager;
import com.slidemd.audio.AudioManager;
import com.slidemd.model.CommandLineOptions;
import com.slidemd.model.RecentFilesManager;
import com.slidemd.model.Slide;
import com.slidemd.model.SlideDeck;
import com.slidemd.parser.MarkdownSlideParser;
import com.slidemd.ui.AppTitleBar;
import com.slidemd.ui.LaserPenOverlay;
import com.slidemd.ui.SideEditorPanel;
import com.slidemd.ui.SlideEditorHelper;
import com.slidemd.ui.SlideView;
import com.slidemd.ui.WelcomeView;
import com.slidemd.ui.WindowResizeHelper;
import com.slidemd.video.FFmpegService;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.util.Duration;
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
    private final AudioManager audioManager = new AudioManager();

    private Stage primaryStage;
    private AppTitleBar appTitleBar;
    private StackPane rootLayout;
    private StackPane slideContainer;
    private LaserPenOverlay laserPenOverlay;
    private StackPane presentationArea;
    private SplitPane splitPane;
    private SideEditorPanel editorPanel;
    private double lastDividerPosition = 0.62;
    private Transition editorTransition;

    private SlideDeck currentDeck;
    private int currentSlideIndex = 0;
    private SlideView currentSlideView;
    private File loadedFile;

    private String cliMusicPath;
    private boolean cliMusicLoop = true;
    private double cliMusicVolume = 0.8;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setFullScreenExitHint("");

        BorderPane mainContainer = new BorderPane();
        mainContainer.setStyle("-fx-background-color: #101010;");
        mainContainer.setMinSize(0, 0);

        laserPenOverlay = new LaserPenOverlay(this::prevSlide, this::nextSlide);

        // Custom Top Title Bar with Centralized Dock and Navigation
        appTitleBar = new AppTitleBar(stage, null,
                this::createNewPresentation, this::openFileDialog, this::toggleEditor,
                this::toggleLaserPen,
                this::prevSlide, this::nextSlide);
        mainContainer.setTop(appTitleBar);

        rootLayout = new StackPane();
        rootLayout.setStyle("-fx-background-color: #101010;");
        rootLayout.setMinSize(0, 0);
        mainContainer.setCenter(rootLayout);

        // Slide Container with Click Navigation (left half = back, right half = next)
        slideContainer = new StackPane();
        slideContainer.setAlignment(Pos.CENTER);
        slideContainer.setMinSize(0, 0);
        slideContainer.setOnMouseClicked(event -> {
            if (laserPenOverlay != null && laserPenOverlay.isActive()) return;
            if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
            if (event.getButton() != MouseButton.PRIMARY) return;

            double width = slideContainer.getWidth();
            if (event.getX() < width / 2.0) {
                prevSlide();
            } else {
                nextSlide();
            }
        });

        presentationArea = new StackPane(slideContainer, laserPenOverlay);
        presentationArea.setAlignment(Pos.CENTER);
        presentationArea.setMinSize(0, 0);

        // Setup Side Editor Panel integrated into the window (VS Code sidebar style)
        editorPanel = new SideEditorPanel(
                this::onEditorContentChanged,
                this::savePresentation,
                this::onEditorSlideNavigated,
                this::closeEditor
        );

        splitPane = new SplitPane();
        splitPane.setStyle("-fx-box-border: transparent; -fx-background-color: #101010; -fx-padding: 0;");
        splitPane.setMinSize(0, 0);
        SplitPane.setResizableWithParent(presentationArea, true);
        SplitPane.setResizableWithParent(editorPanel.getPanelNode(), false);

        // Fecha o editor se o usuário soltar o divisor perto da borda direita
        splitPane.setOnMouseReleased(event -> {
            if (isEditorOpen() && editorTransition == null) {
                double[] pos = splitPane.getDividerPositions();
                if (pos.length > 0 && pos[0] >= 0.92) {
                    closeEditor();
                }
            }
        });

        // Fecha o editor se o painel for arrastado/espremido para menos de 60px de largura
        editorPanel.getPanelNode().widthProperty().addListener((obs, oldW, newW) -> {
            if (isEditorOpen() && editorTransition == null && newW.doubleValue() > 0 && newW.doubleValue() < 60) {
                closeEditor();
            }
        });

        splitPane.getItems().setAll(presentationArea);
        rootLayout.getChildren().add(splitPane);

        StackPane windowRoot = new StackPane(mainContainer);
        Scene scene = new Scene(windowRoot, 1280, 720);
        try {
            var cssUrl = getClass().getResource("/css/markdown-editor.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {}
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
        stage.setMinWidth(640);
        stage.setMinHeight(340);

        // Open Welcome Screen by default, or load CLI argument file
        loadInitialPresentation();

        stage.show();
    }

    private void loadInitialPresentation() {
        var rawArgs = getParameters().getRaw();
        var namedArgs = getParameters().getNamed();
        CommandLineOptions options = CommandLineOptions.parse(rawArgs, namedArgs);

        if (options.musicPath() != null && !options.musicPath().isBlank()) {
            this.cliMusicPath = options.musicPath();
            this.cliMusicLoop = options.musicLoop();
            this.cliMusicVolume = options.musicVolume();
        }

        if (options.markdownFile() != null && options.markdownFile().exists()) {
            loadFile(options.markdownFile());
            return;
        }

        showWelcomeScreen();

        if (this.cliMusicPath != null) {
            audioManager.syncMusic(this.cliMusicPath, new File("."), this.cliMusicLoop, this.cliMusicVolume);
        }
    }

    public void showWelcomeScreen() {
        this.currentDeck = null;
        this.loadedFile = null;
        if (laserPenOverlay != null && laserPenOverlay.isActive()) {
            toggleLaserPen();
        }
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
        if (cliMusicPath == null) {
            audioManager.stop();
        }
        WelcomeView welcomeView = new WelcomeView(this::createNewPresentation, this::openFileDialog, this::loadFile);
        slideContainer.getChildren().setAll(welcomeView);
        slideContainer.setCursor(Cursor.DEFAULT);
    }

    public void loadFile(File file) {
        try {
            this.loadedFile = file;
            this.currentDeck = parser.parseFile(file);
            RecentFilesManager.addRecentFile(file);
            primaryStage.setTitle("slide-md — " + currentDeck.getTitle());
            if (appTitleBar != null) {
                appTitleBar.setTitle(currentDeck.getTitle());
                appTitleBar.setNavigationVisible(true);
            }
            editorPanel.setMarkdownContent(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            currentSlideIndex = 0;
            slideContainer.setCursor(Cursor.HAND);
            displaySlide(currentSlideIndex, false, true);
            if (isEditorOpen()) {
                editorPanel.scrollToSlide(0);
            }
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

            int editingSlide = SlideEditorHelper.getSlideIndexAtCaret(newMarkdown, editorPanel.getCaretPosition());
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

        if (laserPenOverlay != null) {
            laserPenOverlay.clear();
        }

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

        // Sincroniza reprodução de áudio / música de fundo
        updateAudioPlayback(targetSlide, baseDir);
    }

    public void toggleLaserPen() {
        if (laserPenOverlay == null) return;
        boolean newState = !laserPenOverlay.isActive();
        laserPenOverlay.setActive(newState);
        if (appTitleBar != null) {
            appTitleBar.setPenActive(newState);
        }
    }

    private void updateAudioPlayback(Slide targetSlide, File baseDir) {
        String effectiveMusic = null;
        boolean loop = true;
        double volume = 1.0;

        if (targetSlide != null && targetSlide.getMusic() != null) {
            effectiveMusic = targetSlide.getMusic();
            loop = targetSlide.isMusicLoop() != null ? targetSlide.isMusicLoop() : true;
            volume = targetSlide.getMusicVolume() != null ? targetSlide.getMusicVolume() : 1.0;
        } else if (currentDeck != null && currentDeck.getMusic() != null) {
            effectiveMusic = currentDeck.getMusic();
            loop = currentDeck.isMusicLoop();
            volume = currentDeck.getMusicVolume();
        } else if (cliMusicPath != null && !cliMusicPath.isBlank()) {
            effectiveMusic = cliMusicPath;
            loop = cliMusicLoop;
            volume = cliMusicVolume;
        }

        audioManager.syncMusic(effectiveMusic, baseDir, loop, volume);
    }

    public void nextSlide() {
        if (transitionManager.isAnimating()) return;
        if (currentSlideView != null && currentSlideView.playVideoIfNotStarted()) {
            return;
        }
        if (currentDeck != null && currentSlideIndex < currentDeck.getSlideCount() - 1) {
            currentSlideIndex++;
            displaySlide(currentSlideIndex, true, true);
            if (editorPanel != null && isEditorOpen()) {
                editorPanel.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void prevSlide() {
        if (transitionManager.isAnimating()) return;
        if (currentDeck != null && currentSlideIndex > 0) {
            currentSlideIndex--;
            displaySlide(currentSlideIndex, true, false);
            if (editorPanel != null && isEditorOpen()) {
                editorPanel.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void firstSlide() {
        if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
        if (currentSlideIndex != 0) {
            currentSlideIndex = 0;
            displaySlide(currentSlideIndex, true, false);
            if (editorPanel != null && isEditorOpen()) {
                editorPanel.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void lastSlide() {
        if (currentDeck == null || currentDeck.getSlideCount() == 0) return;
        int last = currentDeck.getSlideCount() - 1;
        if (currentSlideIndex != last) {
            currentSlideIndex = last;
            displaySlide(currentSlideIndex, true, true);
            if (editorPanel != null && isEditorOpen()) {
                editorPanel.scrollToSlide(currentSlideIndex);
            }
        }
    }

    public void toggleEditor() {
        toggleEditor(currentSlideIndex);
    }

    public void toggleEditor(int slideIndex) {
        if (isEditorOpen()) {
            closeEditor();
        } else {
            openEditor(slideIndex);
        }
    }

    public boolean isEditorOpen() {
        return splitPane != null && splitPane.getItems().contains(editorPanel.getPanelNode());
    }

    public void openEditor(int slideIndex) {
        if (splitPane != null && !isEditorOpen()) {
            if (editorTransition != null) {
                editorTransition.stop();
            }

            splitPane.getItems().setAll(presentationArea, editorPanel.getPanelNode());
            splitPane.applyCss();
            splitPane.layout();
            splitPane.setDividerPosition(0, 1.0);

            double targetPos = (lastDividerPosition >= 0.25 && lastDividerPosition <= 0.85) ? lastDividerPosition : 0.62;

            editorTransition = new Transition() {
                {
                    setCycleDuration(Duration.millis(140));
                    setInterpolator(Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0));
                }

                @Override
                protected void interpolate(double frac) {
                    double current = 1.0 - frac * (1.0 - targetPos);
                    splitPane.setDividerPosition(0, current);
                }
            };

            editorTransition.setOnFinished(e -> {
                editorTransition = null;
                splitPane.setDividerPosition(0, targetPos);
                splitPane.applyCss();
                splitPane.layout();
                editorPanel.onOpened(slideIndex >= 0 ? slideIndex : currentSlideIndex);

                if (!splitPane.getDividers().isEmpty()) {
                    SplitPane.Divider divider = splitPane.getDividers().get(0);
                    divider.positionProperty().addListener((obs, oldP, newP) -> {
                        if (isEditorOpen() && editorTransition == null) {
                            double p = newP.doubleValue();
                            if (p >= 0.94) {
                                closeEditor();
                            } else if (p >= 0.20 && p <= 0.85) {
                                lastDividerPosition = p;
                            }
                        }
                    });
                }
            });

            editorTransition.play();
        } else if (isEditorOpen() && slideIndex >= 0) {
            editorPanel.scrollToSlide(slideIndex);
        }
    }

    public void closeEditor() {
        if (isEditorOpen()) {
            if (editorTransition != null) {
                editorTransition.stop();
            }

            double[] pos = splitPane.getDividerPositions();
            double startPos = (pos.length > 0 && pos[0] >= 0.25 && pos[0] <= 0.85) ? pos[0] : lastDividerPosition;
            if (pos.length > 0 && pos[0] >= 0.25 && pos[0] <= 0.85) {
                lastDividerPosition = pos[0];
            }

            editorTransition = new Transition() {
                {
                    setCycleDuration(Duration.millis(110));
                    setInterpolator(Interpolator.SPLINE(0.8, 0.0, 0.9, 0.2));
                }

                @Override
                protected void interpolate(double frac) {
                    double current = startPos + frac * (1.0 - startPos);
                    splitPane.setDividerPosition(0, current);
                }
            };

            editorTransition.setOnFinished(e -> {
                editorTransition = null;
                editorPanel.onClosed();
                splitPane.getItems().setAll(presentationArea);
                presentationArea.requestFocus();
            });

            editorTransition.play();
        }
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
                background: [#090d16, #1e293b]
                alignment: center
                transition: morph
                ---

                # Nova Apresentação
                Edite seu conteúdo em tempo real

                Pressione **M** para abrir o editor

                ---
                background: #0f172a
                alignment: left
                transition: morph

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
        editorPanel.setMarkdownContent(newTemplate);
        currentSlideIndex = 0;
        slideContainer.setCursor(Cursor.HAND);
        displaySlide(0, false, true);
        openEditor(0);
    }

    public void savePresentation() {
        if (loadedFile != null && loadedFile.exists()) {
            try {
                Files.writeString(loadedFile.toPath(), editorPanel.getMarkdownContent(), StandardCharsets.UTF_8);
                RecentFilesManager.addRecentFile(loadedFile);
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
                    Files.writeString(file.toPath(), editorPanel.getMarkdownContent(), StandardCharsets.UTF_8);
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
            // Se o foco está no editor e não for atalho com Ctrl, deixa o editor processar digitação normalmente
            if (editorPanel != null && editorPanel.isFocused()) {
                if (event.isShortcutDown() && event.getCode() == KeyCode.S) {
                    savePresentation();
                    event.consume();
                } else if (event.isShortcutDown() && event.getCode() == KeyCode.O) {
                    openFileDialog();
                    event.consume();
                } else if (event.isShortcutDown() && event.getCode() == KeyCode.N) {
                    createNewPresentation();
                    event.consume();
                }
                return;
            }

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
                if (laserPenOverlay != null && laserPenOverlay.isActive()) {
                    toggleLaserPen();
                    event.consume();
                } else if (isEditorOpen()) {
                    closeEditor();
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
                case P -> {
                    toggleLaserPen();
                    event.consume();
                }
                case C -> {
                    if (laserPenOverlay != null && laserPenOverlay.isActive()) {
                        laserPenOverlay.clear();
                        event.consume();
                    }
                }
                case R -> {
                    reloadPresentation();
                    event.consume();
                }
                case U -> {
                    audioManager.toggleMute();
                    event.consume();
                }
                default -> {}
            }
        });
    }

    @Override
    public void stop() {
        if (audioManager != null) {
            audioManager.dispose();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
