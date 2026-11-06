package com.careermate.common.exception;

/** Thrown when the AI provider is not configured, rate limited, times out or returns an invalid response. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
