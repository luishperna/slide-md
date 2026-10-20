package com.slidemd.animation;

import com.slidemd.ui.SlideView;
import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class TransitionManager {

    private static final Duration DEFAULT_DURATION = Duration.millis(320);
    private static final Interpolator FLUID_INTERPOLATOR = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

    private boolean isAnimating = false;

    public boolean isAnimating() {
        return isAnimating;
    }

    public void transition(Pane container, SlideView outgoing, SlideView incoming, boolean forward, String type, Runnable onFinished) {
        if (incoming == null) return;

        if (outgoing == null) {
            // Initial slide display, no transition needed
            container.getChildren().setAll(incoming);
            incoming.setOpacity(1.0);
            incoming.setTranslateX(0);
            incoming.setScaleX(1.0);
            incoming.setScaleY(1.0);
            incoming.onSlideEnter();
            if (onFinished != null) onFinished.run();
            return;
        }

        isAnimating = true;
        outgoing.onSlideLeave();

        double width = container.getWidth() > 0 ? container.getWidth() : 1280;
        double direction = forward ? 1.0 : -1.0;

        String transitionType = type != null ? type.toLowerCase() : "slide";

        switch (transitionType) {
            case "fade" -> executeFadeTransition(container, outgoing, incoming, onFinished);
            case "zoom" -> executeZoomTransition(container, outgoing, incoming, forward, onFinished);
            case "morph", "transform", "transformar" -> executeMorphTransition(container, outgoing, incoming, forward, onFinished);
            default -> executeSlideTransition(container, outgoing, incoming, width, direction, onFinished);
        }
    }

    private void executeMorphTransition(Pane container, SlideView outgoing, SlideView incoming, boolean forward, Runnable onFinished) {
        if (!container.getChildren().contains(incoming)) {
            container.getChildren().add(incoming);
        }

        // Clip retangular para conter os elementos dentro da área da apresentação
        if (container.getClip() == null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(container.widthProperty());
            clip.heightProperty().bind(container.heightProperty());
            container.setClip(clip);
        }

        double width = container.getWidth() > 0 ? container.getWidth() : 1280;
        double direction = forward ? 1.0 : -1.0;
        double travelDistance = width + 150.0;

        // O FUNDO FICA FIXO: nenhum slide desliza por inteiro
        outgoing.setTranslateX(0);
        outgoing.setTranslateY(0);
        outgoing.setScaleX(1.0);
        outgoing.setScaleY(1.0);

        incoming.setTranslateX(0);
        incoming.setTranslateY(0);
        incoming.setScaleX(1.0);
        incoming.setScaleY(1.0);

        // Fundo do novo slide faz um crossfade suave
        incoming.setOpacity(0.0);
        FadeTransition bgFade = new FadeTransition(Duration.millis(480), incoming);
        bgFade.setFromValue(0.0);
        bgFade.setToValue(1.0);
        bgFade.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pt = new ParallelTransition(bgFade);
        Interpolator morphSpline = Interpolator.SPLINE(0.15, 0.95, 0.25, 1.0);

        // 1. Elementos do slide atual: ANDAM para fora do slide (-direction * travelDistance)
        if (outgoing.getContentBox() != null) {
            var outChildren = outgoing.getContentBox().getChildren();
            double outTargetX = -direction * travelDistance;

            for (int i = 0; i < outChildren.size(); i++) {
                javafx.scene.Node node = outChildren.get(i);
                Duration delay = Duration.millis(Math.min(i * 28, 140));

                TranslateTransition tt = new TranslateTransition(Duration.millis(600), node);
                tt.setToX(outTargetX);
                tt.setInterpolator(morphSpline);
                tt.setDelay(delay);

                FadeTransition ft = new FadeTransition(Duration.millis(480), node);
                ft.setToValue(0.0);
                ft.setDelay(delay);

                pt.getChildren().addAll(tt, ft);
            }
        }

        // 2. Elementos do novo slide: VÊM DE FORA DO SLIDE (+direction * travelDistance) e andam para a tela
        if (incoming.getContentBox() != null) {
            var inChildren = incoming.getContentBox().getChildren();
            double inStartX = direction * travelDistance;

            for (int i = 0; i < inChildren.size(); i++) {
                javafx.scene.Node node = inChildren.get(i);
                node.setTranslateX(inStartX);
                node.setOpacity(0.0);

                Duration delay = Duration.millis(80 + Math.min(i * 40, 240));

                TranslateTransition tt = new TranslateTransition(Duration.millis(680), node);
                tt.setToX(0);
                tt.setInterpolator(morphSpline);
                tt.setDelay(delay);

                FadeTransition ft = new FadeTransition(Duration.millis(520), node);
                ft.setToValue(1.0);
                ft.setDelay(delay);

                pt.getChildren().addAll(tt, ft);
            }
        }

        pt.setOnFinished(e -> {
            container.getChildren().remove(outgoing);
            outgoing.setOpacity(1.0);
            outgoing.setTranslateX(0);

            // Reseta posições e opacidades dos elementos do slide que saiu
            if (outgoing.getContentBox() != null) {
                outgoing.getContentBox().setTranslateX(0);
                for (javafx.scene.Node node : outgoing.getContentBox().getChildren()) {
                    node.setTranslateX(0);
                    node.setOpacity(1.0);
                }
            }

            // Garante posições e opacidades normais dos elementos do slide que entrou
            if (incoming.getContentBox() != null) {
                incoming.getContentBox().setTranslateX(0);
                for (javafx.scene.Node node : incoming.getContentBox().getChildren()) {
                    node.setTranslateX(0);
                    node.setOpacity(1.0);
                }
            }

            incoming.setOpacity(1.0);
            incoming.onSlideEnter();
            isAnimating = false;
            if (onFinished != null) onFinished.run();
        });

        pt.play();
    }

    private void executeSlideTransition(Pane container, SlideView outgoing, SlideView incoming, double width, double direction, Runnable onFinished) {
        if (!container.getChildren().contains(incoming)) {
            container.getChildren().add(incoming);
        }

        incoming.setTranslateX(direction * width);
        incoming.setOpacity(1.0);

        TranslateTransition outT = new TranslateTransition(DEFAULT_DURATION, outgoing);
        outT.setToX(-direction * (width * 0.4));
        outT.setInterpolator(FLUID_INTERPOLATOR);

        FadeTransition outF = new FadeTransition(DEFAULT_DURATION, outgoing);
        outF.setToValue(0.4);

        TranslateTransition inT = new TranslateTransition(DEFAULT_DURATION, incoming);
        inT.setToX(0);
        inT.setInterpolator(FLUID_INTERPOLATOR);

        ParallelTransition pt = new ParallelTransition(outT, outF, inT);
        pt.setOnFinished(e -> {
            container.getChildren().remove(outgoing);
            outgoing.setTranslateX(0);
            outgoing.setOpacity(1.0);
            incoming.onSlideEnter();
            isAnimating = false;
            if (onFinished != null) onFinished.run();
        });
        pt.play();
    }

    private void executeFadeTransition(Pane container, SlideView outgoing, SlideView incoming, Runnable onFinished) {
        if (!container.getChildren().contains(incoming)) {
            container.getChildren().add(incoming);
        }

        incoming.setOpacity(0.0);
        incoming.setTranslateX(0);

        FadeTransition outF = new FadeTransition(DEFAULT_DURATION, outgoing);
        outF.setToValue(0.0);
        outF.setInterpolator(FLUID_INTERPOLATOR);

        FadeTransition inF = new FadeTransition(DEFAULT_DURATION, incoming);
        inF.setToValue(1.0);
        inF.setInterpolator(FLUID_INTERPOLATOR);

        ParallelTransition pt = new ParallelTransition(outF, inF);
        pt.setOnFinished(e -> {
            container.getChildren().remove(outgoing);
            outgoing.setOpacity(1.0);
            incoming.onSlideEnter();
            isAnimating = false;
            if (onFinished != null) onFinished.run();
        });
        pt.play();
    }

    private void executeZoomTransition(Pane container, SlideView outgoing, SlideView incoming, boolean forward, Runnable onFinished) {
        if (!container.getChildren().contains(incoming)) {
            container.getChildren().add(incoming);
        }

        incoming.setScaleX(forward ? 0.92 : 1.08);
        incoming.setScaleY(forward ? 0.92 : 1.08);
        incoming.setOpacity(0.0);

        ScaleTransition outScale = new ScaleTransition(DEFAULT_DURATION, outgoing);
        outScale.setToX(forward ? 1.08 : 0.92);
        outScale.setToY(forward ? 1.08 : 0.92);

        FadeTransition outF = new FadeTransition(DEFAULT_DURATION, outgoing);
        outF.setToValue(0.0);

        ScaleTransition inScale = new ScaleTransition(DEFAULT_DURATION, incoming);
        inScale.setToX(1.0);
        inScale.setToY(1.0);

        FadeTransition inF = new FadeTransition(DEFAULT_DURATION, incoming);
        inF.setToValue(1.0);

        ParallelTransition pt = new ParallelTransition(outScale, outF, inScale, inF);
        pt.setInterpolator(FLUID_INTERPOLATOR);
        pt.setOnFinished(e -> {
            container.getChildren().remove(outgoing);
            outgoing.setScaleX(1.0);
            outgoing.setScaleY(1.0);
            outgoing.setOpacity(1.0);
            incoming.onSlideEnter();
            isAnimating = false;
            if (onFinished != null) onFinished.run();
        });
        pt.play();
    }
}
