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

        incoming.setOpacity(0.0);
        incoming.setTranslateX(0);
        incoming.setTranslateY(0);

        outgoing.setTranslateX(0);
        outgoing.setTranslateY(0);

        ParallelTransition pt = new ParallelTransition();

        // 1. Outgoing slide: gentle upward or downward drift and fade out
        FadeTransition outFade = new FadeTransition(Duration.millis(260), outgoing);
        outFade.setToValue(0.0);
        outFade.setInterpolator(FLUID_INTERPOLATOR);
        pt.getChildren().add(outFade);

        if (outgoing.getContentBox() != null) {
            double outY = forward ? -40.0 : 40.0;
            TranslateTransition outSlide = new TranslateTransition(Duration.millis(260), outgoing.getContentBox());
            outSlide.setToY(outY);
            outSlide.setInterpolator(FLUID_INTERPOLATOR);
            pt.getChildren().add(outSlide);
        }

        // 2. Incoming slide background crossfade
        FadeTransition inFade = new FadeTransition(Duration.millis(320), incoming);
        inFade.setToValue(1.0);
        inFade.setInterpolator(FLUID_INTERPOLATOR);
        pt.getChildren().add(inFade);

        // 3. Staggered cascade entrance for incoming slide elements (PowerPoint Morph effect)
        if (incoming.getContentBox() != null) {
            var children = incoming.getContentBox().getChildren();
            double inStartY = forward ? 45.0 : -45.0;
            incoming.getContentBox().setTranslateY(0);

            for (int i = 0; i < children.size(); i++) {
                javafx.scene.Node node = children.get(i);
                node.setTranslateY(inStartY);
                node.setOpacity(0.0);
                node.setScaleX(0.95);
                node.setScaleY(0.95);

                Duration delay = Duration.millis(Math.min(i * 45, 180));

                TranslateTransition tt = new TranslateTransition(Duration.millis(380), node);
                tt.setToY(0);
                tt.setInterpolator(FLUID_INTERPOLATOR);
                tt.setDelay(delay);

                FadeTransition ft = new FadeTransition(Duration.millis(320), node);
                ft.setToValue(1.0);
                ft.setInterpolator(FLUID_INTERPOLATOR);
                ft.setDelay(delay);

                ScaleTransition st = new ScaleTransition(Duration.millis(380), node);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(FLUID_INTERPOLATOR);
                st.setDelay(delay);

                pt.getChildren().addAll(tt, ft, st);
            }
        }

        pt.setOnFinished(e -> {
            container.getChildren().remove(outgoing);
            outgoing.setOpacity(1.0);
            if (outgoing.getContentBox() != null) {
                outgoing.getContentBox().setTranslateY(0);
            }
            if (incoming.getContentBox() != null) {
                for (javafx.scene.Node node : incoming.getContentBox().getChildren()) {
                    node.setTranslateY(0);
                    node.setOpacity(1.0);
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                }
            }
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
