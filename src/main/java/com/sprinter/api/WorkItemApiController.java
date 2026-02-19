package com.sprinter.api;

import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.service.SprintService;
import com.sprinter.service.WorkItemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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

    private final WorkItemService workItemService;
    private final SprintService   sprintService;

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
            g.progress  = wi.isDone() ? 100 : 0;
            g.parent    = wi.getParent()       != null ? "wi-" + wi.getParent().getId()  : null;
            g.type      = wi.getType().name();
            g.status    = wi.getStatus().name();
            g.priority  = wi.getPriority().name();
            g.url       = "/items/" + wi.getId();
            return g;
        }
    }
}
