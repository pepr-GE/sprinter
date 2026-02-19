package com.sprinter.controller;

import com.sprinter.domain.enums.SystemRole;
import com.sprinter.dto.UserDto;
import com.sprinter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro administrátorskou sekci.
 * Přístup jen pro uživatele s rolí ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // ---- Správa uživatelů ----

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        var pageable  = PageRequest.of(page, 20, Sort.by("lastName", "firstName"));
        var usersPage = userService.findAll(search, pageable);

        model.addAttribute("usersPage",    usersPage);
        model.addAttribute("search",       search);
        model.addAttribute("systemRoles",  SystemRole.values());
        model.addAttribute("pageTitle",    "Správa uživatelů");
        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("userDto",     new UserDto());
        model.addAttribute("systemRoles", SystemRole.values());
        model.addAttribute("pageTitle",   "Nový uživatel");
        return "admin/user-form";
    }

    @PostMapping("/users/new")
    public String createUser(@Valid @ModelAttribute("userDto") UserDto dto,
                             BindingResult binding,
                             RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("systemRoles", SystemRole.values());
            model.addAttribute("pageTitle",   "Nový uživatel");
            return "admin/user-form";
        }

        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            binding.rejectValue("password", "", "Heslo je povinné při vytváření uživatele.");
            model.addAttribute("systemRoles", SystemRole.values());
            return "admin/user-form";
        }

        try {
            userService.createUser(dto.getUsername(), dto.getEmail(), dto.getPassword(),
                    dto.getFirstName(), dto.getLastName(), dto.getSystemRole());
            flash.addFlashAttribute("successMessage",
                    "Uživatel '" + dto.getUsername() + "' byl vytvořen.");
            return "redirect:/admin/users";
        } catch (com.sprinter.exception.ValidationException e) {
            binding.reject("", e.getMessage());
            model.addAttribute("systemRoles", SystemRole.values());
            return "admin/user-form";
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        var user = userService.findById(id);

        var dto = new UserDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setSystemRole(user.getSystemRole());

        model.addAttribute("userDto",     dto);
        model.addAttribute("user",        user);
        model.addAttribute("systemRoles", SystemRole.values());
        model.addAttribute("pageTitle",   "Editace uživatele – " + user.getFullName());
        return "admin/user-form";
    }

    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("userDto") UserDto dto,
                             BindingResult binding,
                             RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("user",        userService.findById(id));
            model.addAttribute("systemRoles", SystemRole.values());
            return "admin/user-form";
        }

        try {
            userService.updateUser(id, dto.getUsername(), dto.getEmail(),
                    dto.getFirstName(), dto.getLastName(), dto.getSystemRole());

            // Volitelná změna hesla
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                userService.changePassword(id, null, dto.getPassword());
            }

            flash.addFlashAttribute("successMessage", "Uživatel byl aktualizován.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            binding.reject("", e.getMessage());
            model.addAttribute("user",        userService.findById(id));
            model.addAttribute("systemRoles", SystemRole.values());
            return "admin/user-form";
        }
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id, RedirectAttributes flash) {
        try {
            userService.deactivateUser(id);
            flash.addFlashAttribute("successMessage", "Účet byl deaktivován.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable Long id, RedirectAttributes flash) {
        try {
            userService.activateUser(id);
            flash.addFlashAttribute("successMessage", "Účet byl aktivován.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
