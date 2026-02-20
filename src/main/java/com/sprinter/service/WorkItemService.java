package com.sprinter.service;

import com.sprinter.domain.entity.*;
import com.sprinter.domain.enums.*;
import com.sprinter.domain.repository.*;
import com.sprinter.exception.AccessDeniedException;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.exception.ValidationException;
import com.sprinter.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Servisní třída pro správu pracovních položek (WorkItem).
 *
 * <p>Pokrývá úkoly, problémy, stories, epicy a články.
 * Implementuje business logiku pro workflow (změna stavu), přiřazení,
 * komentáře, přílohy a závislosti.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkItemService {

    private final WorkItemRepository           workItemRepository;
    private final ProjectRepository            projectRepository;
    private final CommentRepository            commentRepository;
    private final LabelRepository              labelRepository;
    private final WorkItemDependencyRepository dependencyRepository;
    private final SprintRepository             sprintRepository;
    private final ProjectService               projectService;
    private final UserService                  userService;

    // ---- Čtení ----

    @Transactional(readOnly = true)
    public WorkItem findById(Long id) {
        return workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pracovní položka", id));
    }

    @Transactional(readOnly = true)
    public WorkItem findByProjectKeyAndNumber(String projectKey, Long itemNumber) {
        return workItemRepository.findByProjectKeyAndItemNumber(projectKey, itemNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Položka " + projectKey + "-" + itemNumber + " nebyla nalezena."));
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findBacklog(Long projectId) {
        projectService.requireAccess(projectId);
        return workItemRepository.findBacklogItems(projectId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findBySprint(Long sprintId) {
        return workItemRepository.findBySprintId(sprintId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findGanttItems(Long projectId) {
        projectService.requireAccess(projectId);
        return workItemRepository.findGanttItems(projectId);
    }

    @Transactional(readOnly = true)
    public Page<WorkItem> findWithFilters(Long projectId, WorkItemType type,
                                          WorkItemStatus status, String search, Pageable pageable) {
        projectService.requireAccess(projectId);
        return workItemRepository.findWithFilters(projectId, type, status, search, pageable);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findAssignedToCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException());
        return workItemRepository.findAssignedToUser(userId);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> findArticles(Long projectId) {
        projectService.requireAccess(projectId);
        return workItemRepository.findByProjectIdAndTypeOrderByUpdatedAtDesc(projectId, WorkItemType.ARTICLE);
    }

    // ---- Vytváření ----

    /**
     * Vytvoří novou pracovní položku.
     *
     * @param projectId   ID projektu
     * @param type        typ položky
     * @param title       název
     * @param description popis (Markdown)
     * @param priority    priorita
     * @param assigneeId  ID přiřazeného uživatele (může být null)
     * @param parentId    ID nadřazené položky (může být null)
     * @param startDate   datum zahájení
     * @param dueDate     termín dokončení
     * @param storyPoints story pointy
     * @param labelIds    ID štítků
     * @return vytvořená položka
     */
    public WorkItem createWorkItem(Long projectId, WorkItemType type, String title,
                                   String description, Priority priority, Long assigneeId,
                                   Long parentId, LocalDate startDate, LocalDate dueDate,
                                   Integer storyPoints, Long sprintId, Integer progressPct,
                                   Set<Long> labelIds) {
        projectService.requireContentEditAccess(projectId);

        var project  = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projekt", projectId));
        var reporter = getCurrentUser();

        // Atomický increment čísla položky
        Long itemNumber = project.nextItemNumber();
        projectRepository.save(project);

        User assignee = assigneeId != null ? userService.findById(assigneeId) : null;
        WorkItem parent = parentId != null ? findById(parentId) : null;
        Sprint sprint = sprintId != null
                ? sprintRepository.findById(sprintId).orElse(null)
                : null;

        var workItem = WorkItem.builder()
                .project(project)
                .itemNumber(itemNumber)
                .type(type)
                .title(title.trim())
                .description(description)
                .status(WorkItemStatus.TO_DO)
                .priority(priority != null ? priority : Priority.MEDIUM)
                .assignee(assignee)
                .reporter(reporter)
                .parent(parent)
                .sprint(sprint)
                .startDate(startDate)
                .dueDate(dueDate)
                .storyPoints(storyPoints)
                .progressPct(progressPct != null ? progressPct : 0)
                .build();

        // Přiřazení štítků
        if (labelIds != null && !labelIds.isEmpty()) {
            var labels = labelRepository.findAllById(labelIds);
            workItem.getLabels().addAll(labels);
        }

        workItem = workItemRepository.save(workItem);
        log.info("Vytvořena položka {} ({})", workItem.getItemKey(), workItem.getType().getDisplayName());
        return workItem;
    }

    /**
     * Aktualizuje pracovní položku.
     */
    public WorkItem updateWorkItem(Long id, String title, String description, Priority priority,
                                   Long assigneeId, LocalDate startDate, LocalDate dueDate,
                                   Integer storyPoints, Double estimatedHours,
                                   Long sprintId, Integer progressPct, Set<Long> labelIds) {
        var workItem = findById(id);
        projectService.requireContentEditAccess(workItem.getProject().getId());

        workItem.setTitle(title.trim());
        workItem.setDescription(description);
        workItem.setPriority(priority != null ? priority : workItem.getPriority());
        workItem.setAssignee(assigneeId != null ? userService.findById(assigneeId) : null);
        workItem.setSprint(sprintId != null
                ? sprintRepository.findById(sprintId).orElse(null)
                : null);
        workItem.setStartDate(startDate);
        workItem.setDueDate(dueDate);
        workItem.setStoryPoints(storyPoints);
        workItem.setEstimatedHours(estimatedHours);
        workItem.setProgressPct(progressPct != null ? progressPct : 0);

        // Aktualizace štítků
        workItem.getLabels().clear();
        if (labelIds != null && !labelIds.isEmpty()) {
            workItem.getLabels().addAll(labelRepository.findAllById(labelIds));
        }

        return workItemRepository.save(workItem);
    }

    /**
     * Přiřadí položku do sprintu (nebo přesune do backlogu při sprintId == null).
     */
    public WorkItem changeSprint(Long id, Long sprintId) {
        var workItem = findById(id);
        projectService.requireContentEditAccess(workItem.getProject().getId());
        workItem.setSprint(sprintId != null
                ? sprintRepository.findById(sprintId).orElse(null)
                : null);
        return workItemRepository.save(workItem);
    }

    /**
     * Změní stav pracovní položky (workflow transition).
     */
    public WorkItem changeStatus(Long id, WorkItemStatus newStatus) {
        var workItem = findById(id);

        // Pozorovatel nemůže měnit stav
        Long projectId = workItem.getProject().getId();
        projectService.requireContentEditAccess(projectId);

        WorkItemStatus oldStatus = workItem.getStatus();
        workItem.setStatus(newStatus);

        // Automaticky nastavíme datum dokončení
        if (newStatus == WorkItemStatus.DONE && workItem.getCompletedAt() == null) {
            workItem.setCompletedAt(LocalDateTime.now());
        } else if (newStatus != WorkItemStatus.DONE && newStatus != WorkItemStatus.CANCELLED) {
            workItem.setCompletedAt(null);
        }

        workItem = workItemRepository.save(workItem);
        log.debug("Položka {} změnila stav {} → {}", workItem.getItemKey(), oldStatus, newStatus);
        return workItem;
    }

    /**
     * Nastaví procento dokončení položky.
     */
    public WorkItem changeProgress(Long id, Integer progressPct) {
        var workItem = findById(id);
        projectService.requireContentEditAccess(workItem.getProject().getId());
        int pct = progressPct != null ? Math.max(0, Math.min(100, progressPct)) : 0;
        workItem.setProgressPct(pct);
        return workItemRepository.save(workItem);
    }

    /**
     * Zaznamená odpracované hodiny.
     */
    public WorkItem logHours(Long id, Double hours) {
        var workItem = findById(id);
        projectService.requireContentEditAccess(workItem.getProject().getId());

        double current = workItem.getLoggedHours() != null ? workItem.getLoggedHours() : 0.0;
        workItem.setLoggedHours(current + hours);
        return workItemRepository.save(workItem);
    }

    /**
     * Smaže pracovní položku.
     */
    public void deleteWorkItem(Long id) {
        var workItem = findById(id);
        projectService.requireManageAccess(workItem.getProject().getId());
        workItemRepository.delete(workItem);
        log.info("Smazána položka ID={}", id);
    }

    // ---- Komentáře ----

    /**
     * Přidá komentář k pracovní položce.
     * Mohou přidávat i pozorovatelé.
     */
    public Comment addComment(Long workItemId, String content) {
        var workItem = findById(workItemId);
        // Pozorovatelé MOHOU komentovat – proto requireAccess, ne requireContentEdit
        projectService.requireAccess(workItem.getProject().getId());

        var comment = Comment.builder()
                .workItem(workItem)
                .author(getCurrentUser())
                .content(content.trim())
                .build();

        return commentRepository.save(comment);
    }

    /**
     * Upraví komentář (pouze autor nebo admin).
     */
    public Comment updateComment(Long commentId, String content) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Komentář", commentId));

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (!SecurityUtils.isCurrentUserAdmin() && !comment.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Komentář může upravit pouze jeho autor.");
        }

        comment.setContent(content.trim());
        comment.setEdited(true);
        return commentRepository.save(comment);
    }

    /**
     * Smaže komentář (pouze autor nebo admin).
     */
    public void deleteComment(Long commentId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Komentář", commentId));

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (!SecurityUtils.isCurrentUserAdmin() && !comment.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Komentář může smazat pouze jeho autor.");
        }

        commentRepository.delete(comment);
    }

    // ---- Závislosti ----

    /**
     * Přidá závislost mezi dvěma pracovními položkami.
     */
    public WorkItemDependency addDependency(Long predecessorId, Long successorId, DependencyType type) {
        if (predecessorId.equals(successorId)) {
            throw new ValidationException("Položka nemůže záviset sama na sobě.");
        }
        if (dependencyRepository.existsBetween(predecessorId, successorId)) {
            throw new ValidationException("Závislost mezi těmito položkami již existuje.");
        }

        var predecessor = findById(predecessorId);
        var successor   = findById(successorId);

        projectService.requireContentEditAccess(successor.getProject().getId());

        var dep = WorkItemDependency.builder()
                .predecessor(predecessor)
                .successor(successor)
                .dependencyType(type)
                .build();

        return dependencyRepository.save(dep);
    }

    /**
     * Odebere závislost.
     */
    public void removeDependency(Long dependencyId) {
        var dep = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Závislost", dependencyId));
        projectService.requireContentEditAccess(dep.getSuccessor().getProject().getId());
        dependencyRepository.delete(dep);
    }

    // ---- Pomocné metody ----

    private User getCurrentUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Není přihlášen žádný uživatel."));
    }
}
