package com.slidemd.model;

public enum Alignment {
    LEFT,
    CENTER,
    RIGHT;

    public static Alignment fromString(String value) {
        if (value == null) return CENTER;
        return switch (value.trim().toLowerCase()) {
            case "left", "start" -> LEFT;
            case "right", "end" -> RIGHT;
            default -> CENTER;
        };
    }
}
