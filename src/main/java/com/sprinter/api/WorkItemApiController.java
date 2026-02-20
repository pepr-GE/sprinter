package com.sprinter.api;

import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.domain.repository.DocumentRepository;
import com.sprinter.domain.repository.ProjectRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.ProjectService;
import com.sprinter.service.SprintService;
import com.sprinter.service.WorkItemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller pro pracovní položky.
 *
 * <p>Používán z JavaScriptu pro:
 * <ul>
 *   <li>Drag &amp; drop na Kanban tabuli (změna stavu)</li>
 *   <li>Přesouvání položek mezi sprintem a backlogem</li>
 *   <li>AJAX aktualizace bez znovunačtení stránky</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkItemApiController {

    private final WorkItemService    workItemService;
    private final SprintService      sprintService;
    private final ProjectService     projectService;
    private final ProjectRepository  projectRepository;
    private final WorkItemRepository workItemRepository;
    private final DocumentRepository documentRepository;

    /**
     * Změní stav pracovní položky (pro Kanban drag &amp; drop).
     */
    @PatchMapping("/work-items/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest req) {
        try {
            var item = workItemService.changeStatus(id, req.getStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "id",      item.getId(),
                    "status",  item.getStatus().name(),
                    "key",     item.getItemKey()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error",   e.getMessage()
            ));
        }
    }

    /**
     * Přiřadí položku do sprintu nebo přesune do backlogu.
     */
    @PatchMapping("/work-items/{id}/sprint")
    public ResponseEntity<Map<String, Object>> updateSprint(
            @PathVariable Long id,
            @RequestBody SprintUpdateRequest req) {
        try {
            if (req.getSprintId() != null) {
                sprintService.addWorkItemToSprint(req.getSprintId(), id);
            } else {
                sprintService.removeWorkItemFromSprint(id);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Vrátí položky sprintu jako JSON (pro inicializaci Kanban tabule).
     */
    @GetMapping("/sprints/{sprintId}/items")
    public ResponseEntity<List<WorkItemSummary>> getSprintItems(@PathVariable Long sprintId) {
        var items = workItemService.findBySprint(sprintId).stream()
                .map(WorkItemSummary::fromEntity)
                .toList();
        return ResponseEntity.ok(items);
    }

    /**
     * Globální vyhledávání – projekty a pracovní položky přístupné přihlášenému uživateli.
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResults> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(new SearchResults());
        }
        String term = q.trim();

        // Projekty dostupné aktuálnímu uživateli
        var userId   = SecurityUtils.getCurrentUserId().orElse(null);
        var projects = userId != null
                ? projectRepository.searchProjects(term).stream()
                    .filter(p -> projectService.getCurrentUserRole(p.getId()).isPresent())
                    .limit(5).toList()
                : java.util.List.<com.sprinter.domain.entity.Project>of();

        // Pracovní položky v dostupných projektech
        var accessibleProjectIds = userId != null
                ? projectRepository.findProjectsForUser(userId).stream()
                    .map(com.sprinter.domain.entity.Project::getId).toList()
                : java.util.List.<Long>of();

        var workItems = accessibleProjectIds.isEmpty()
                ? java.util.List.<WorkItem>of()
                : workItemRepository.searchInProjects(accessibleProjectIds, term, PageRequest.of(0, 8));

        // Dokumenty v dostupných projektech
        var documents = accessibleProjectIds.isEmpty()
                ? java.util.List.<com.sprinter.domain.entity.Document>of()
                : documentRepository.searchInProjects(accessibleProjectIds, term).stream()
                    .limit(5).toList();

        var result = new SearchResults();
        result.projects  = projects.stream().map(p -> {
            var r = new SearchResults.ProjectResult();
            r.id   = p.getId();
            r.name = p.getName();
            r.key  = p.getProjectKey();
            r.url  = "/projects/" + p.getId();
            return r;
        }).toList();
        result.items = workItems.stream().map(wi -> {
            var r = new SearchResults.ItemResult();
            r.id          = wi.getId();
            r.key         = wi.getItemKey();
            r.title       = wi.getTitle();
            r.type        = wi.getType().name();
            r.typeLabel   = wi.getType().getDisplayName();
            r.status      = wi.getStatus().getDisplayName();
            r.projectName = wi.getProject().getName();
            r.url         = "/items/" + wi.getId();
            return r;
        }).toList();
        result.documents = documents.stream().map(d -> {
            var r = new SearchResults.DocumentResult();
            r.id          = d.getId();
            r.title       = d.getTitle();
            r.projectName = d.getProject() != null ? d.getProject().getName() : null;
            r.url         = "/documents/" + d.getId();
            return r;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Vrátí položky pro Ganttův diagram jako JSON.
     */
    @GetMapping("/projects/{projectId}/gantt-items")
    public ResponseEntity<List<GanttItem>> getGanttItems(@PathVariable Long projectId) {
        var items = workItemService.findGanttItems(projectId).stream()
                .map(GanttItem::fromEntity)
                .toList();
        return ResponseEntity.ok(items);
    }

    // ---- Vnitřní DTO třídy pro API ----

    @Data
    public static class StatusUpdateRequest {
        private WorkItemStatus status;
    }

    @Data
    public static class SprintUpdateRequest {
        private Long sprintId;  // null = backlog
    }

    @Data
    public static class WorkItemSummary {
        private Long   id;
        private String key;
        private String title;
        private String status;
        private String type;
        private String priority;
        private String assigneeName;
        private String assigneeInitials;
        private Integer storyPoints;
        private String dueDate;

        public static WorkItemSummary fromEntity(WorkItem wi) {
            var s = new WorkItemSummary();
            s.id              = wi.getId();
            s.key             = wi.getItemKey();
            s.title           = wi.getTitle();
            s.status          = wi.getStatus().name();
            s.type            = wi.getType().name();
            s.priority        = wi.getPriority().name();
            s.storyPoints     = wi.getStoryPoints();
            s.dueDate         = wi.getDueDate() != null ? wi.getDueDate().toString() : null;
            if (wi.getAssignee() != null) {
                s.assigneeName     = wi.getAssignee().getFullName();
                s.assigneeInitials = wi.getAssignee().getInitials();
            }
            return s;
        }
    }

    @Data
    public static class SearchResults {
        public java.util.List<ProjectResult>  projects  = new java.util.ArrayList<>();
        public java.util.List<ItemResult>     items     = new java.util.ArrayList<>();
        public java.util.List<DocumentResult> documents = new java.util.ArrayList<>();

        @Data
        public static class ProjectResult {
            public Long   id;
            public String name;
            public String key;
            public String url;
        }
        @Data
        public static class ItemResult {
            public Long   id;
            public String key;
            public String title;
            public String type;
            public String typeLabel;
            public String status;
            public String projectName;
            public String url;
        }
        @Data
        public static class DocumentResult {
            public Long   id;
            public String title;
            public String projectName;
            public String url;
        }
    }

    @Data
    public static class GanttItem {
        private String id;
        private String text;
        private String startDate;
        private String endDate;
        private int    progress;
        private String parent;
        private String type;
        private String status;
        private String priority;
        private String url;

        public static GanttItem fromEntity(WorkItem wi) {
            var g = new GanttItem();
            g.id        = "wi-" + wi.getId();
            g.text      = "[" + wi.getItemKey() + "] " + wi.getTitle();
            g.startDate = wi.getStartDate()    != null ? wi.getStartDate().toString()    : null;
            g.endDate   = wi.getDueDate()      != null ? wi.getDueDate().toString()      : null;
            g.progress  = wi.getProgressPct() != null ? wi.getProgressPct() : (wi.isDone() ? 100 : 0);
            g.parent    = wi.getParent()       != null ? "wi-" + wi.getParent().getId()  : null;
            g.type      = wi.getType().name();
            g.status    = wi.getStatus().name();
            g.priority  = wi.getPriority().name();
            g.url       = "/items/" + wi.getId();
            return g;
        }
    }
}
