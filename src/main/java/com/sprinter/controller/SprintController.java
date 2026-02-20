package com.sprinter.controller;

import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.dto.SprintDto;
import com.sprinter.service.ProjectService;
import com.sprinter.service.SprintService;
import com.sprinter.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro správu sprintů.
 */
@Controller
@RequiredArgsConstructor
public class SprintController {

    private final SprintService   sprintService;
    private final ProjectService  projectService;
    private final WorkItemService workItemService;

    // ---- Formulář nového sprintu ----

    @GetMapping("/projects/{projectId}/sprints/new")
    public String newSprintForm(@PathVariable Long projectId, Model model) {
        projectService.requireManageAccess(projectId);
        var project = projectService.findById(projectId);

        model.addAttribute("sprintDto",  new SprintDto());
        model.addAttribute("project",    project);
        model.addAttribute("pageTitle",  "Nový sprint – " + project.getName());
        return "sprint/form";
    }

    @PostMapping("/projects/{projectId}/sprints")
    public String createSprint(@PathVariable Long projectId,
                               @Valid @ModelAttribute("sprintDto") SprintDto dto,
                               BindingResult binding,
                               RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("project", projectService.findById(projectId));
            return "sprint/form";
        }

        try {
            var sprint = sprintService.createSprint(projectId, dto.getName(), dto.getGoal(),
                    dto.getStartDate(), dto.getEndDate());
            flash.addFlashAttribute("successMessage",
                    "Sprint '" + sprint.getName() + "' byl vytvořen.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/projects/" + projectId + "/backlog";
    }

    // ---- Detail sprintu ----

    @GetMapping("/sprints/{id}")
    public String sprintDetail(@PathVariable Long id, Model model) {
        var sprint  = sprintService.findById(id);
        var project = sprint.getProject();
        projectService.requireAccess(project.getId());

        var boardItems = workItemService.findBySprint(id);
        long doneCount   = boardItems.stream().filter(i -> i.getStatus() == WorkItemStatus.DONE).count();
        int  totalPoints = boardItems.stream().mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();
        int  donePoints  = boardItems.stream().filter(i -> i.getStatus() == WorkItemStatus.DONE)
                                     .mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum();

        model.addAttribute("sprint",          sprint);
        model.addAttribute("project",         project);
        model.addAttribute("boardItems",      boardItems);
        model.addAttribute("doneCount",       doneCount);
        model.addAttribute("totalPoints",     totalPoints);
        model.addAttribute("donePoints",      donePoints);
        model.addAttribute("allSprints",      sprintService.findByProject(project.getId()));
        model.addAttribute("currentUserRole", projectService.getCurrentUserRole(project.getId()).orElse(null));
        model.addAttribute("pageTitle",       sprint.getName());
        return "sprint/detail";
    }

    // ---- Editace sprintu ----

    @GetMapping("/sprints/{id}/edit")
    public String editSprintForm(@PathVariable Long id, Model model) {
        var sprint = sprintService.findById(id);
        projectService.requireManageAccess(sprint.getProject().getId());

        var dto = new SprintDto();
        dto.setName(sprint.getName());
        dto.setGoal(sprint.getGoal());
        dto.setStartDate(sprint.getStartDate());
        dto.setEndDate(sprint.getEndDate());

        model.addAttribute("sprintDto", dto);
        model.addAttribute("sprint",    sprint);
        model.addAttribute("project",   sprint.getProject());
        model.addAttribute("pageTitle", "Editace sprintu – " + sprint.getName());
        return "sprint/form";
    }

    @PostMapping("/sprints/{id}/edit")
    public String updateSprint(@PathVariable Long id,
                               @Valid @ModelAttribute("sprintDto") SprintDto dto,
                               BindingResult binding,
                               RedirectAttributes flash, Model model) {
        var sprint = sprintService.findById(id);

        if (binding.hasErrors()) {
            model.addAttribute("sprint",  sprint);
            model.addAttribute("project", sprint.getProject());
            return "sprint/form";
        }

        try {
            sprintService.updateSprint(id, dto.getName(), dto.getGoal(),
                    dto.getStartDate(), dto.getEndDate());
            flash.addFlashAttribute("successMessage", "Sprint byl aktualizován.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/sprints/" + id;
    }

    // ---- Spuštění, dokončení, zrušení ----

    @PostMapping("/sprints/{id}/start")
    public String startSprint(@PathVariable Long id, RedirectAttributes flash) {
        try {
            var sprint = sprintService.startSprint(id);
            flash.addFlashAttribute("successMessage",
                    "Sprint '" + sprint.getName() + "' byl spuštěn.");
            return "redirect:/projects/" + sprint.getProject().getId() + "/board";
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/sprints/" + id;
        }
    }

    @PostMapping("/sprints/{id}/complete")
    public String completeSprint(@PathVariable Long id,
                                 @RequestParam(required = false) Long moveToSprintId,
                                 RedirectAttributes flash) {
        try {
            var sprint = sprintService.completeSprint(id, moveToSprintId);
            flash.addFlashAttribute("successMessage",
                    "Sprint '" + sprint.getName() + "' byl uzavřen.");
            return "redirect:/projects/" + sprint.getProject().getId() + "/backlog";
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/sprints/" + id;
        }
    }

    @PostMapping("/sprints/{id}/cancel")
    public String cancelSprint(@PathVariable Long id, RedirectAttributes flash) {
        try {
            var sprint = sprintService.findById(id);
            Long projectId = sprint.getProject().getId();
            sprintService.cancelSprint(id);
            flash.addFlashAttribute("successMessage", "Sprint byl zrušen.");
            return "redirect:/projects/" + projectId + "/backlog";
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/sprints/" + id;
        }
    }
}
