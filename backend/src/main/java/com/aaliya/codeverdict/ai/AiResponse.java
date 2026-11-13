package com.aaliya.codeverdict.ai;

/** An AI answer plus its cost, so every review can log tokens used and time spent. */
public record AiResponse<T>(T value, int tokens, long millis, boolean cached) {

    public static <T> AiResponse<T> cachedCopy(AiResponse<T> r) {
        return new AiResponse<>(r.value(), 0, 0, true);
    }
}
