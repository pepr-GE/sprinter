package com.sprinter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Základní výjimka aplikace SPRINTER.
 *
 * <p>Podtřídy pokrývají konkrétní chybové situace:
 * {@link ResourceNotFoundException}, {@link AccessDeniedException},
 * {@link ValidationException}.</p>
 */
public class SprinterException extends RuntimeException {

    private final HttpStatus status;

    public SprinterException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public SprinterException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public SprinterException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
