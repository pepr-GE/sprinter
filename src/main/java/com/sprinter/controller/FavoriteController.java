package com.sprinter.controller;

import com.sprinter.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro správu oblíbených položek.
 */
@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** Přidá položku do oblíbených. */
    @PostMapping("/add")
    public String add(@RequestParam String entityType,
                      @RequestParam Long entityId,
                      @RequestParam String title,
                      @RequestParam String url,
                      @RequestParam(required = false, defaultValue = "bi-star") String icon,
                      @RequestParam(defaultValue = "") String returnTo,
                      RedirectAttributes flash) {
        try {
            favoriteService.addFavorite(entityType, entityId, title, url, icon);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return returnTo.isBlank() ? "redirect:/" : "redirect:" + returnTo;
    }

    /** Odebere položku z oblíbených podle entitního typu + ID. */
    @PostMapping("/remove")
    public String remove(@RequestParam String entityType,
                         @RequestParam Long entityId,
                         @RequestParam(defaultValue = "") String returnTo,
                         RedirectAttributes flash) {
        try {
            favoriteService.removeFavorite(entityType, entityId);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return returnTo.isBlank() ? "redirect:/" : "redirect:" + returnTo;
    }

    /** Odebere položku z oblíbených podle ID záznamu. */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String returnTo,
                         RedirectAttributes flash) {
        try {
            favoriteService.removeFavoriteById(id);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return returnTo.isBlank() ? "redirect:/" : "redirect:" + returnTo;
    }
}
