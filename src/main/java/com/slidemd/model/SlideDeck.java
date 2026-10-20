package com.slidemd.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlideDeck {
    private String title = "Untitled Presentation";
    private final List<Slide> slides = new ArrayList<>();
    private File sourceFile;
    private String music;
    private boolean musicLoop = true;
    private double musicVolume = 1.0;
    private String color;

    public SlideDeck() {}

    public SlideDeck(File sourceFile) {
        this.sourceFile = sourceFile;
        if (sourceFile != null) {
            this.title = sourceFile.getName().replaceFirst("[.][^.]+$", "");
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Slide> getSlides() {
        return Collections.unmodifiableList(slides);
    }

    public void addSlide(Slide slide) {
        this.slides.add(slide);
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getSlideCount() {
        return slides.size();
    }

    public Slide getSlide(int index) {
        if (index >= 0 && index < slides.size()) {
            return slides.get(index);
        }
        return null;
    }

    public String getMusic() {
        return music;
    }

    public void setMusic(String music) {
        this.music = music;
    }

    public boolean isMusicLoop() {
        return musicLoop;
    }

    public void setMusicLoop(boolean musicLoop) {
        this.musicLoop = musicLoop;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(double musicVolume) {
        this.musicVolume = musicVolume;
    }

    public String getSound() {
        return music;
    }

    public void setSound(String sound) {
        this.music = sound;
    }

    public boolean isSoundLoop() {
        return musicLoop;
    }

    public void setSoundLoop(boolean soundLoop) {
        this.musicLoop = soundLoop;
    }

    public double getSoundVolume() {
        return musicVolume;
    }

    public void setSoundVolume(double soundVolume) {
        this.musicVolume = soundVolume;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
