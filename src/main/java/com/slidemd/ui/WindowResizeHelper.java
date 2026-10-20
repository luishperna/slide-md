package com.slidemd.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * Utilitário para redimensionamento suave e preciso de janelas JavaFX sem borda (UNDECORATED).
 * Suporta redimensionamento através dos 4 cantos (diagonal com setas bidirecionais para fora)
 * e das 4 bordas (horizontal/vertical), utilizando um overlay de topo com prioridade visual
 * e event filters para garantir máxima estabilidade e compatibilidade.
 */
public class WindowResizeHelper {

    public static final int BORDER_THICKNESS = 8;
    public static final int CORNER_THICKNESS = 18;

    public record WindowBounds(double x, double y, double width, double height) {}

    public static void addResizeListener(Stage stage, Scene scene) {
        if (scene.getRoot() instanceof Pane pane) {
            ResizeOverlay overlay = new ResizeOverlay(stage);
            pane.getChildren().add(overlay);
        }

        ResizeListener listener = new ResizeListener(stage);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, listener);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, listener);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, listener);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, listener);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, listener);
        scene.addEventFilter(MouseEvent.MOUSE_EXITED, listener);
    }

    /**
     * Determina o cursor apropriado com base nas coordenadas do mouse na cena.
     */
    public static Cursor calculateCursor(double mouseX, double mouseY, double sceneWidth, double sceneHeight,
                                         int borderThickness, int cornerThickness) {
        if (sceneWidth <= 0 || sceneHeight <= 0) {
            return Cursor.DEFAULT;
        }

        if (mouseX < -borderThickness || mouseY < -borderThickness ||
            mouseX > sceneWidth + borderThickness || mouseY > sceneHeight + borderThickness) {
            return Cursor.DEFAULT;
        }

        boolean leftCorner = mouseX < cornerThickness;
        boolean rightCorner = mouseX > sceneWidth - cornerThickness;
        boolean topCorner = mouseY < cornerThickness;
        boolean bottomCorner = mouseY > sceneHeight - cornerThickness;

        boolean leftEdge = mouseX < borderThickness;
        boolean rightEdge = mouseX > sceneWidth - borderThickness;
        boolean topEdge = mouseY < borderThickness;
        boolean bottomEdge = mouseY > sceneHeight - borderThickness;

        // 1. Prioridade máxima para os 4 cantos (área generosa para facilidade de captura)
        if (topCorner && leftCorner) return Cursor.NW_RESIZE;
        if (topCorner && rightCorner) return Cursor.NE_RESIZE;
        if (bottomCorner && leftCorner) return Cursor.SW_RESIZE;
        if (bottomCorner && rightCorner) return Cursor.SE_RESIZE;

        // 2. Bordas retas
        if (leftEdge) return Cursor.W_RESIZE;
        if (rightEdge) return Cursor.E_RESIZE;
        if (topEdge) return Cursor.N_RESIZE;
        if (bottomEdge) return Cursor.S_RESIZE;

        return Cursor.DEFAULT;
    }

    /**
     * Calcula as novas dimensões e posição da janela garantindo estabilidade e limites mínimos.
     */
    public static WindowBounds calculateNewBounds(Cursor resizeCursor,
                                                  double startX, double startY,
                                                  double startWidth, double startHeight,
                                                  double deltaX, double deltaY,
                                                  double minWidth, double minHeight) {
        double x = startX;
        double y = startY;
        double width = startWidth;
        double height = startHeight;

        boolean resizeEast = (resizeCursor == Cursor.E_RESIZE || resizeCursor == Cursor.NE_RESIZE || resizeCursor == Cursor.SE_RESIZE);
        boolean resizeSouth = (resizeCursor == Cursor.S_RESIZE || resizeCursor == Cursor.SW_RESIZE || resizeCursor == Cursor.SE_RESIZE);
        boolean resizeWest = (resizeCursor == Cursor.W_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.SW_RESIZE);
        boolean resizeNorth = (resizeCursor == Cursor.N_RESIZE || resizeCursor == Cursor.NW_RESIZE || resizeCursor == Cursor.NE_RESIZE);

        if (resizeEast) {
            width = Math.max(minWidth, startWidth + deltaX);
        }
        if (resizeSouth) {
            height = Math.max(minHeight, startHeight + deltaY);
        }
        if (resizeWest) {
            double maxDeltaX = startWidth - minWidth;
            double actualDeltaX = Math.min(deltaX, maxDeltaX);
            x = startX + actualDeltaX;
            width = startWidth - actualDeltaX;
        }
        if (resizeNorth) {
            double maxDeltaY = startHeight - minHeight;
            double actualDeltaY = Math.min(deltaY, maxDeltaY);
            y = startY + actualDeltaY;
            height = startHeight - actualDeltaY;
        }

        return new WindowBounds(x, y, width, height);
    }

    private static class ResizeListener implements javafx.event.EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor currentCursor = Cursor.DEFAULT;
        private Cursor activeResizeCursor = Cursor.DEFAULT;
        private boolean isResizing = false;
        private boolean wasResizing = false;

        private double startX = 0;
        private double startY = 0;
        private double startScreenX = 0;
        private double startScreenY = 0;
        private double startWidth = 0;
        private double startHeight = 0;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (stage.isFullScreen() || stage.isMaximized()) {
                if (currentCursor != Cursor.DEFAULT) {
                    currentCursor = Cursor.DEFAULT;
                    stage.getScene().setCursor(Cursor.DEFAULT);
                }
                isResizing = false;
                return;
            }

            var eventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            if (MouseEvent.MOUSE_MOVED.equals(eventType)) {
                if (!isResizing) {
                    Cursor cursor = calculateCursor(
                            mouseEvent.getSceneX(),
                            mouseEvent.getSceneY(),
                            scene.getWidth(),
                            scene.getHeight(),
                            BORDER_THICKNESS,
                            CORNER_THICKNESS
                    );

                    if (cursor != currentCursor) {
                        currentCursor = cursor;
                        scene.setCursor(cursor);
                    }
                }
            } else if (MouseEvent.MOUSE_PRESSED.equals(eventType)) {
                if (currentCursor != Cursor.DEFAULT) {
                    isResizing = true;
                    wasResizing = false;
                    activeResizeCursor = currentCursor;
                    startX = stage.getX();
                    startY = stage.getY();
                    startScreenX = mouseEvent.getScreenX();
                    startScreenY = mouseEvent.getScreenY();
                    startWidth = stage.getWidth();
                    startHeight = stage.getHeight();
                    mouseEvent.consume();
                }
            } else if (MouseEvent.MOUSE_DRAGGED.equals(eventType)) {
                if (isResizing && activeResizeCursor != Cursor.DEFAULT) {
                    double deltaX = mouseEvent.getScreenX() - startScreenX;
                    double deltaY = mouseEvent.getScreenY() - startScreenY;

                    double minWidth = Math.max(400, stage.getMinWidth());
                    double minHeight = Math.max(300, stage.getMinHeight());

                    WindowBounds bounds = calculateNewBounds(
                            activeResizeCursor,
                            startX, startY,
                            startWidth, startHeight,
                            deltaX, deltaY,
                            minWidth, minHeight
                    );

                    if (bounds.x() != stage.getX()) stage.setX(bounds.x());
                    if (bounds.y() != stage.getY()) stage.setY(bounds.y());
                    if (bounds.width() != stage.getWidth()) stage.setWidth(bounds.width());
                    if (bounds.height() != stage.getHeight()) stage.setHeight(bounds.height);

                    scene.setCursor(activeResizeCursor);
                    mouseEvent.consume();
                }
            } else if (MouseEvent.MOUSE_RELEASED.equals(eventType)) {
                if (isResizing) {
                    isResizing = false;
                    wasResizing = true;
                    activeResizeCursor = Cursor.DEFAULT;

                    Cursor cursor = calculateCursor(
                            mouseEvent.getSceneX(),
                            mouseEvent.getSceneY(),
                            scene.getWidth(),
                            scene.getHeight(),
                            BORDER_THICKNESS,
                            CORNER_THICKNESS
                    );
                    currentCursor = cursor;
                    scene.setCursor(cursor);
                    mouseEvent.consume();
                }
            } else if (MouseEvent.MOUSE_CLICKED.equals(eventType)) {
                if (wasResizing) {
                    wasResizing = false;
                    mouseEvent.consume();
                }
            } else if (MouseEvent.MOUSE_EXITED.equals(eventType)) {
                if (!isResizing) {
                    currentCursor = Cursor.DEFAULT;
                    scene.setCursor(Cursor.DEFAULT);
                }
            }
        }
    }

    /**
     * Camada transparente de sobreposição posicionada no topo da hierarquia da janela (Z-index superior).
     * Garante que os 4 cantos e 4 bordas capturem com prioridade o ponteiro do mouse, exibindo
     * imediatamente os cursores nativos bidirecionais (setas para fora nos cantos e bordas)
     * sem que nós filhos com estilos próprios (como botões ou containers) sobreponham o cursor.
     */
    public static class ResizeOverlay extends Region {
        private final Stage stage;
        private final Region nwCorner = new Region();
        private final Region neCorner = new Region();
        private final Region swCorner = new Region();
        private final Region seCorner = new Region();
        private final Region nEdge = new Region();
        private final Region sEdge = new Region();
        private final Region wEdge = new Region();
        private final Region eEdge = new Region();

        public ResizeOverlay(Stage stage) {
            this.stage = stage;
            setPickOnBounds(false);

            initZone(nwCorner, Cursor.NW_RESIZE);
            initZone(neCorner, Cursor.NE_RESIZE);
            initZone(swCorner, Cursor.SW_RESIZE);
            initZone(seCorner, Cursor.SE_RESIZE);
            initZone(nEdge, Cursor.N_RESIZE);
            initZone(sEdge, Cursor.S_RESIZE);
            initZone(wEdge, Cursor.W_RESIZE);
            initZone(eEdge, Cursor.E_RESIZE);

            getChildren().addAll(nwCorner, neCorner, swCorner, seCorner, nEdge, sEdge, wEdge, eEdge);

            if (stage != null) {
                stage.fullScreenProperty().addListener((obs, oldVal, isFull) -> updateActiveState());
                stage.maximizedProperty().addListener((obs, oldVal, isMax) -> updateActiveState());
                updateActiveState();
            }
        }

        private void updateActiveState() {
            if (stage != null) {
                boolean active = !stage.isFullScreen() && !stage.isMaximized();
                setVisible(active);
                setManaged(active);
            }
        }

        private void initZone(Region region, Cursor cursor) {
            region.setCursor(cursor);
            region.setPickOnBounds(true);
            region.setStyle("-fx-background-color: transparent;");

            final double[] dragStart = new double[6]; // startX, startY, startWidth, startHeight, startScreenX, startScreenY

            region.setOnMousePressed(e -> {
                if (stage == null || stage.isFullScreen() || stage.isMaximized()) return;
                dragStart[0] = stage.getX();
                dragStart[1] = stage.getY();
                dragStart[2] = stage.getWidth();
                dragStart[3] = stage.getHeight();
                dragStart[4] = e.getScreenX();
                dragStart[5] = e.getScreenY();
                e.consume();
            });

            region.setOnMouseDragged(e -> {
                if (stage == null || stage.isFullScreen() || stage.isMaximized()) return;
                double deltaX = e.getScreenX() - dragStart[4];
                double deltaY = e.getScreenY() - dragStart[5];
                double minWidth = Math.max(400, stage.getMinWidth());
                double minHeight = Math.max(300, stage.getMinHeight());

                WindowBounds bounds = calculateNewBounds(
                        cursor,
                        dragStart[0], dragStart[1],
                        dragStart[2], dragStart[3],
                        deltaX, deltaY,
                        minWidth, minHeight
                );

                if (bounds.x() != stage.getX()) stage.setX(bounds.x());
                if (bounds.y() != stage.getY()) stage.setY(bounds.y());
                if (bounds.width() != stage.getWidth()) stage.setWidth(bounds.width());
                if (bounds.height() != stage.getHeight()) stage.setHeight(bounds.height());
                e.consume();
            });
        }

        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            if (w <= 0 || h <= 0) return;

            double corner = CORNER_THICKNESS;
            double border = BORDER_THICKNESS;

            // Cantos: prioridade visual nos 4 cantos para facilitar a expansão diagonal
            nwCorner.resizeRelocate(0, 0, corner, corner);
            neCorner.resizeRelocate(w - corner, 0, corner, corner);
            swCorner.resizeRelocate(0, h - corner, corner, corner);
            seCorner.resizeRelocate(w - corner, h - corner, corner, corner);

            // Bordas retas
            nEdge.resizeRelocate(corner, 0, Math.max(0, w - 2 * corner), border);
            sEdge.resizeRelocate(corner, h - border, Math.max(0, w - 2 * corner), border);
            wEdge.resizeRelocate(0, corner, border, Math.max(0, h - 2 * corner));
            eEdge.resizeRelocate(w - border, corner, border, Math.max(0, h - 2 * corner));
        }

        public Region getNwCorner() { return nwCorner; }
        public Region getNeCorner() { return neCorner; }
        public Region getSwCorner() { return swCorner; }
        public Region getSeCorner() { return seCorner; }
        public Region getNEdge() { return nEdge; }
        public Region getSEdge() { return sEdge; }
        public Region getWEdge() { return wEdge; }
        public Region getEEdge() { return eEdge; }
    }
}

