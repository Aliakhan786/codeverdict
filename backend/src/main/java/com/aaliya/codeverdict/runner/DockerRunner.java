package com.aaliya.codeverdict.runner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Production runner: every run happens in a fresh, locked-down container.
 *
 * <pre>
 * --network none        no internet / LAN access
 * --memory 256m         hard memory cap (no swap)
 * --cpus 1              one CPU
 * --pids-limit 64       stops fork bombs / thread bombs
 * --read-only           read-only root filesystem (+ small tmpfs /tmp)
 * --user 65534          runs as "nobody", not root
 * --cap-drop ALL        no Linux capabilities
 * </pre>
 *
 * The workspace is mounted read-only at /work; only /work/out is writable
 * (JUnit writes its XML report there). The container is killed on timeout
 * and removed after every run (--rm).
 *
 * Note: when the backend itself runs in a container, the job folder must be
 * mounted at the SAME path on the host (see docker-compose.sandbox.yml),
 * because the Docker daemon resolves -v paths on the host.
 */
public final class DockerRunner implements CodeRunner {

    private static final String JUNIT_IN_WORKSPACE = "lib/junit-platform-console-standalone.jar";

    private final String image;
    private final Path junitJar;
    private final int memoryMb;
    private final int cpus;
    private final int pidsLimit;
    private final int maxOutputBytes;

    public DockerRunner(String image, Path junitJar, int memoryMb, int cpus, int pidsLimit, int maxOutputBytes) {
        this.image = image;
        this.junitJar = junitJar;
        this.memoryMb = memoryMb;
        this.cpus = cpus;
        this.pidsLimit = pidsLimit;
        this.maxOutputBytes = maxOutputBytes;
    }

    @Override
    public RunResult run(Workspace workspace, RunSpec spec) {
        Path out = workspace.dir("out");
        makeWorldWritable(out);
        if (spec.withJUnit()) {
            copyJunitJar(workspace);
        }
        String name = "cv-run-" + UUID.randomUUID();
        List<String> cmd = new ArrayList<>(List.of(
                "docker", "run", "--rm",
                "--name", name,
                "--network", "none",
                "--memory", memoryMb + "m",
                "--memory-swap", memoryMb + "m",
                "--cpus", String.valueOf(cpus),
                "--pids-limit", String.valueOf(pidsLimit),
                "--read-only",
                "--tmpfs", "/tmp:rw,size=32m",
                "--user", "65534:65534",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "-v", workspace.root() + ":/work:ro",
                "-v", out + ":/work/out:rw",
                "-w", "/work",
                image,
                "java"));
        // Leave head-room inside the container for JVM metaspace/threads.
        cmd.addAll(JvmFlags.forSpec(spec, Math.max(64, memoryMb - 56)));
        List<String> cp = new ArrayList<>();
        if (spec.withJUnit()) {
            cp.add("/work/" + JUNIT_IN_WORKSPACE);
        }
        for (String rel : spec.classpath()) {
            cp.add("/work/" + rel);
        }
        cmd.add("-cp");
        cmd.add(String.join(":", cp));
        cmd.add(spec.mainClass());
        cmd.addAll(spec.args());

        // Container start adds ~1 s, so allow a little extra wall time.
        Duration timeout = spec.timeout().plusSeconds(3);
        return ProcessSupport.execute(cmd, workspace.root(), timeout, maxOutputBytes, null,
                () -> killQuietly(name));
    }

    private void copyJunitJar(Workspace ws) {
        if (ws.exists(JUNIT_IN_WORKSPACE)) {
            return;
        }
        try {
            Path target = ws.path(JUNIT_IN_WORKSPACE);
            Files.createDirectories(target.getParent());
            Files.copy(junitJar, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not copy JUnit launcher into workspace", e);
        }
    }

    private static void makeWorldWritable(Path dir) {
        try {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxrwxrwx"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // non-POSIX file system (Windows): Docker Desktop handles permissions itself
        }
    }

    private static void killQuietly(String containerName) {
        try {
            new ProcessBuilder("docker", "kill", containerName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (IOException e) {
            // container may already be gone
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String name() {
        return "docker";
    }
}
