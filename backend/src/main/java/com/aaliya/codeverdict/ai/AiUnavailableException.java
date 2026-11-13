package com.aaliya.codeverdict.ai;

/** Thrown when the AI cannot answer (no key, quota, outage, unparsable JSON after retry). */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
