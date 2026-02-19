package com.sprinter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Globální handler pro výjimky.
 *
 * <p>Zachytává výjimky z celé aplikace a vrací vhodné odpovědi:
 * <ul>
 *   <li>Pro REST API požadavky (Accept: application/json) – JSON chybová odpověď</li>
 *   <li>Pro webové požadavky – error stránka (Thymeleaf)</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Zpracuje chybu přístupu (403 Forbidden).
     */
    @ExceptionHandler(com.sprinter.exception.AccessDeniedException.class)
    public Object handleAccessDenied(com.sprinter.exception.AccessDeniedException ex,
                                      HttpServletRequest request) {
        log.warn("Přístup odepřen [{}]: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage(), "status", 403));
        }
        return errorView("Přístup odepřen", ex.getMessage(), 403);
    }

    /**
     * Zpracuje chybu "nenalezeno" (404 Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.info("Zdroj nenalezen [{}]: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage(), "status", 404));
        }
        return errorView("Nenalezeno", ex.getMessage(), 404);
    }

    /**
     * Zpracuje validační chyby (422 Unprocessable Entity).
     */
    @ExceptionHandler(ValidationException.class)
    public Object handleValidation(ValidationException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", ex.getMessage(), "status", 422));
        }
        return errorView("Chyba validace", ex.getMessage(), 422);
    }

    /**
     * Zpracuje ostatní neočekávané výjimky (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public Object handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Neočekávaná chyba [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Interní chyba serveru.", "status", 500));
        }
        return errorView("Chyba serveru", "Nastala neočekávaná chyba. Prosím zkuste to znovu.", 500);
    }

    // ---- Pomocné metody ----

    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return (accept != null && accept.contains("application/json"))
                || request.getRequestURI().startsWith("/api/");
    }

    private ModelAndView errorView(String title, String message, int status) {
        var mav = new ModelAndView("error/general");
        mav.addObject("errorTitle",   title);
        mav.addObject("errorMessage", message);
        mav.addObject("errorStatus",  status);
        mav.setStatus(HttpStatus.valueOf(status));
        return mav;
    }
}
