package com.slidemd.ui;

import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class WindowResizeHelper {

    private static final int BORDER = 6;

    public static void addResizeListener(Stage stage, Scene scene) {
        ResizeListener listener = new ResizeListener(stage);
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
    }

    private static class ResizeListener implements javafx.event.EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
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
                if (cursorEvent != Cursor.DEFAULT) {
                    cursorEvent = Cursor.DEFAULT;
                    stage.getScene().setCursor(Cursor.DEFAULT);
                }
                return;
            }

            var eventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            if (MouseEvent.MOUSE_MOVED.equals(eventType)) {
                double mouseX = mouseEvent.getSceneX();
                double mouseY = mouseEvent.getSceneY();
                double sceneWidth = scene.getWidth();
                double sceneHeight = scene.getHeight();

                boolean left = mouseX < BORDER;
                boolean right = mouseX > sceneWidth - BORDER;
                boolean top = mouseY < BORDER;
                boolean bottom = mouseY > sceneHeight - BORDER;

                if (top && left) cursorEvent = Cursor.NW_RESIZE;
                else if (top && right) cursorEvent = Cursor.NE_RESIZE;
                else if (bottom && left) cursorEvent = Cursor.SW_RESIZE;
                else if (bottom && right) cursorEvent = Cursor.SE_RESIZE;
                else if (left) cursorEvent = Cursor.W_RESIZE;
                else if (right) cursorEvent = Cursor.E_RESIZE;
                else if (top) cursorEvent = Cursor.N_RESIZE;
                else if (bottom) cursorEvent = Cursor.S_RESIZE;
                else cursorEvent = Cursor.DEFAULT;

                scene.setCursor(cursorEvent);
            } else if (MouseEvent.MOUSE_PRESSED.equals(eventType)) {
                startX = stage.getX();
                startY = stage.getY();
                startScreenX = mouseEvent.getScreenX();
                startScreenY = mouseEvent.getScreenY();
                startWidth = stage.getWidth();
                startHeight = stage.getHeight();
            } else if (MouseEvent.MOUSE_DRAGGED.equals(eventType)) {
                if (cursorEvent == Cursor.DEFAULT) return;

                double deltaX = mouseEvent.getScreenX() - startScreenX;
                double deltaY = mouseEvent.getScreenY() - startScreenY;

                if (cursorEvent == Cursor.E_RESIZE || cursorEvent == Cursor.NE_RESIZE || cursorEvent == Cursor.SE_RESIZE) {
                    double newWidth = Math.max(stage.getMinWidth(), startWidth + deltaX);
                    stage.setWidth(newWidth);
                }
                if (cursorEvent == Cursor.S_RESIZE || cursorEvent == Cursor.SW_RESIZE || cursorEvent == Cursor.SE_RESIZE) {
                    double newHeight = Math.max(stage.getMinHeight(), startHeight + deltaY);
                    stage.setHeight(newHeight);
                }
                if (cursorEvent == Cursor.W_RESIZE || cursorEvent == Cursor.NW_RESIZE || cursorEvent == Cursor.SW_RESIZE) {
                    double newWidth = Math.max(stage.getMinWidth(), startWidth - deltaX);
                    if (newWidth > stage.getMinWidth()) {
                        stage.setX(startX + deltaX);
                        stage.setWidth(newWidth);
                    }
                }
                if (cursorEvent == Cursor.N_RESIZE || cursorEvent == Cursor.NW_RESIZE || cursorEvent == Cursor.NE_RESIZE) {
                    double newHeight = Math.max(stage.getMinHeight(), startHeight - deltaY);
                    if (newHeight > stage.getMinHeight()) {
                        stage.setY(startY + deltaY);
                        stage.setHeight(newHeight);
                    }
                }
            } else if (MouseEvent.MOUSE_EXITED.equals(eventType)) {
                scene.setCursor(Cursor.DEFAULT);
            }
        }
    }
}
