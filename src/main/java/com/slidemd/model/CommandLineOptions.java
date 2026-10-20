package com.slidemd.model;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Parser de argumentos de linha de comando para o slide-md.
 * Suporta arquivo Markdown inicial e flags como:
 *   --music <caminho> ou --music=<caminho>
 *   -music <caminho> ou -music=<caminho>
 *   -m <caminho>
 *   --music-loop <true/false>
 *   --music-volume <80% ou 0.8>
 */
public record CommandLineOptions(
        File markdownFile,
        String musicPath,
        boolean musicLoop,
        double musicVolume
) {
    public static CommandLineOptions parse(List<String> rawArgs, Map<String, String> namedArgs) {
        File file = null;
        String music = null;
        boolean loop = true;
        double volume = 0.8;

        // 1. Argumentos nomeados JavaFX (--chave=valor)
        if (namedArgs != null) {
            if (namedArgs.containsKey("music")) {
                music = namedArgs.get("music");
            } else if (namedArgs.containsKey("audio")) {
                music = namedArgs.get("audio");
            } else if (namedArgs.containsKey("bgm")) {
                music = namedArgs.get("bgm");
            }

            if (namedArgs.containsKey("music-loop")) {
                loop = Boolean.parseBoolean(namedArgs.get("music-loop"));
            }
            if (namedArgs.containsKey("music-volume")) {
                volume = parseVolume(namedArgs.get("music-volume"), volume);
            }
        }

        // 2. Argumentos brutos da linha de comando
        if (rawArgs != null) {
            for (int i = 0; i < rawArgs.size(); i++) {
                String arg = rawArgs.get(i).trim();

                if (arg.startsWith("--music=") || arg.startsWith("-music=") || arg.startsWith("--audio=") || arg.startsWith("-m=")) {
                    music = arg.substring(arg.indexOf('=') + 1).trim();
                } else if (arg.equals("--music") || arg.equals("-music") || arg.equals("-m") || arg.equals("--audio") || arg.equals("-audio")) {
                    if (i + 1 < rawArgs.size() && !rawArgs.get(i + 1).startsWith("-")) {
                        music = rawArgs.get(++i).trim();
                    }
                } else if (arg.startsWith("--music-loop=") || arg.startsWith("-music-loop=")) {
                    loop = Boolean.parseBoolean(arg.substring(arg.indexOf('=') + 1).trim());
                } else if (arg.equals("--music-loop") || arg.equals("-music-loop")) {
                    if (i + 1 < rawArgs.size() && !rawArgs.get(i + 1).startsWith("-")) {
                        loop = Boolean.parseBoolean(rawArgs.get(++i).trim());
                    }
                } else if (arg.startsWith("--music-volume=") || arg.startsWith("-music-volume=")) {
                    volume = parseVolume(arg.substring(arg.indexOf('=') + 1).trim(), volume);
                } else if (arg.equals("--music-volume") || arg.equals("-music-volume")) {
                    if (i + 1 < rawArgs.size()) {
                        volume = parseVolume(rawArgs.get(++i).trim(), volume);
                    }
                } else if (!arg.startsWith("-")) {
                    if (file == null) {
                        File candidate = new File(arg);
                        if (candidate.exists() || arg.endsWith(".md") || arg.endsWith(".markdown")) {
                            file = candidate;
                        }
                    }
                }
            }
        }

        return new CommandLineOptions(file, music, loop, volume);
    }

    private static double parseVolume(String str, double defaultVal) {
        if (str == null || str.isBlank()) return defaultVal;
        try {
            String clean = str.trim().replace("%", "");
            double v = Double.parseDouble(clean);
            if (str.contains("%") || v > 1.0) v /= 100.0;
            return Math.max(0.0, Math.min(1.0, v));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
