package com.sprinter.service;

import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.domain.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Servisní třída pro generování reportů a statistik.
 *
 * <p>Poskytuje data pro:
 * <ul>
 *   <li>Přehled stavu projektu (počty položek dle stavu)</li>
 *   <li>Burn-down chart sprintu</li>
 *   <li>Velocity chart (story points za sprinty)</li>
 *   <li>Statistiky přiřazení (kdo řeší co)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final WorkItemRepository workItemRepository;
    private final ProjectService     projectService;
    private final SprintService      sprintService;

    /**
     * Vrátí počty pracovních položek dle stavu pro daný projekt.
     *
     * @param projectId ID projektu
     * @return mapa stav → počet
     */
    public Map<WorkItemStatus, Long> getProjectStatusCounts(Long projectId) {
        projectService.requireAccess(projectId);

        var rows = workItemRepository.countByStatus(projectId);
        Map<WorkItemStatus, Long> result = new HashMap<>();

        // Inicializace všemi stavy na 0
        for (WorkItemStatus s : WorkItemStatus.values()) {
            result.put(s, 0L);
        }

        for (Object[] row : rows) {
            WorkItemStatus status = (WorkItemStatus) row[0];
            Long           count  = (Long) row[1];
            result.put(status, count);
        }

        return result;
    }

    /**
     * Vrátí souhrn story pointů dle stavu pro sprint (základ burn-down chartu).
     *
     * @param sprintId ID sprintu
     * @return mapa stav → story points
     */
    public Map<WorkItemStatus, Long> getSprintPointsByStatus(Long sprintId) {
        var rows = workItemRepository.sumStoryPointsByStatusInSprint(sprintId);
        Map<WorkItemStatus, Long> result = new HashMap<>();

        for (WorkItemStatus s : WorkItemStatus.values()) {
            result.put(s, 0L);
        }
        for (Object[] row : rows) {
            WorkItemStatus status = (WorkItemStatus) row[0];
            Long           points = ((Number) row[1]).longValue();
            result.put(status, points);
        }

        return result;
    }

    /**
     * Vrátí procento dokončenosti projektu na základě počtu dokončených položek.
     */
    public int getProjectCompletionPercent(Long projectId) {
        var counts = getProjectStatusCounts(projectId);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return 0;

        long done = counts.entrySet().stream()
                .filter(e -> e.getKey().isTerminal())
                .mapToLong(Map.Entry::getValue)
                .sum();

        return (int) (done * 100L / total);
    }
}
