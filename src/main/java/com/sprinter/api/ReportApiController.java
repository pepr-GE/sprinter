package com.sprinter.api;

import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API controller pro reporty – vrací data pro Chart.js.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    /**
     * Vrátí počty položek dle stavu pro pie/bar chart (projekt).
     */
    @GetMapping("/projects/{projectId}/status-counts")
    public ResponseEntity<Map<String, Object>> projectStatusCounts(@PathVariable Long projectId) {
        var counts  = reportService.getProjectStatusCounts(projectId);
        var labels  = new java.util.ArrayList<String>();
        var data    = new java.util.ArrayList<Long>();
        var colors  = new java.util.ArrayList<String>();

        Map<WorkItemStatus, String> statusColors = Map.of(
                WorkItemStatus.TO_DO,       "#6c757d",
                WorkItemStatus.IN_PROGRESS, "#6DA3C7",
                WorkItemStatus.IN_REVIEW,   "#f59e0b",
                WorkItemStatus.DONE,        "#22c55e",
                WorkItemStatus.CANCELLED,   "#ef4444"
        );

        for (var entry : counts.entrySet()) {
            labels.add(entry.getKey().getDisplayName());
            data.add(entry.getValue());
            colors.add(statusColors.getOrDefault(entry.getKey(), "#999"));
        }

        return ResponseEntity.ok(Map.of(
                "labels",           labels,
                "data",             data,
                "backgroundColor",  colors,
                "completion",       reportService.getProjectCompletionPercent(projectId)
        ));
    }

    /**
     * Vrátí story points dle stavu pro sprint burn-down.
     */
    @GetMapping("/sprints/{sprintId}/points")
    public ResponseEntity<Map<String, Object>> sprintPoints(@PathVariable Long sprintId) {
        var points = reportService.getSprintPointsByStatus(sprintId);

        long total = points.values().stream().mapToLong(Long::longValue).sum();
        long done  = points.entrySet().stream()
                .filter(e -> e.getKey().isTerminal())
                .mapToLong(Map.Entry::getValue)
                .sum();

        return ResponseEntity.ok(Map.of(
                "totalPoints",     total,
                "donePoints",      done,
                "remainingPoints", total - done,
                "breakdown",       points.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().getDisplayName(),
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new))
        ));
    }
}
