package com.sprinter.service;

import com.sprinter.domain.entity.Sprint;
import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.enums.SprintStatus;
import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.domain.repository.SprintRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servisní třída pro správu sprintů.
 *
 * <p>Implementuje business logiku pro:
 * <ul>
 *   <li>Vytváření, spouštění a uzavírání sprintů</li>
 *   <li>Přiřazování pracovních položek ke sprintům</li>
 *   <li>Přesun nevyřešených položek do backlogu nebo nového sprintu</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SprintService {

    private final SprintRepository   sprintRepository;
    private final WorkItemRepository workItemRepository;
    private final ProjectService     projectService;

    // ---- Čtení ----

    @Transactional(readOnly = true)
    public Sprint findById(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint", id));
    }

    @Transactional(readOnly = true)
    public List<Sprint> findByProject(Long projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<Sprint> findActiveSprint(Long projectId) {
        return sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
    }

    // ---- Vytváření ----

    /**
     * Vytvoří nový sprint v projektu ve stavu PLANNING.
     */
    public Sprint createSprint(Long projectId, String name, String goal,
                                LocalDate startDate, LocalDate endDate) {
        projectService.requireManageAccess(projectId);

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("Datum ukončení sprintu nemůže být před datem zahájení.");
        }

        var project = projectService.findById(projectId);

        var sprint = Sprint.builder()
                .project(project)
                .name(name.trim())
                .goal(goal)
                .startDate(startDate)
                .endDate(endDate)
                .status(SprintStatus.PLANNING)
                .build();

        sprint = sprintRepository.save(sprint);
        log.info("Vytvořen sprint '{}' v projektu {}", sprint.getName(), project.getProjectKey());
        return sprint;
    }

    /**
     * Aktualizuje sprint.
     */
    public Sprint updateSprint(Long id, String name, String goal,
                                LocalDate startDate, LocalDate endDate) {
        var sprint = findById(id);
        projectService.requireManageAccess(sprint.getProject().getId());

        if (sprint.isTerminal()) {
            throw new ValidationException("Dokončený nebo zrušený sprint nelze upravovat.");
        }

        sprint.setName(name.trim());
        sprint.setGoal(goal);
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);

        return sprintRepository.save(sprint);
    }

    /**
     * Spustí sprint (přechod z PLANNING → ACTIVE).
     * V projektu může být aktivní vždy jen jeden sprint.
     */
    public Sprint startSprint(Long id) {
        var sprint = findById(id);
        Long projectId = sprint.getProject().getId();
        projectService.requireManageAccess(projectId);

        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new ValidationException("Spustit lze pouze sprint ve stavu 'Plánování'.");
        }
        if (sprintRepository.hasActiveSprint(projectId)) {
            throw new ValidationException("V projektu již probíhá jiný aktivní sprint.");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        sprint = sprintRepository.save(sprint);
        log.info("Spuštěn sprint '{}' v projektu {}", sprint.getName(), sprint.getProject().getProjectKey());
        return sprint;
    }

    /**
     * Uzavře sprint (přechod z ACTIVE → COMPLETED).
     *
     * <p>Nevyřešené položky lze přesunout do backlogu nebo do jiného sprintu.
     * Dokončené položky zůstanou uzavřeny.</p>
     *
     * @param id                  ID uzavíraného sprintu
     * @param moveIncompleteToId  ID sprintu, do kterého přesunout nedokončené položky
     *                             (null = přesun do backlogu)
     */
    public Sprint completeSprint(Long id, Long moveIncompleteToId) {
        var sprint = findById(id);
        projectService.requireManageAccess(sprint.getProject().getId());

        if (!sprint.isActive()) {
            throw new ValidationException("Ukončit lze pouze aktivní sprint.");
        }

        // Přesun nedokončených položek
        List<WorkItem> incomplete = sprint.getWorkItems().stream()
                .filter(wi -> !wi.getStatus().isTerminal())
                .toList();

        Sprint targetSprint = null;
        if (moveIncompleteToId != null) {
            targetSprint = findById(moveIncompleteToId);
        }

        for (WorkItem wi : incomplete) {
            wi.setSprint(targetSprint);
            workItemRepository.save(wi);
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(LocalDateTime.now());

        sprint = sprintRepository.save(sprint);
        log.info("Uzavřen sprint '{}', přesunuto {} nedokončených položek",
                  sprint.getName(), incomplete.size());
        return sprint;
    }

    /**
     * Zruší sprint.
     */
    public void cancelSprint(Long id) {
        var sprint = findById(id);
        projectService.requireManageAccess(sprint.getProject().getId());

        if (sprint.isTerminal()) {
            throw new ValidationException("Sprint je již uzavřen.");
        }

        // Všechny položky přesuneme do backlogu
        sprint.getWorkItems().forEach(wi -> {
            wi.setSprint(null);
            workItemRepository.save(wi);
        });

        sprint.setStatus(SprintStatus.CANCELLED);
        sprintRepository.save(sprint);
        log.info("Zrušen sprint '{}'", sprint.getName());
    }

    /**
     * Přidá pracovní položku do sprintu.
     */
    public void addWorkItemToSprint(Long sprintId, Long workItemId) {
        var sprint   = findById(sprintId);
        var workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", workItemId));

        if (sprint.isTerminal()) {
            throw new ValidationException("Do uzavřeného sprintu nelze přidávat položky.");
        }
        if (!workItem.getType().isSprintable()) {
            throw new ValidationException("Typ položky '" + workItem.getType().getDisplayName()
                    + "' nelze přiřadit ke sprintu.");
        }

        workItem.setSprint(sprint);
        workItemRepository.save(workItem);
    }

    /**
     * Odebere pracovní položku ze sprintu (přesune do backlogu).
     */
    public void removeWorkItemFromSprint(Long workItemId) {
        var workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem", workItemId));
        workItem.setSprint(null);
        workItemRepository.save(workItem);
    }
}
