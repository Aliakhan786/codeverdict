package com.aaliya.codeverdict.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A private temp folder for one review job. Every file the job needs
 * (Solution.java, Reference.java, proof tests, harness) lives here and
 * the whole folder is deleted when the job ends.
 */
public final class Workspace implements AutoCloseable {

    private final Path root;
    private final boolean keepOnClose;

    private Workspace(Path root, boolean keepOnClose) {
        this.root = root;
        this.keepOnClose = keepOnClose;
    }

    public static Workspace create(Path baseDir, boolean keepOnClose) {
        try {
            Files.createDirectories(baseDir);
            Path dir = baseDir.resolve("job-" + UUID.randomUUID());
            Files.createDirectories(dir);
            return new Workspace(dir.toAbsolutePath().normalize(), keepOnClose);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create workspace in " + baseDir, e);
        }
    }

    public Path root() {
        return root;
    }

    /** Resolves a path relative to the workspace and refuses to escape it. */
    public Path path(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes workspace: " + relative);
        }
        return p;
    }

    public Path write(String relative, String content) {
        Path p = path(relative);
        try {
            Files.createDirectories(p.getParent());
            Files.writeString(p, content, StandardCharsets.UTF_8);
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path dir(String relative) {
        Path p = path(relative);
        try {
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String read(String relative) {
        try {
            return Files.readString(path(relative), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean exists(String relative) {
        return Files.exists(path(relative));
    }

    /** Deletes a sub folder (used to reset a reports folder between runs). */
    public void deleteDir(String relative) {
        deleteTree(path(relative));
    }

    @Override
    public void close() {
        if (!keepOnClose) {
            deleteTree(root);
        }
    }

    private static void deleteTree(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort: temp folder is cleaned by the OS eventually
                }
            });
        } catch (IOException ignored) {
            // best effort
        }
    }
}
