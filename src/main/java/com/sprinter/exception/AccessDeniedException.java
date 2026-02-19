package com.sprinter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Výjimka pro případ nedostatečných oprávnění.
 * Výsledkem je HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends SprinterException {

    public AccessDeniedException() {
        super("Nemáte oprávnění k této akci.", HttpStatus.FORBIDDEN);
    }

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
