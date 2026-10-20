package com.slidemd.audio;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de áudio e música de fundo (BGM) para apresentações.
 * Suporta trilha contínua global (frontmatter) ou faixas específicas por slide,
 * com controle de loop e volume.
 */
public class AudioManager {

    private static final Logger LOGGER = Logger.getLogger(AudioManager.class.getName());

    private MediaPlayer mediaPlayer;
    private String currentPlayingUri;
    private boolean isMuted = false;

    public AudioManager() {}

    /**
     * Sincroniza a reprodução da música com base no caminho e parâmetros informados.
     * Se a faixa requisitada já for a que está tocando, mantém a reprodução sem reiniciar.
     */
    public void syncMusic(String musicPath, File baseDir, boolean loop, double volume) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> syncMusic(musicPath, baseDir, loop, volume));
            return;
        }

        if (musicPath == null || musicPath.isBlank() || musicPath.equalsIgnoreCase("none") || musicPath.equalsIgnoreCase("off") || musicPath.equalsIgnoreCase("false")) {
            stop();
            return;
        }

        try {
            String resolvedUri = resolveMusicUri(musicPath, baseDir);
            if (resolvedUri == null) {
                LOGGER.warning("Arquivo de áudio não encontrado: " + musicPath);
                return;
            }

            // Se for a mesma música já em reprodução, apenas ajusta volume e loop sem reiniciar
            if (resolvedUri.equals(currentPlayingUri) && mediaPlayer != null) {
                mediaPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
                mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED || mediaPlayer.getStatus() == MediaPlayer.Status.READY) {
                    mediaPlayer.play();
                }
                return;
            }

            // Para faixa anterior
            stop();

            Media media = new Media(resolvedUri);
            mediaPlayer = new MediaPlayer(media);
            currentPlayingUri = resolvedUri;

            mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
            mediaPlayer.setVolume(Math.max(0.0, Math.min(1.0, volume)));
            mediaPlayer.setMute(isMuted);
            mediaPlayer.setAutoPlay(true);

            mediaPlayer.setOnError(() -> {
                LOGGER.log(Level.WARNING, "Erro ao reproduzir áudio: " + resolvedUri, mediaPlayer.getError());
            });

            mediaPlayer.play();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao inicializar áudio: " + musicPath, e);
        }
    }

    private String resolveMusicUri(String path, File baseDir) {
        String clean = path.trim();
        if (clean.startsWith("url(") && clean.endsWith(")")) {
            clean = clean.substring(4, clean.length() - 1).trim();
            if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
                clean = clean.substring(1, clean.length() - 1);
            }
        }

        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("file:/")) {
            return clean;
        }

        File file = new File(clean);
        if (!file.isAbsolute() && baseDir != null) {
            file = new File(baseDir, clean);
        }

        if (file.exists()) {
            return file.toURI().toString();
        }

        return null;
    }

    public void pause() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (Exception ignored) {}
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.play();
            } catch (Exception ignored) {}
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        currentPlayingUri = null;
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (mediaPlayer != null) {
            mediaPlayer.setMute(isMuted);
        }
    }

    public boolean isMuted() {
        return isMuted;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void dispose() {
        stop();
    }
}
