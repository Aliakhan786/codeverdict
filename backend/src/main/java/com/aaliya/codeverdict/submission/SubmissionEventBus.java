package com.aaliya.codeverdict.submission;

import com.aaliya.codeverdict.proof.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes review progress to browsers with Server-Sent Events.
 * Why SSE: updates only flow server -> browser, it is plain HTTP (works through proxies),
 * the browser reconnects automatically, and there is no WebSocket protocol to manage.
 * Late subscribers get a replay of the stages so far, so refreshing the page loses nothing.
 */
@Component
public class SubmissionEventBus {

    private static final Logger log = LoggerFactory.getLogger(SubmissionEventBus.class);
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<UUID, List<ProgressEvent>> history = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID submissionId, ProgressEvent current) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> list = emitters.computeIfAbsent(submissionId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        List<ProgressEvent> past = new ArrayList<>(history.getOrDefault(submissionId, List.of()));
        if (past.isEmpty() && current != null) {
            past.add(current);
        }
        for (ProgressEvent e : past) {
            if (!send(emitter, e)) {
                return emitter;
            }
        }
        // Already finished before we subscribed? Close right away.
        if (current != null && current.terminal()) {
            if (past.stream().noneMatch(ProgressEvent::terminal)) {
                send(emitter, current);
            }
            emitter.complete();
        }
        return emitter;
    }

    public void publish(UUID submissionId, Stage stage, String detail) {
        ProgressEvent event = new ProgressEvent(stage.name(), stage.label(), detail, stage.progress(), stage.terminal(),
                Instant.now());
        history.computeIfAbsent(submissionId, k -> new CopyOnWriteArrayList<>()).add(event);
        List<SseEmitter> list = emitters.getOrDefault(submissionId, List.of());
        for (SseEmitter emitter : list) {
            if (send(emitter, event) && stage.terminal()) {
                emitter.complete();
            }
        }
        if (stage.terminal()) {
            emitters.remove(submissionId);
            history.remove(submissionId);
        }
    }

    private static boolean send(SseEmitter emitter, ProgressEvent event) {
        try {
            emitter.send(SseEmitter.event().name("progress").data(event, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE client went away: {}", e.getMessage());
            emitter.completeWithError(e);
            return false;
        }
    }
}
