package com.slidemd.ui;

import javafx.application.Platform;
import javafx.scene.Cursor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaserPenOverlayTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    void testInitialState() {
        LaserPenOverlay overlay = new LaserPenOverlay();
        assertFalse(overlay.isActive());
        assertTrue(overlay.isMouseTransparent());
        assertFalse(overlay.isVisible());
    }

    @Test
    void testActivationToggle() {
        LaserPenOverlay overlay = new LaserPenOverlay();

        overlay.setActive(true);
        assertTrue(overlay.isActive());
        assertFalse(overlay.isMouseTransparent());
        assertTrue(overlay.isVisible());
        assertEquals(Cursor.NONE, overlay.getCursor());

        overlay.setActive(false);
        assertFalse(overlay.isActive());
        assertTrue(overlay.isMouseTransparent());
        assertFalse(overlay.isVisible());
        assertEquals(Cursor.DEFAULT, overlay.getCursor());
    }

    @Test
    void testClearDoesNotThrow() {
        LaserPenOverlay overlay = new LaserPenOverlay();
        overlay.setActive(true);
        assertDoesNotThrow(overlay::clear);
    }

    @Test
    void testClickNavigation() {
        java.util.concurrent.atomic.AtomicBoolean prevCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicBoolean nextCalled = new java.util.concurrent.atomic.AtomicBoolean(false);

        LaserPenOverlay overlay = new LaserPenOverlay(() -> prevCalled.set(true), () -> nextCalled.set(true));
        overlay.resize(1000, 600);
        overlay.setActive(true);

        // Simula clique na metade esquerda (volta slide)
        javafx.scene.input.MouseEvent pressLeft = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED, 200, 300, 200, 300,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null);
        overlay.fireEvent(pressLeft);

        javafx.scene.input.MouseEvent releaseLeft = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_RELEASED, 200, 300, 200, 300,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null);
        overlay.fireEvent(releaseLeft);

        assertTrue(prevCalled.get());
        assertFalse(nextCalled.get());

        // Simula clique na metade direita (avança slide)
        javafx.scene.input.MouseEvent pressRight = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_PRESSED, 800, 300, 800, 300,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null);
        overlay.fireEvent(pressRight);

        javafx.scene.input.MouseEvent releaseRight = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_RELEASED, 800, 300, 800, 300,
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, false, false, false, null);
        overlay.fireEvent(releaseRight);

        assertTrue(nextCalled.get());
    }
}
