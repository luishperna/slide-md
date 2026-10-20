package com.slidemd.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de histórico de apresentações recentemente abertas pelo slide-md.
 * Persiste a lista de caminhos no diretório do usuário (~/.slide-md/recent_files.txt).
 */
public class RecentFilesManager {

    private static final Logger LOGGER = Logger.getLogger(RecentFilesManager.class.getName());
    private static final int MAX_RECENT_FILES = 8;
    private static Path storagePath = defaultStoragePath();

    private static Path defaultStoragePath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".slide-md", "recent_files.txt");
    }

    /**
     * Permite alterar o caminho de armazenamento (útil para testes unitários).
     */
    public static void setStoragePath(Path path) {
        storagePath = path != null ? path : defaultStoragePath();
    }

    /**
     * Retorna a lista de arquivos recentes existentes no disco, ordenados do mais recente ao mais antigo.
     */
    public static synchronized List<File> getRecentFiles() {
        if (!Files.exists(storagePath)) {
            return Collections.emptyList();
        }

        try {
            List<String> lines = Files.readAllLines(storagePath, StandardCharsets.UTF_8);
            List<File> result = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    File file = new File(trimmed);
                    if (file.exists() && file.isFile()) {
                        result.add(file);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao ler histórico de apresentações recentes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Adiciona um arquivo ao topo do histórico de recentes e persiste em disco.
     */
    public static synchronized void addRecentFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        try {
            String canonicalPath = file.getCanonicalPath();
            List<String> lines = new ArrayList<>();

            if (Files.exists(storagePath)) {
                for (String line : Files.readAllLines(storagePath, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(canonicalPath)) {
                        lines.add(trimmed);
                    }
                }
            }

            // Insere no topo
            lines.add(0, canonicalPath);

            // Mantém no máximo MAX_RECENT_FILES
            if (lines.size() > MAX_RECENT_FILES) {
                lines = lines.subList(0, MAX_RECENT_FILES);
            }

            // Garante criação do diretório ~/.slide-md
            if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                Files.createDirectories(storagePath.getParent());
            }

            Files.write(storagePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao salvar histórico de apresentações recentes: " + e.getMessage());
        }
    }

    /**
     * Remove um arquivo específico do histórico.
     */
    public static synchronized void removeRecentFile(File file) {
        if (file == null || !Files.exists(storagePath)) {
            return;
        }

        try {
            String pathToRemove = file.getCanonicalPath();
            List<String> lines = new ArrayList<>();

            for (String line : Files.readAllLines(storagePath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase(pathToRemove)) {
                    lines.add(trimmed);
                }
            }

            Files.write(storagePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao remover arquivo do histórico: " + e.getMessage());
        }
    }

    /**
     * Limpa o histórico de recentes.
     */
    public static synchronized void clear() {
        try {
            if (Files.exists(storagePath)) {
                Files.delete(storagePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao limpar histórico: " + e.getMessage());
        }
    }
}
