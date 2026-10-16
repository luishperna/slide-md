package com.slidemd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Slide {
    private final int index;
    private final String rawMarkdown;
    private String title;
    private String backgroundColor;
    private String backgroundImage;
    private Alignment alignment = Alignment.CENTER;
    private String transition = "slide"; // slide, fade, zoom
    private VideoSpec videoSpec;
    private final List<String> notes = new ArrayList<>();

    public Slide(int index, String rawMarkdown) {
        this.index = index;
        this.rawMarkdown = rawMarkdown;
    }

    public int getIndex() {
        return index;
    }

    public String getRawMarkdown() {
        return rawMarkdown;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public VideoSpec getVideoSpec() {
        return videoSpec;
    }

    public void setVideoSpec(VideoSpec videoSpec) {
        this.videoSpec = videoSpec;
    }

    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public void addNote(String note) {
        this.notes.add(note);
    }

    public boolean hasVideo() {
        return videoSpec != null && (videoSpec.getSourcePath() != null || videoSpec.hasJoin());
    }
}
