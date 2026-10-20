package com.slidemd.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Overlay interativo de caneta e rastro laser para apresentações.
 * Renderiza apenas o rastro fluído e contínuo (sem bolinhas/pontos soltos),
 * com a cauda diminuindo suavemente a espessura e a transparência.
 * Permite também cliques simples para avançar ou voltar slides normalmente.
 */
public class LaserPenOverlay extends Pane {

    private static final class Point {
        final double x;
        final double y;
        final long timestamp;
        final long maxLifeMs;

        Point(double x, double y, long timestamp, long maxLifeMs) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
            this.maxLifeMs = maxLifeMs;
        }
    }

    private static final class Stroke {
        final List<Point> points = new ArrayList<>();
        final boolean isDrawing;

        Stroke(boolean isDrawing) {
            this.isDrawing = isDrawing;
        }
    }

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final List<Stroke> strokes = new ArrayList<>();

    private Runnable onPrevSlide;
    private Runnable onNextSlide;

    private Stroke currentStroke = null;
    private boolean active = false;
    private boolean isMouseDown = false;

    private double pressStartX;
    private double pressStartY;
    private long pressStartTime;
    private boolean hasDragged = false;

    private final AnimationTimer renderLoop;

    public LaserPenOverlay() {
        this(null, null);
    }

    public LaserPenOverlay(Runnable onPrevSlide, Runnable onNextSlide) {
        this.onPrevSlide = onPrevSlide;
        this.onNextSlide = onNextSlide;

        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        getChildren().add(canvas);
        setPickOnBounds(false);
        setMouseTransparent(true);
        setVisible(false);

        setupMouseHandlers();

        this.renderLoop = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                render();
            }
        };
    }

    public void setNavigationHandlers(Runnable onPrevSlide, Runnable onNextSlide) {
        this.onPrevSlide = onPrevSlide;
        this.onNextSlide = onNextSlide;
    }

    private void setupMouseHandlers() {
        setOnMouseExited(e -> {
            if (!active) return;
            if (!isMouseDown) {
                currentStroke = null;
            }
        });

        setOnMouseMoved(e -> {
            if (!active) return;
            e.consume();
            double x = e.getX();
            double y = e.getY();

            // Rastro contínuo ágil ao mover o ponteiro
            if (currentStroke == null || currentStroke.isDrawing) {
                currentStroke = new Stroke(false);
                strokes.add(currentStroke);
            }
            appendPoint(currentStroke, x, y, 350);
        });

        setOnMousePressed(e -> {
            if (!active) return;
            e.consume();

            // Botão direito limpa as anotações instantaneamente
            if (e.getButton() == MouseButton.SECONDARY) {
                clear();
                return;
            }

            if (e.getButton() == MouseButton.PRIMARY) {
                isMouseDown = true;
                hasDragged = false;
                pressStartX = e.getX();
                pressStartY = e.getY();
                pressStartTime = System.currentTimeMillis();

                currentStroke = new Stroke(true);
                strokes.add(currentStroke);
                appendPoint(currentStroke, e.getX(), e.getY(), 1200);
            }
        });

        setOnMouseDragged(e -> {
            if (!active) return;
            e.consume();
            if (!isMouseDown) return;

            double x = e.getX();
            double y = e.getY();

            if (Math.hypot(x - pressStartX, y - pressStartY) > 6.0) {
                hasDragged = true;
            }

            if (currentStroke == null || !currentStroke.isDrawing) {
                currentStroke = new Stroke(true);
                strokes.add(currentStroke);
            }
            appendPoint(currentStroke, x, y, 1200);
        });

        setOnMouseReleased(e -> {
            if (!active) return;
            e.consume();
            if (e.getButton() == MouseButton.PRIMARY) {
                isMouseDown = false;
                long elapsed = System.currentTimeMillis() - pressStartTime;

                // Se o usuário deu um clique simples (sem arraste significativo): muda de slide!
                if (!hasDragged && elapsed < 400) {
                    if (currentStroke != null) {
                        strokes.remove(currentStroke);
                    }
                    double width = getWidth();
                    if (pressStartX < width / 2.0) {
                        if (onPrevSlide != null) onPrevSlide.run();
                    } else {
                        if (onNextSlide != null) onNextSlide.run();
                    }
                }

                currentStroke = null;
            }
        });
    }

    private void appendPoint(Stroke stroke, double x, double y, long lifeMs) {
        long now = System.currentTimeMillis();
        if (!stroke.points.isEmpty()) {
            Point last = stroke.points.get(stroke.points.size() - 1);
            double distSq = (x - last.x) * (x - last.x) + (y - last.y) * (y - last.y);
            // Amostras próximas o suficiente para fluidez suave sem sobrecarga
            if (distSq < 3.0) return;
        }
        stroke.points.add(new Point(x, y, now, lifeMs));
    }

    /**
     * Alterna ou define a ativação da caneta laser.
     */
    public void setActive(boolean active) {
        this.active = active;
        setMouseTransparent(!active);
        setVisible(active);

        if (active) {
            setCursor(Cursor.NONE);
            renderLoop.start();
        } else {
            setCursor(Cursor.DEFAULT);
            isMouseDown = false;
            currentStroke = null;
            clear();
            renderLoop.stop();
        }
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Limpa imediatamente todos os traços do rastro.
     */
    public void clear() {
        strokes.clear();
        currentStroke = null;
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0) {
            gc.clearRect(0, 0, w, h);
        }
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);
        long now = System.currentTimeMillis();

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        Iterator<Stroke> strokeIter = strokes.iterator();
        while (strokeIter.hasNext()) {
            Stroke stroke = strokeIter.next();
            List<Point> pts = stroke.points;

            // Remove pontos que já expiraram
            pts.removeIf(p -> (now - p.timestamp) > p.maxLifeMs);

            if (pts.isEmpty()) {
                if (stroke != currentStroke) {
                    strokeIter.remove();
                }
                continue;
            }

            int n = pts.size();
            // Apenas renderiza linhas contínuas entre pontos (zero bolinhas soltas)
            if (n < 2) {
                continue;
            }

            // Renderiza os segmentos conectando os pontos de forma contínua
            for (int i = 0; i < n - 1; i++) {
                Point pA = pts.get(i);
                Point pB = pts.get(i + 1);

                // Posição relativa na cauda (0.0 = cauda mais antiga, 1.0 = cabeça mais recente)
                double tailRatio = (double) i / (n - 1);

                // Vida útil restante dos pontos
                double ageA = Math.max(0.0, 1.0 - (double) (now - pA.timestamp) / pA.maxLifeMs);
                double ageB = Math.max(0.0, 1.0 - (double) (now - pB.timestamp) / pB.maxLifeMs);
                double lifeFactor = (ageA + ageB) / 2.0;

                // Opacidade progressiva da cabeça até a cauda
                double alpha = Math.min(tailRatio, lifeFactor);
                alpha = Math.pow(alpha, 1.15);

                if (alpha <= 0.01) continue;

                // Espessura afilando suavemente em direção à cauda
                double baseWidth = stroke.isDrawing ? 4.8 : 3.8;
                double strokeWidth = 1.0 + (baseWidth - 1.0) * alpha;

                // 1. Passada de Glow sutil suave e mais claro
                gc.setLineWidth(strokeWidth * 2.4);
                gc.setStroke(Color.color(1.0, 1.0, 1.0, Math.min(0.35, alpha * 0.40)));
                gc.strokeLine(pA.x, pA.y, pB.x, pB.y);

                // 2. Passada do Núcleo branco limpo de alta intensidade
                gc.setLineWidth(strokeWidth);
                gc.setStroke(Color.color(1.0, 1.0, 1.0, Math.min(1.0, alpha * 1.25)));
                gc.strokeLine(pA.x, pA.y, pB.x, pB.y);
            }
        }
    }
}
