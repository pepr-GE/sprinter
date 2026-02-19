package com.sprinter.controller;

import com.sprinter.domain.entity.Project;
import com.sprinter.domain.enums.ProjectRole;
import com.sprinter.domain.enums.ProjectStatus;
import com.sprinter.dto.ProjectDto;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controller pro správu projektů a podprojektů.
 *
 * <p>Zpracovává HTTP požadavky pro:
 * <ul>
 *   <li>Seznam projektů</li>
 *   <li>Detail projektu (přehled, board, backlog, Gantt, reporty)</li>
 *   <li>Vytváření a editaci projektů/podprojektů</li>
 *   <li>Správu projektového týmu</li>
 * </ul>
 */
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService  projectService;
    private final SprintService   sprintService;
    private final WorkItemService workItemService;
    private final ReportService   reportService;
    private final UserService     userService;

    // ---- Seznam projektů ----

    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects",  projectService.findProjectsForCurrentUser());
        model.addAttribute("pageTitle", "Projekty");
        return "project/list";
    }

    // ---- Detail projektu ----

    /**
     * Přehled projektu – základní informace, statistiky, podprojekty.
     */
    @GetMapping("/{id}")
    public String projectOverview(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("subprojects",       projectService.findSubprojects(id));
        model.addAttribute("statusCounts",      reportService.getProjectStatusCounts(id));
        model.addAttribute("completionPercent", reportService.getProjectCompletionPercent(id));
        model.addAttribute("activeSprint",      sprintService.findActiveSprint(id).orElse(null));
        model.addAttribute("members",           projectService.findMembers(id));
        model.addAttribute("pageTitle",         project.getName());
        model.addAttribute("activeTab",         "overview");
        return "project/overview";
    }

    /**
     * Kanban board projektu.
     */
    @GetMapping("/{id}/board")
    public String projectBoard(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        var activeSprint = sprintService.findActiveSprint(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("activeSprint",    activeSprint.orElse(null));
        model.addAttribute("hasActiveSprint", activeSprint.isPresent());
        model.addAttribute("activeTab",       "board");
        model.addAttribute("pageTitle",       project.getName() + " – Board");

        if (activeSprint.isPresent()) {
            model.addAttribute("boardItems", workItemService.findBySprint(activeSprint.get().getId()));
        }

        return "project/board";
    }

    /**
     * Backlog projektu.
     */
    @GetMapping("/{id}/backlog")
    public String projectBacklog(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("backlogItems", workItemService.findBacklog(id));
        model.addAttribute("sprints",      sprintService.findByProject(id));
        model.addAttribute("activeTab",    "backlog");
        model.addAttribute("pageTitle",    project.getName() + " – Backlog");
        return "project/backlog";
    }

    /**
     * Ganttův diagram projektu.
     */
    @GetMapping("/{id}/gantt")
    public String projectGantt(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("ganttItems", workItemService.findGanttItems(id));
        model.addAttribute("activeTab",  "gantt");
        model.addAttribute("pageTitle",  project.getName() + " – Gantt");
        return "project/gantt";
    }

    /**
     * Reporty projektu.
     */
    @GetMapping("/{id}/reports")
    public String projectReports(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("statusCounts",      reportService.getProjectStatusCounts(id));
        model.addAttribute("completionPercent", reportService.getProjectCompletionPercent(id));
        model.addAttribute("sprints",           sprintService.findByProject(id));
        model.addAttribute("activeTab",         "reports");
        model.addAttribute("pageTitle",         project.getName() + " – Reporty");
        return "project/reports";
    }

    /**
     * Správa projektového týmu.
     */
    @GetMapping("/{id}/team")
    public String projectTeam(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("members",         projectService.findMembers(id));
        model.addAttribute("availableUsers",  userService.findUsersNotInProject(id));
        model.addAttribute("projectRoles",    ProjectRole.values());
        model.addAttribute("activeTab",       "team");
        model.addAttribute("pageTitle",       project.getName() + " – Tým");
        return "project/team";
    }

    /**
     * Nastavení projektu.
     */
    @GetMapping("/{id}/settings")
    public String projectSettings(@PathVariable Long id, Model model) {
        var project = projectService.findById(id);
        projectService.requireManageAccess(id);

        addProjectCommonAttributes(model, project);
        model.addAttribute("projectStatuses", ProjectStatus.values());
        model.addAttribute("projectDto",      toDto(project));
        model.addAttribute("activeTab",       "settings");
        model.addAttribute("pageTitle",       project.getName() + " – Nastavení");
        return "project/settings";
    }

    // ---- Vytváření projektu ----

    @GetMapping("/new")
    public String newProjectForm(Model model) {
        model.addAttribute("projectDto",  new ProjectDto());
        model.addAttribute("users",       userService.findAll(null, org.springframework.data.domain.Pageable.unpaged()).getContent());
        model.addAttribute("parentId",    null);
        model.addAttribute("pageTitle",   "Nový projekt");
        return "project/form";
    }

    @GetMapping("/{parentId}/new-subproject")
    public String newSubprojectForm(@PathVariable Long parentId, Model model) {
        var parent = projectService.findById(parentId);
        projectService.requireManageAccess(parentId);

        model.addAttribute("projectDto", new ProjectDto());
        model.addAttribute("parent",     parent);
        model.addAttribute("parentId",   parentId);
        model.addAttribute("pageTitle",  "Nový podprojekt");
        return "project/form";
    }

    @PostMapping("/new")
    public String createProject(@Valid @ModelAttribute("projectDto") ProjectDto dto,
                                BindingResult binding,
                                @RequestParam(required = false) Long parentId,
                                RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Nový projekt");
            return "project/form";
        }

        try {
            var currentUser = SecurityUtils.getCurrentUser()
                    .orElseThrow(() -> new com.sprinter.exception.AccessDeniedException());

            Project project;
            if (parentId != null) {
                project = projectService.createSubproject(parentId,
                        dto.getName(), dto.getProjectKey(), dto.getDescription(),
                        currentUser, dto.getStartDate(), dto.getEndDate());
            } else {
                project = projectService.createProject(dto.getName(),
                        dto.getProjectKey(), dto.getDescription(),
                        currentUser, dto.getStartDate(), dto.getEndDate());
            }

            flash.addFlashAttribute("successMessage",
                    "Projekt '" + project.getName() + "' byl úspěšně vytvořen.");
            return "redirect:/projects/" + project.getId();

        } catch (com.sprinter.exception.ValidationException e) {
            binding.reject("", e.getMessage());
            model.addAttribute("pageTitle", "Nový projekt");
            return "project/form";
        }
    }

    // ---- Aktualizace projektu ----

    @PostMapping("/{id}/settings")
    public String updateProject(@PathVariable Long id,
                                @Valid @ModelAttribute("projectDto") ProjectDto dto,
                                BindingResult binding,
                                RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            var project = projectService.findById(id);
            addProjectCommonAttributes(model, project);
            model.addAttribute("projectStatuses", ProjectStatus.values());
            model.addAttribute("activeTab", "settings");
            return "project/settings";
        }

        try {
            projectService.updateProject(id, dto.getName(), dto.getDescription(),
                    dto.getStatus(), dto.getStartDate(), dto.getEndDate());
            flash.addFlashAttribute("successMessage", "Projekt byl úspěšně aktualizován.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/projects/" + id + "/settings";
    }

    // ---- Správa členů ----

    @PostMapping("/{id}/team/add")
    public String addTeamMember(@PathVariable Long id,
                                @RequestParam Long userId,
                                @RequestParam ProjectRole role,
                                RedirectAttributes flash) {
        try {
            projectService.requireManageAccess(id);
            projectService.addMember(id, userId, role);
            flash.addFlashAttribute("successMessage", "Člen byl přidán do týmu.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/projects/" + id + "/team";
    }

    @PostMapping("/{id}/team/{userId}/role")
    public String updateMemberRole(@PathVariable Long id,
                                   @PathVariable Long userId,
                                   @RequestParam ProjectRole role,
                                   RedirectAttributes flash) {
        try {
            projectService.requireManageAccess(id);
            projectService.updateMemberRole(id, userId, role);
            flash.addFlashAttribute("successMessage", "Role člena byla aktualizována.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/projects/" + id + "/team";
    }

    @PostMapping("/{id}/team/{userId}/remove")
    public String removeTeamMember(@PathVariable Long id,
                                   @PathVariable Long userId,
                                   RedirectAttributes flash) {
        try {
            projectService.removeMember(id, userId);
            flash.addFlashAttribute("successMessage", "Člen byl odebrán z týmu.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/projects/" + id + "/team";
    }

    // ---- Archivace ----

    @PostMapping("/{id}/archive")
    public String archiveProject(@PathVariable Long id, RedirectAttributes flash) {
        try {
            projectService.archiveProject(id);
            flash.addFlashAttribute("successMessage", "Projekt byl archivován.");
            return "redirect:/projects";
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/projects/" + id + "/settings";
        }
    }

    // ---- Pomocné metody ----

    private void addProjectCommonAttributes(Model model, Project project) {
        model.addAttribute("project",         project);
        model.addAttribute("currentUserRole", projectService.getCurrentUserRole(project.getId()).orElse(null));
        model.addAttribute("subprojects",     projectService.findSubprojects(project.getId()));
    }

    private ProjectDto toDto(Project project) {
        var dto = new ProjectDto();
        dto.setName(project.getName());
        dto.setProjectKey(project.getProjectKey());
        dto.setDescription(project.getDescription());
        dto.setStatus(project.getStatus());
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        return dto;
    }
}
