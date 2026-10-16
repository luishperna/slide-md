package com.slidemd.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlideDeck {
    private String title = "Untitled Presentation";
    private final List<Slide> slides = new ArrayList<>();
    private File sourceFile;

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
}
