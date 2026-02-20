package com.sprinter.controller;

import com.sprinter.domain.repository.CommentRepository;
import com.sprinter.domain.repository.DocumentRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.dto.ActivityEntry;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Zobrazuje kompletní seznam změn a nových objektů (activity feed).
 */
@Controller
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ProjectService     projectService;
    private final WorkItemRepository workItemRepository;
    private final DocumentRepository documentRepository;
    private final CommentRepository  commentRepository;

    @GetMapping
    public String activityList(Model model) {
        var user = SecurityUtils.getCurrentUser().orElse(null);
        if (user == null) return "redirect:/login";

        var projectIds = projectService.findProjectsForCurrentUser()
                .stream().map(p -> p.getId()).toList();

        // Zobrazit posledních 30 dní pokud není previous login
        LocalDateTime since = user.getPreviousLastLoginAt() != null
                ? user.getPreviousLastLoginAt()
                : LocalDateTime.now().minusDays(30);

        var entries  = new ArrayList<ActivityEntry>();
        var pageable = PageRequest.of(0, 50);

        if (!projectIds.isEmpty()) {
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
                            c.getContent().length() > 120
                                    ? c.getContent().substring(0, 120) + "…"
                                    : c.getContent(),
                            "/items/" + c.getWorkItem().getId() + "#comments",
                            c.getCreatedAt(),
                            c.getAuthor().getFullName(),
                            "bi-chat-left-text",
                            "wi-type-task"
                    )));
        }

        Collections.sort(entries);

        model.addAttribute("activityEntries", entries);
        model.addAttribute("activitySince",   since);
        model.addAttribute("pageTitle",       "Přehled změn");
        model.addAttribute("activeNav",       "dashboard");
        return "activity/index";
    }
}
