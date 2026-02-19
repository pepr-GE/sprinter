package com.sprinter.security;

import com.sprinter.domain.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Pomocné statické metody pro práci s aktuálně přihlášeným uživatelem.
 */
public final class SecurityUtils {

    private SecurityUtils() { /* utility třída – neinstaciovat */ }

    /**
     * Vrátí {@link SprinterUserDetails} aktuálně přihlášeného uživatele.
     *
     * @return Optional s detaily uživatele, nebo prázdný Optional pokud není nikdo přihlášen
     */
    public static Optional<SprinterUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof SprinterUserDetails ud) {
            return Optional.of(ud);
        }
        return Optional.empty();
    }

    /**
     * Vrátí doménového uživatele aktuálně přihlášeného.
     *
     * @return Optional s uživatelem
     */
    public static Optional<User> getCurrentUser() {
        return getCurrentUserDetails().map(SprinterUserDetails::getUser);
    }

    /**
     * Vrátí ID aktuálně přihlášeného uživatele.
     *
     * @return Optional s ID uživatele
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUserDetails().map(SprinterUserDetails::getUserId);
    }

    /**
     * Vrátí true, pokud je aktuálně přihlášený uživatel správce systému.
     */
    public static boolean isCurrentUserAdmin() {
        return getCurrentUser().map(User::isAdmin).orElse(false);
    }
}
