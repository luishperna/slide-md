package com.slidemd.ui;

import com.slidemd.model.Alignment;
import com.slidemd.model.Slide;
import com.slidemd.parser.MarkdownSlideParser;
import com.slidemd.video.FFmpegService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlideView extends StackPane {
    private static final Logger LOGGER = Logger.getLogger(SlideView.class.getName());

    private final Slide slide;
    private final File baseDir;
    private final FFmpegService ffmpegService;
    private final MarkdownSlideParser parser;

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private StackPane mediaContainer;
    private ProgressIndicator videoSpinner;
    private VBox contentBox;

    public SlideView(Slide slide, File baseDir, FFmpegService ffmpegService, MarkdownSlideParser parser) {
        this.slide = slide;
        this.baseDir = baseDir;
        this.ffmpegService = ffmpegService;
        this.parser = parser;

        buildSlide();
    }

    private void buildSlide() {
        getChildren().clear();

        // 1. Background Layer
        setupBackground();

        // 2. Video Layer (if configured)
        if (slide.hasVideo()) {
            setupVideo();
        }

        // 3. Content Layer (Markdown text, lists, images)
        contentBox = new VBox(12);
        contentBox.setAlignment(toPos(slide.getAlignment()));
        contentBox.setMaxWidth(1000);
        contentBox.setPadding(new Insets(50, 70, 50, 70));

        // Render Markdown content (only content, without directives)
        String markdownToRender = slide.getContentMarkdown();
        if (markdownToRender != null && !markdownToRender.isBlank()) {
            org.commonmark.node.Node ast = parser.parseMarkdownAst(markdownToRender);
            MarkdownNodeRenderer renderer = new MarkdownNodeRenderer(baseDir, slide.getAlignment());
            List<Node> renderedNodes = renderer.renderNodes(ast);
            contentBox.getChildren().addAll(renderedNodes);
        }

        // Add media container if present
        if (mediaContainer != null) {
            contentBox.getChildren().add(mediaContainer);
        }

        getChildren().add(contentBox);
        setAlignment(Pos.CENTER);
    }

    public VBox getContentBox() {
        return contentBox;
    }

    private void setupBackground() {
        if (slide.getBackgroundImage() != null && !slide.getBackgroundImage().isBlank()) {
            try {
                File imgFile = new File(slide.getBackgroundImage());
                if (!imgFile.isAbsolute() && baseDir != null) {
                    imgFile = new File(baseDir, slide.getBackgroundImage());
                }

                Image img;
                if (imgFile.exists()) {
                    img = new Image(imgFile.toURI().toString());
                } else {
                    img = new Image(slide.getBackgroundImage(), true);
                }

                ImageView bgImgView = new ImageView(img);
                bgImgView.fitWidthProperty().bind(widthProperty());
                bgImgView.fitHeightProperty().bind(heightProperty());
                bgImgView.setPreserveRatio(false);
                bgImgView.setOpacity(0.45); // Subtle darkening for high text readability
                getChildren().add(bgImgView);

                // Add dark overlay
                Region overlay = new Region();
                overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7);");
                getChildren().add(overlay);
                return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error loading background image", e);
            }
        }

        if (slide.getBackgroundColor() != null && !slide.getBackgroundColor().isBlank()) {
            setStyle("-fx-background-color: " + slide.getBackgroundColor() + ";");
        } else {
            // Default elegant deep dark gradient
            setStyle("-fx-background-color: linear-gradient(to bottom right, #090d16, #111827);");
        }
    }

    private void setupVideo() {
        mediaContainer = new StackPane();
        mediaContainer.setMaxSize(700, 390);
        mediaContainer.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 12px; -fx-padding: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 25, 0, 0, 8);");

        videoSpinner = new ProgressIndicator();
        videoSpinner.setMaxSize(50, 50);
        mediaContainer.getChildren().add(videoSpinner);

        mediaView = new MediaView();
        mediaView.setFitWidth(680);
        mediaView.setFitHeight(380);
        mediaView.setPreserveRatio(true);

        // Process asynchronously using FFmpegService (cuts, joins or raw)
        ffmpegService.processVideoAsync(slide.getVideoSpec(), baseDir).thenAccept(videoFile -> {
            Platform.runLater(() -> {
                if (videoFile != null && videoFile.exists()) {
                    try {
                        Media media = new Media(videoFile.toURI().toString());
                        mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.setAutoPlay(slide.getVideoSpec().isAutoplay());
                        if (slide.getVideoSpec().isLoop()) {
                            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        }

                        // Pure Java Native Virtual Cutting (Works 100% without FFmpeg)
                        Double startSec = slide.getVideoSpec().getCutStartSeconds();
                        if (startSec != null && startSec > 0) {
                            mediaPlayer.setStartTime(Duration.seconds(startSec));
                        }
                        Double endSec = slide.getVideoSpec().getCutEndSeconds();
                        if (endSec != null && endSec > 0) {
                            mediaPlayer.setStopTime(Duration.seconds(endSec));
                        }

                        mediaPlayer.setOnReady(() -> {
                            mediaContainer.getChildren().remove(videoSpinner);
                            if (!mediaContainer.getChildren().contains(mediaView)) {
                                mediaContainer.getChildren().add(mediaView);
                            }
                        });

                        mediaPlayer.setOnError(() -> {
                            mediaContainer.getChildren().remove(videoSpinner);
                            Label err = new Label("Erro ao reproduzir vídeo");
                            err.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                            mediaContainer.getChildren().add(err);
                        });

                        mediaView.setMediaPlayer(mediaPlayer);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to initialize MediaPlayer", e);
                        mediaContainer.getChildren().remove(videoSpinner);
                        Label err = new Label("Formato de vídeo não suportado pelo sistema");
                        err.setStyle("-fx-text-fill: #f87171;");
                        mediaContainer.getChildren().add(err);
                    }
                } else {
                    mediaContainer.getChildren().remove(videoSpinner);
                    Label err = new Label("Vídeo não encontrado");
                    err.setStyle("-fx-text-fill: #f87171;");
                    mediaContainer.getChildren().add(err);
                }
            });
        });
    }

    public void onSlideEnter() {
        if (mediaPlayer != null) {
            Double startSec = slide.getVideoSpec() != null ? slide.getVideoSpec().getCutStartSeconds() : null;
            if (startSec != null && startSec > 0) {
                mediaPlayer.seek(Duration.seconds(startSec));
            } else {
                mediaPlayer.seek(Duration.ZERO);
            }
            if (slide.getVideoSpec() != null && slide.getVideoSpec().isAutoplay()) {
                mediaPlayer.play();
            }
        }
    }

    public void onSlideLeave() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void dispose() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    public Slide getSlide() {
        return slide;
    }

    private Pos toPos(Alignment align) {
        return switch (align) {
            case LEFT -> Pos.CENTER_LEFT;
            case RIGHT -> Pos.CENTER_RIGHT;
            case CENTER -> Pos.CENTER;
        };
    }
}
