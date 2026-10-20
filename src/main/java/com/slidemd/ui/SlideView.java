package com.slidemd.ui;

import com.slidemd.model.Alignment;
import com.slidemd.model.Slide;
import com.slidemd.model.VideoSpec;
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
        contentBox = new VBox(10);
        contentBox.setAlignment(toPos(slide.getAlignment()));
        contentBox.setMaxWidth(1000);
        contentBox.setPadding(new Insets(24, 40, 24, 40));

        // Render Markdown content (only content, without directives)
        String markdownToRender = slide.getContentMarkdown();
        boolean videoInserted = false;
        if (markdownToRender != null && !markdownToRender.isBlank()) {
            org.commonmark.node.Node ast = parser.parseMarkdownAst(markdownToRender);
            MarkdownNodeRenderer renderer = new MarkdownNodeRenderer(baseDir, slide.getAlignment(), slide.getColor());
            List<Node> renderedNodes = renderer.renderNodes(ast);
            for (Node n : renderedNodes) {
                if (n instanceof MarkdownNodeRenderer.VideoPlaceholder) {
                    if (mediaContainer != null) {
                        contentBox.getChildren().add(mediaContainer);
                        videoInserted = true;
                    }
                } else {
                    contentBox.getChildren().add(n);
                }
            }
        }

        // Add media container if present and not already inserted inline
        if (mediaContainer != null && !videoInserted) {
            contentBox.getChildren().add(mediaContainer);
        }

        // Add directive image if configured and not already rendered inline
        if (slide.getImage() != null && !slide.getImage().isBlank()) {
            boolean hasImageView = contentBox.getChildren().stream().anyMatch(n -> n instanceof ImageView);
            if (!hasImageView) {
                Node imgNode = renderDirectiveImage(slide.getImage());
                if (imgNode != null) {
                    contentBox.getChildren().add(imgNode);
                }
            }
        }

        getChildren().add(contentBox);
        setAlignment(toPos(slide.getAlignment()));
        setMinSize(0, 0);
    }

    private Node renderDirectiveImage(String path) {
        try {
            File imgFile = new File(path);
            if (!imgFile.isAbsolute() && baseDir != null) {
                imgFile = new File(baseDir, path);
            }

            Image fxImg;
            if (imgFile.exists()) {
                fxImg = new Image(imgFile.toURI().toString());
            } else if (path.startsWith("http://") || path.startsWith("https://")) {
                fxImg = new Image(path, true);
            } else {
                return new Label("[Imagem não encontrada: " + path + "]");
            }

            ImageView iv = new ImageView(fxImg);
            iv.setPreserveRatio(true);
            iv.setFitWidth(650);
            iv.setFitHeight(400);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 6);");
            return iv;
        } catch (Exception e) {
            return new Label("[Erro ao carregar imagem: " + path + "]");
        }
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
                    loadMediaWithFallback(videoFile, false);
                } else {
                    mediaContainer.getChildren().remove(videoSpinner);
                    Label err = new Label("Vídeo não encontrado");
                    err.setStyle("-fx-text-fill: #f87171;");
                    mediaContainer.getChildren().add(err);
                }
            });
        });
    }

    private boolean isVirtualCut = false;

    private void loadMediaWithFallback(File videoFile, boolean isFallback) {
        try {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                } catch (Exception ignored) {}
                mediaPlayer = null;
            }

            File originalSource = VideoSpec.resolveFile(baseDir, slide.getVideoSpec().getSourcePath());
            // Se o arquivo for o original (FFmpeg não gerou corte físico em disco), usamos corte virtual JavaFX
            isVirtualCut = (videoFile != null && videoFile.equals(originalSource) && slide.getVideoSpec().hasCut());

            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setAutoPlay(false);
            mediaPlayer.setCycleCount(1);

            // Pure Java Native Virtual Cutting (apenas se não foi cortado fisicamente pelo FFmpeg)
            if (isVirtualCut) {
                Double startSec = slide.getVideoSpec().getCutStartSeconds();
                if (startSec != null && startSec > 0) {
                    mediaPlayer.setStartTime(Duration.seconds(startSec));
                }
                Double endSec = slide.getVideoSpec().getCutEndSeconds();
                if (endSec != null && endSec > 0) {
                    mediaPlayer.setStopTime(Duration.seconds(endSec));
                }
            }

            mediaPlayer.setOnReady(() -> {
                mediaContainer.getChildren().remove(videoSpinner);
                if (!mediaContainer.getChildren().contains(mediaView)) {
                    mediaContainer.getChildren().add(mediaView);
                }
                // Pré-carrega o primeiro quadro para exibir a miniatura imediatamente
                if (isVirtualCut) {
                    Double startSec = slide.getVideoSpec().getCutStartSeconds();
                    if (startSec != null && startSec > 0) {
                        mediaPlayer.seek(Duration.seconds(startSec));
                    } else {
                        mediaPlayer.seek(Duration.millis(50));
                    }
                } else {
                    mediaPlayer.seek(Duration.millis(50));
                }
            });

            Runnable handleMediaError = () -> {
                LOGGER.log(Level.WARNING, "Media playback failed on: " + videoFile.getName());
                if (!isFallback && ffmpegService.isAvailable()) {
                    LOGGER.info("Attempting automatic video sanitization via FFmpeg...");
                    File toSanitize = originalSource != null && originalSource.exists() ? originalSource : videoFile;
                    ffmpegService.sanitizeVideoAsync(toSanitize).thenAccept(sanitized -> {
                        Platform.runLater(() -> {
                            if (sanitized != null && sanitized.exists() && !sanitized.equals(videoFile)) {
                                loadMediaWithFallback(sanitized, true);
                                return;
                            }
                            displayVideoError();
                        });
                    });
                    return;
                }
                displayVideoError();
            };

            media.setOnError(handleMediaError);
            mediaPlayer.setOnError(handleMediaError);

            mediaView.setMediaPlayer(mediaPlayer);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize MediaPlayer", e);
            displayVideoError();
        }
    }

    private void displayVideoError() {
        mediaContainer.getChildren().remove(videoSpinner);
        Label err = new Label("Erro ao reproduzir vídeo");
        err.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        if (!mediaContainer.getChildren().contains(err)) {
            mediaContainer.getChildren().add(err);
        }
    }

    private boolean videoStarted = false;

    public boolean playVideoIfNotStarted() {
        if (mediaPlayer != null && !videoStarted) {
            videoStarted = true;
            if (isVirtualCut) {
                Double startSec = slide.getVideoSpec() != null ? slide.getVideoSpec().getCutStartSeconds() : null;
                if (startSec != null && startSec > 0) {
                    mediaPlayer.seek(Duration.seconds(startSec));
                }
            } else {
                mediaPlayer.seek(Duration.ZERO);
            }
            mediaPlayer.play();
            return true;
        }
        return false;
    }

    public void onSlideEnter() {
        videoStarted = false;
        if (mediaPlayer != null) {
            if (isVirtualCut) {
                Double startSec = slide.getVideoSpec() != null ? slide.getVideoSpec().getCutStartSeconds() : null;
                if (startSec != null && startSec > 0) {
                    mediaPlayer.seek(Duration.seconds(startSec));
                } else {
                    mediaPlayer.seek(Duration.ZERO);
                }
            } else {
                mediaPlayer.seek(Duration.ZERO);
            }
            mediaPlayer.pause();
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
            case TOP -> Pos.TOP_CENTER;
            case DOWN -> Pos.BOTTOM_CENTER;
            case CENTER -> Pos.CENTER;
        };
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        adjustContentScale();
    }

    private void adjustContentScale() {
        if (contentBox == null) return;

        double availWidth = getWidth();
        double availHeight = getHeight();
        if (availWidth <= 0 || availHeight <= 0) return;

        double contentWidth = contentBox.getLayoutBounds().getWidth();
        double contentHeight = contentBox.getLayoutBounds().getHeight();

        if (contentWidth <= 0 || contentHeight <= 0) return;

        // Margem de respiro de 30px
        double targetWidth = contentWidth + 30.0;
        double targetHeight = contentHeight + 30.0;

        double scaleX = availWidth / targetWidth;
        double scaleY = availHeight / targetHeight;
        double targetScale = Math.min(1.0, Math.min(scaleX, scaleY));

        targetScale = Math.max(0.3, targetScale);

        if (Math.abs(contentBox.getScaleX() - targetScale) > 0.005) {
            contentBox.setScaleX(targetScale);
            contentBox.setScaleY(targetScale);
        }
    }
}
