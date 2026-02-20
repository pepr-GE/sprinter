package com.sprinter.controller;

import com.sprinter.domain.repository.CommentRepository;
import com.sprinter.domain.repository.DocumentRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.dto.ActivityEntry;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.ProjectService;
import com.sprinter.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Controller pro hlavní nástěnku (dashboard).
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProjectService     projectService;
    private final WorkItemService    workItemService;
    private final WorkItemRepository workItemRepository;
    private final DocumentRepository documentRepository;
    private final CommentRepository  commentRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        var userId = SecurityUtils.getCurrentUserId().orElse(null);

        model.addAttribute("projects",  projectService.findProjectsForCurrentUser());
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeNav", "dashboard");

        if (userId != null) {
            model.addAttribute("myItems", workItemService.findAssignedToCurrentUser());
            addActivityFeed(model);
        }

        return "dashboard/index";
    }

    private void addActivityFeed(Model model) {
        var projectIds = projectService.findProjectsForCurrentUser()
                .stream().map(p -> p.getId()).toList();
        if (projectIds.isEmpty()) return;

        // Zobrazit posledních 7 dní od všech uživatelů v přístupných projektech
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        var pageable = PageRequest.of(0, 10);
        var entries  = new ArrayList<ActivityEntry>();

        workItemRepository.findRecentlyUpdatedInProjects(projectIds, since, pageable)
                .forEach(wi -> entries.add(new ActivityEntry(
                        "work_item",
                        wi.getItemKey(),
                        wi.getTitle(),
                        "/items/" + wi.getId(),
                        wi.getUpdatedAt() != null ? wi.getUpdatedAt() : wi.getCreatedAt(),
                        wi.getReporter() != null ? wi.getReporter().getFullName() : "–",
                        wi.getType().getIconClass(),
                        wi.getType().getCssClass()
                )));

        documentRepository.findRecentlyUpdatedInProjects(projectIds, since, pageable)
                .forEach(doc -> entries.add(new ActivityEntry(
                        "document",
                        null,
                        doc.getTitle(),
                        "/documents/" + doc.getId(),
                        doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt(),
                        doc.getAuthor().getFullName(),
                        "bi-file-earmark-text",
                        "wi-type-task"
                )));

        commentRepository.findRecentInProjects(projectIds, since, pageable)
                .forEach(c -> entries.add(new ActivityEntry(
                        "comment",
                        c.getWorkItem().getItemKey(),
                        c.getContent().length() > 80
                                ? c.getContent().substring(0, 80) + "…"
                                : c.getContent(),
                        "/items/" + c.getWorkItem().getId() + "#comments",
                        c.getCreatedAt(),
                        c.getAuthor().getFullName(),
                        "bi-chat-left-text",
                        "wi-type-task"
                )));

        Collections.sort(entries);
        model.addAttribute("recentActivity", entries.stream().limit(10).toList());
    }
}
