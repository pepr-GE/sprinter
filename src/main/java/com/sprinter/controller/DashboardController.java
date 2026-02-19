package com.sprinter.controller;

import com.sprinter.security.SecurityUtils;
import com.sprinter.service.ProjectService;
import com.sprinter.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * Controller pro hlavní nástěnku (dashboard).
 *
 * <p>Dashboard zobrazuje:
 * <ul>
 *   <li>Rychlý přehled projektů uživatele</li>
 *   <li>Přidělené (a nezavřené) pracovní položky</li>
 *   <li>Položky s překročeným termínem</li>
 *   <li>Nedávno aktualizované položky</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProjectService  projectService;
    private final WorkItemService workItemService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        Long userId = SecurityUtils.getCurrentUserId().orElse(null);

        // Projekty dostupné pro aktuálního uživatele
        model.addAttribute("projects", projectService.findProjectsForCurrentUser());

        // Položky přiřazené uživateli
        if (userId != null) {
            model.addAttribute("myItems",     workItemService.findAssignedToCurrentUser());
        }

        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard/index";
    }
}
