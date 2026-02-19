package com.sprinter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Výjimka pro případ, kdy požadovaný zdroj (entita) nebyl nalezen.
 * Výsledkem je HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends SprinterException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " s ID " + id + " nebyl(a) nalezen(a).", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
