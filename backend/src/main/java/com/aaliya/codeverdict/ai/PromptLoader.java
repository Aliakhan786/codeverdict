package com.aaliya.codeverdict.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads prompt templates from src/main/resources/prompts/ and fills {{placeholders}}.
 * Prompts live in text files so they can be improved without touching Java code.
 */
public final class PromptLoader {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String template(String name) {
        return cache.computeIfAbsent(name, PromptLoader::load);
    }

    public String render(String name, Map<String, String> vars) {
        String text = template(name + ".txt");
        for (Map.Entry<String, String> e : vars.entrySet()) {
            text = text.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
        }
        return text;
    }

    public String schema(String name) {
        return template(name + ".schema.json");
    }

    /** "12: for (int i = 0; ...)" - exact line numbers make charge locations exact. */
    public static String numberLines(String code) {
        String[] lines = code.split("\r?\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(i + 1).append(": ").append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    private static String load(String file) {
        try (InputStream in = PromptLoader.class.getResourceAsStream("/prompts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Missing prompt file: prompts/" + file);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
