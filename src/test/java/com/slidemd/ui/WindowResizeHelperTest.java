package com.slidemd.ui;

import javafx.scene.Cursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowResizeHelperTest {

    private static final int BORDER = WindowResizeHelper.BORDER_THICKNESS;
    private static final int CORNER = WindowResizeHelper.CORNER_THICKNESS;
    private static final double WIDTH = 1280;
    private static final double HEIGHT = 720;

    @Test
    void testCornerDetection() {
        // Top-Left corner
        assertEquals(Cursor.NW_RESIZE, WindowResizeHelper.calculateCursor(5, 5, WIDTH, HEIGHT, BORDER, CORNER));
        assertEquals(Cursor.NW_RESIZE, WindowResizeHelper.calculateCursor(15, 15, WIDTH, HEIGHT, BORDER, CORNER));

        // Top-Right corner
        assertEquals(Cursor.NE_RESIZE, WindowResizeHelper.calculateCursor(WIDTH - 5, 5, WIDTH, HEIGHT, BORDER, CORNER));
        assertEquals(Cursor.NE_RESIZE, WindowResizeHelper.calculateCursor(WIDTH - 15, 15, WIDTH, HEIGHT, BORDER, CORNER));

        // Bottom-Left corner
        assertEquals(Cursor.SW_RESIZE, WindowResizeHelper.calculateCursor(5, HEIGHT - 5, WIDTH, HEIGHT, BORDER, CORNER));
        assertEquals(Cursor.SW_RESIZE, WindowResizeHelper.calculateCursor(15, HEIGHT - 15, WIDTH, HEIGHT, BORDER, CORNER));

        // Bottom-Right corner
        assertEquals(Cursor.SE_RESIZE, WindowResizeHelper.calculateCursor(WIDTH - 5, HEIGHT - 5, WIDTH, HEIGHT, BORDER, CORNER));
        assertEquals(Cursor.SE_RESIZE, WindowResizeHelper.calculateCursor(WIDTH - 15, HEIGHT - 15, WIDTH, HEIGHT, BORDER, CORNER));
    }

    @Test
    void testEdgeDetection() {
        // Left edge
        assertEquals(Cursor.W_RESIZE, WindowResizeHelper.calculateCursor(4, 300, WIDTH, HEIGHT, BORDER, CORNER));

        // Right edge
        assertEquals(Cursor.E_RESIZE, WindowResizeHelper.calculateCursor(WIDTH - 4, 300, WIDTH, HEIGHT, BORDER, CORNER));

        // Top edge
        assertEquals(Cursor.N_RESIZE, WindowResizeHelper.calculateCursor(600, 4, WIDTH, HEIGHT, BORDER, CORNER));

        // Bottom edge
        assertEquals(Cursor.S_RESIZE, WindowResizeHelper.calculateCursor(600, HEIGHT - 4, WIDTH, HEIGHT, BORDER, CORNER));
    }

    @Test
    void testInsideWindowReturnsDefault() {
        assertEquals(Cursor.DEFAULT, WindowResizeHelper.calculateCursor(640, 360, WIDTH, HEIGHT, BORDER, CORNER));
        assertEquals(Cursor.DEFAULT, WindowResizeHelper.calculateCursor(50, 50, WIDTH, HEIGHT, BORDER, CORNER));
    }

    @Test
    void testCalculateNewBoundsEastAndSouth() {
        var bounds = WindowResizeHelper.calculateNewBounds(
                Cursor.SE_RESIZE,
                100, 100,
                1000, 600,
                50, 40,
                800, 500
        );

        assertEquals(100, bounds.x());
        assertEquals(100, bounds.y());
        assertEquals(1050, bounds.width());
        assertEquals(640, bounds.height());
    }

    @Test
    void testCalculateNewBoundsWestAndNorth() {
        var bounds = WindowResizeHelper.calculateNewBounds(
                Cursor.NW_RESIZE,
                100, 100,
                1000, 600,
                50, 40,
                800, 500
        );

        // When moving NW inward, x moves by +50 and width shrinks by 50
        assertEquals(150, bounds.x());
        assertEquals(140, bounds.y());
        assertEquals(950, bounds.width());
        assertEquals(560, bounds.height());
    }

    @Test
    void testCalculateNewBoundsRespectsMinimums() {
        var bounds = WindowResizeHelper.calculateNewBounds(
                Cursor.NW_RESIZE,
                100, 100,
                1000, 600,
                500, 300,
                800, 500
        );

        // Max allowed deltaX is 1000 - 800 = 200
        // Max allowed deltaY is 600 - 500 = 100
        assertEquals(300, bounds.x());
        assertEquals(200, bounds.y());
        assertEquals(800, bounds.width());
        assertEquals(500, bounds.height());
    }

    @Test
    void testResizeOverlayCursors() {
        var overlay = new WindowResizeHelper.ResizeOverlay(null);

        // Os 4 cantos devem ter as setas de redimensionamento diagonal para fora
        assertEquals(Cursor.NW_RESIZE, overlay.getNwCorner().getCursor());
        assertEquals(Cursor.NE_RESIZE, overlay.getNeCorner().getCursor());
        assertEquals(Cursor.SW_RESIZE, overlay.getSwCorner().getCursor());
        assertEquals(Cursor.SE_RESIZE, overlay.getSeCorner().getCursor());

        // As 4 bordas retas
        assertEquals(Cursor.N_RESIZE, overlay.getNEdge().getCursor());
        assertEquals(Cursor.S_RESIZE, overlay.getSEdge().getCursor());
        assertEquals(Cursor.W_RESIZE, overlay.getWEdge().getCursor());
        assertEquals(Cursor.E_RESIZE, overlay.getEEdge().getCursor());

        // O overlay deve deixar passar cliques no centro da janela
        assertEquals(false, overlay.isPickOnBounds());

        // E capturar nas bordas e cantos
        assertEquals(true, overlay.getNwCorner().isPickOnBounds());
        assertEquals(true, overlay.getSeCorner().isPickOnBounds());
    }

    @Test
    void testResizeOverlayLayoutChildren() {
        var overlay = new WindowResizeHelper.ResizeOverlay(null);
        overlay.resize(1280, 720);
        overlay.layout();

        // Verifica canto Top-Left
        assertEquals(0, overlay.getNwCorner().getLayoutX());
        assertEquals(0, overlay.getNwCorner().getLayoutY());
        assertEquals(WindowResizeHelper.CORNER_THICKNESS, overlay.getNwCorner().getWidth());
        assertEquals(WindowResizeHelper.CORNER_THICKNESS, overlay.getNwCorner().getHeight());

        // Verifica canto Bottom-Right
        assertEquals(1280 - WindowResizeHelper.CORNER_THICKNESS, overlay.getSeCorner().getLayoutX());
        assertEquals(720 - WindowResizeHelper.CORNER_THICKNESS, overlay.getSeCorner().getLayoutY());
        assertEquals(WindowResizeHelper.CORNER_THICKNESS, overlay.getSeCorner().getWidth());
        assertEquals(WindowResizeHelper.CORNER_THICKNESS, overlay.getSeCorner().getHeight());
    }
}
