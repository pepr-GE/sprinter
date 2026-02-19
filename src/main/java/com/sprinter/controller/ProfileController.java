package com.sprinter.controller;

import com.sprinter.security.SecurityUtils;
import com.sprinter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro správu uživatelského profilu a preferencí.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String profilePage(Model model) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.sprinter.exception.AccessDeniedException());
        model.addAttribute("user",      userService.findById(userId));
        model.addAttribute("pageTitle", "Můj profil");
        return "profile/index";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes flash) {
        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("errorMessage", "Nová hesla se neshodují.");
            return "redirect:/profile";
        }

        try {
            Long userId = SecurityUtils.getCurrentUserId()
                    .orElseThrow(() -> new com.sprinter.exception.AccessDeniedException());
            userService.changePassword(userId, currentPassword, newPassword);
            flash.addFlashAttribute("successMessage", "Heslo bylo úspěšně změněno.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }

    /**
     * Změní UI téma (light/dark) – voláno AJAX požadavkem nebo formulářem.
     */
    @PostMapping("/theme")
    public String changeTheme(@RequestParam String theme,
                              @RequestParam(defaultValue = "/dashboard") String returnTo,
                              RedirectAttributes flash) {
        SecurityUtils.getCurrentUserId().ifPresent(userId ->
                userService.updateTheme(userId, theme));
        return "redirect:" + returnTo;
    }
}
