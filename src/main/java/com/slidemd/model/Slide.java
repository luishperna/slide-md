package com.slidemd.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Slide {
    private final int index;
    private final String rawMarkdown;
    private String contentMarkdown;
    private String title;
    private String backgroundColor;
    private String backgroundImage;
    private String image;
    private String color;
    private Alignment alignment = Alignment.CENTER;
    private String transition = "slide"; // slide, fade, zoom, morph
    private VideoSpec videoSpec;
    private String music;
    private Boolean musicLoop;
    private Double musicVolume;
    private final List<String> notes = new ArrayList<>();

    public Slide(int index, String rawMarkdown) {
        this.index = index;
        this.rawMarkdown = rawMarkdown;
        this.contentMarkdown = rawMarkdown;
    }

    public int getIndex() {
        return index;
    }

    public String getRawMarkdown() {
        return rawMarkdown;
    }

    public String getContentMarkdown() {
        return contentMarkdown != null ? contentMarkdown : rawMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }

    public Boolean isMusicLoop() {
        return musicLoop;
    }

    public void setMusicLoop(Boolean musicLoop) {
        this.musicLoop = musicLoop;
    }

    public Double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(Double musicVolume) {
        this.musicVolume = musicVolume;
    }

    public String getSound() {
        return music;
    }

    public void setSound(String sound) {
        this.music = sound;
    }

    public Boolean isSoundLoop() {
        return musicLoop;
    }

    public void setSoundLoop(Boolean soundLoop) {
        this.musicLoop = soundLoop;
    }

    public Double getSoundVolume() {
        return musicVolume;
    }

    public void setSoundVolume(Double soundVolume) {
        this.musicVolume = soundVolume;
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

    public boolean hasMusic() {
        return music != null && !music.isBlank() && !music.equalsIgnoreCase("none") && !music.equalsIgnoreCase("off");
    }

    public boolean hasSound() {
        return hasMusic();
    }
}
