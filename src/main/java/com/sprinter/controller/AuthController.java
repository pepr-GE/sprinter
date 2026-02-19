package com.sprinter.controller;

import com.sprinter.security.SprinterUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller pro autentizaci (přihlašovací stránka).
 * Samotné zpracování přihlašovacího formuláře obstarává Spring Security.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /**
     * Zobrazí přihlašovací stránku.
     *
     * @param error  parametr přítomný při chybě přihlášení
     * @param logout parametr přítomný po odhlášení
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error",  required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        // Pokud je uživatel již přihlášen, přesměrujeme na dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof SprinterUserDetails) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage",
                    "Nesprávné přihlašovací jméno nebo heslo. Zkuste to znovu.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Byl jste úspěšně odhlášen.");
        }

        return "auth/login";
    }
}
