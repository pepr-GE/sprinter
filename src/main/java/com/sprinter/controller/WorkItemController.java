package com.sprinter.controller;

import com.sprinter.domain.enums.*;
import com.sprinter.dto.WorkItemDto;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.*;
import com.sprinter.domain.repository.LabelRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro správu pracovních položek (úkoly, problémy, stories, epicy, články).
 */
@Controller
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;
    private final ProjectService  projectService;
    private final UserService     userService;
    private final SprintService   sprintService;
    private final LabelRepository labelRepository;

    // ---- Formulář nové položky ----

    /**
     * Zobrazí formulář pro vytvoření nové pracovní položky v projektu.
     */
    @GetMapping("/projects/{projectId}/items/new")
    public String newItemForm(@PathVariable Long projectId,
                              @RequestParam(required = false) WorkItemType type,
                              Model model) {
        var project = projectService.findById(projectId);
        projectService.requireContentEditAccess(projectId);

        var dto = new WorkItemDto();
        if (type != null) dto.setType(type);

        addFormAttributes(model, projectId, project.getProjectKey());
        model.addAttribute("workItemDto", dto);
        model.addAttribute("project",     project);
        model.addAttribute("pageTitle",   "Nová položka – " + project.getName());
        return "workitem/form";
    }

    /**
     * Zpracuje odeslání formuláře pro novou položku.
     */
    @PostMapping("/projects/{projectId}/items")
    public String createItem(@PathVariable Long projectId,
                             @Valid @ModelAttribute("workItemDto") WorkItemDto dto,
                             BindingResult binding,
                             RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            var project = projectService.findById(projectId);
            addFormAttributes(model, projectId, project.getProjectKey());
            model.addAttribute("project", project);
            return "workitem/form";
        }

        try {
            var item = workItemService.createWorkItem(
                    projectId, dto.getType(), dto.getTitle(), dto.getDescription(),
                    dto.getPriority(), dto.getAssigneeId(), dto.getParentId(),
                    dto.getStartDate(), dto.getDueDate(), dto.getStoryPoints(),
                    dto.getLabelIds());

            flash.addFlashAttribute("successMessage",
                    "Položka " + item.getItemKey() + " byla vytvořena.");
            return "redirect:/items/" + item.getId();

        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/projects/" + projectId + "/items/new";
        }
    }

    // ---- Detail položky ----

    /**
     * Zobrazí detail pracovní položky.
     */
    @GetMapping("/items/{id}")
    public String itemDetail(@PathVariable Long id, Model model) {
        var item = workItemService.findById(id);
        projectService.requireAccess(item.getProject().getId());

        model.addAttribute("item",              item);
        model.addAttribute("project",           item.getProject());
        model.addAttribute("currentUserRole",
                projectService.getCurrentUserRole(item.getProject().getId()).orElse(null));
        model.addAttribute("currentUserId",
                SecurityUtils.getCurrentUserId().orElse(null));
        model.addAttribute("statuses",          WorkItemStatus.values());
        model.addAttribute("priorities",        Priority.values());
        model.addAttribute("projectMembers",    userService.findProjectMembers(item.getProject().getId()));
        model.addAttribute("pageTitle",         item.getItemKey() + " – " + item.getTitle());
        return "workitem/detail";
    }

    /**
     * Zobrazí detail přes klíč projektu a číslo (např. /PROJ-42).
     */
    @GetMapping("/{projectKey}-{itemNumber:\\d+}")
    public String itemDetailByKey(@PathVariable String projectKey,
                                  @PathVariable Long itemNumber) {
        var item = workItemService.findByProjectKeyAndNumber(projectKey, itemNumber);
        return "redirect:/items/" + item.getId();
    }

    // ---- Editace položky ----

    @GetMapping("/items/{id}/edit")
    public String editItemForm(@PathVariable Long id, Model model) {
        var item = workItemService.findById(id);
        var projectId = item.getProject().getId();
        projectService.requireContentEditAccess(projectId);

        var dto = new WorkItemDto();
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setType(item.getType());
        dto.setPriority(item.getPriority());
        dto.setAssigneeId(item.getAssignee() != null ? item.getAssignee().getId() : null);
        dto.setParentId(item.getParent() != null ? item.getParent().getId() : null);
        dto.setStartDate(item.getStartDate());
        dto.setDueDate(item.getDueDate());
        dto.setStoryPoints(item.getStoryPoints());
        dto.setEstimatedHours(item.getEstimatedHours());
        item.getLabels().forEach(l -> dto.getLabelIds().add(l.getId()));

        addFormAttributes(model, projectId, item.getProject().getProjectKey());
        model.addAttribute("workItemDto", dto);
        model.addAttribute("item",        item);
        model.addAttribute("project",     item.getProject());
        model.addAttribute("pageTitle",   "Editace " + item.getItemKey());
        return "workitem/form";
    }

    @PostMapping("/items/{id}/edit")
    public String updateItem(@PathVariable Long id,
                             @Valid @ModelAttribute("workItemDto") WorkItemDto dto,
                             BindingResult binding,
                             RedirectAttributes flash, Model model) {
        var item = workItemService.findById(id);

        if (binding.hasErrors()) {
            addFormAttributes(model, item.getProject().getId(), item.getProject().getProjectKey());
            model.addAttribute("item",    item);
            model.addAttribute("project", item.getProject());
            return "workitem/form";
        }

        try {
            workItemService.updateWorkItem(id, dto.getTitle(), dto.getDescription(),
                    dto.getPriority(), dto.getAssigneeId(), dto.getStartDate(),
                    dto.getDueDate(), dto.getStoryPoints(), dto.getEstimatedHours(),
                    dto.getLabelIds());
            flash.addFlashAttribute("successMessage", "Položka byla aktualizována.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/items/" + id;
    }

    // ---- Změna stavu ----

    @PostMapping("/items/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam WorkItemStatus status,
                               @RequestParam(defaultValue = "") String returnTo,
                               RedirectAttributes flash) {
        try {
            workItemService.changeStatus(id, status);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return returnTo.isBlank() ? "redirect:/items/" + id : "redirect:" + returnTo;
    }

    // ---- Komentáře ----

    @PostMapping("/items/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             RedirectAttributes flash) {
        try {
            workItemService.addComment(id, content);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/items/" + id + "#comments";
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam Long workItemId,
                                RedirectAttributes flash) {
        try {
            workItemService.deleteComment(commentId);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/items/" + workItemId + "#comments";
    }

    // ---- Smazání položky ----

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes flash) {
        var item = workItemService.findById(id);
        Long projectId = item.getProject().getId();

        try {
            workItemService.deleteWorkItem(id);
            flash.addFlashAttribute("successMessage", "Položka byla smazána.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/items/" + id;
        }

        return "redirect:/projects/" + projectId + "/backlog";
    }

    // ---- Pomocné metody ----

    private void addFormAttributes(Model model, Long projectId, String projectKey) {
        model.addAttribute("workItemTypes",  WorkItemType.values());
        model.addAttribute("priorities",     Priority.values());
        model.addAttribute("projectMembers", userService.findProjectMembers(projectId));
        model.addAttribute("labels",         labelRepository.findByProjectIdOrProjectIsNullOrderByNameAsc(projectId));
        model.addAttribute("sprints",        sprintService.findByProject(projectId));
    }
}
