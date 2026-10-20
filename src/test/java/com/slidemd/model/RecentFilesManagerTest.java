package com.slidemd.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecentFilesManagerTest {

    @TempDir
    Path tempDir;

    private Path testStorage;

    @BeforeEach
    void setUp() {
        testStorage = tempDir.resolve("recent_test.txt");
        RecentFilesManager.setStoragePath(testStorage);
    }

    @AfterEach
    void tearDown() {
        RecentFilesManager.setStoragePath(null);
    }

    @Test
    void testAddAndGetRecentFiles() throws IOException {
        File file1 = Files.createFile(tempDir.resolve("slide1.md")).toFile();
        File file2 = Files.createFile(tempDir.resolve("slide2.md")).toFile();

        RecentFilesManager.addRecentFile(file1);
        RecentFilesManager.addRecentFile(file2);

        List<File> recents = RecentFilesManager.getRecentFiles();
        assertEquals(2, recents.size());
        // Mais recente primeiro (file2)
        assertEquals(file2.getCanonicalPath(), recents.get(0).getCanonicalPath());
        assertEquals(file1.getCanonicalPath(), recents.get(1).getCanonicalPath());
    }

    @Test
    void testDuplicatesMovedToTop() throws IOException {
        File file1 = Files.createFile(tempDir.resolve("deck1.md")).toFile();
        File file2 = Files.createFile(tempDir.resolve("deck2.md")).toFile();

        RecentFilesManager.addRecentFile(file1);
        RecentFilesManager.addRecentFile(file2);
        RecentFilesManager.addRecentFile(file1); // Reabre file1

        List<File> recents = RecentFilesManager.getRecentFiles();
        assertEquals(2, recents.size());
        assertEquals(file1.getCanonicalPath(), recents.get(0).getCanonicalPath());
        assertEquals(file2.getCanonicalPath(), recents.get(1).getCanonicalPath());
    }

    @Test
    void testRemoveRecentFile() throws IOException {
        File file1 = Files.createFile(tempDir.resolve("a.md")).toFile();
        File file2 = Files.createFile(tempDir.resolve("b.md")).toFile();

        RecentFilesManager.addRecentFile(file1);
        RecentFilesManager.addRecentFile(file2);
        RecentFilesManager.removeRecentFile(file1);

        List<File> recents = RecentFilesManager.getRecentFiles();
        assertEquals(1, recents.size());
        assertEquals(file2.getCanonicalPath(), recents.get(0).getCanonicalPath());
    }

    @Test
    void testNonExistentFilesIgnored() throws IOException {
        File file1 = Files.createFile(tempDir.resolve("exists.md")).toFile();
        RecentFilesManager.addRecentFile(file1);

        // Deleta o arquivo do disco
        Files.delete(file1.toPath());

        List<File> recents = RecentFilesManager.getRecentFiles();
        assertTrue(recents.isEmpty());
    }
}
