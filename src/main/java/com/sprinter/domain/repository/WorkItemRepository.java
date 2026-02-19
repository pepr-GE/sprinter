package com.sprinter.domain.repository;

import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.domain.enums.WorkItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pro entitu {@link WorkItem}.
 */
@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    /** Vyhledá pracovní položku dle klíče projektu a čísla položky. */
    @Query("""
           SELECT wi FROM WorkItem wi
           JOIN wi.project p
           WHERE p.projectKey = :projectKey AND wi.itemNumber = :itemNumber
           """)
    Optional<WorkItem> findByProjectKeyAndItemNumber(
            @Param("projectKey")  String projectKey,
            @Param("itemNumber")  Long   itemNumber);

    /** Vrátí všechny položky projektu seřazené dle čísla. */
    List<WorkItem> findByProjectIdOrderByItemNumberDesc(Long projectId);

    /**
     * Vrátí backlog (položky bez sprintu) daného projektu.
     * Neobsahuje ARTICLE typ, který nemá sprint.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           WHERE wi.project.id = :projectId
             AND wi.sprint IS NULL
             AND wi.type IN ('TASK','ISSUE','STORY','EPIC')
             AND wi.parent IS NULL
           ORDER BY wi.priority DESC, wi.itemNumber ASC
           """)
    List<WorkItem> findBacklogItems(@Param("projectId") Long projectId);

    /**
     * Vrátí položky přiřazené do daného sprintu.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           LEFT JOIN FETCH wi.assignee
           LEFT JOIN FETCH wi.labels
           WHERE wi.sprint.id = :sprintId AND wi.parent IS NULL
           ORDER BY wi.status ASC, wi.priority DESC, wi.itemNumber ASC
           """)
    List<WorkItem> findBySprintId(@Param("sprintId") Long sprintId);

    /**
     * Vrátí položky projektu pro Ganttův diagram – ty, které mají datum zahájení nebo termín.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           WHERE wi.project.id = :projectId
             AND wi.type IN ('TASK','ISSUE','STORY','EPIC')
             AND (wi.startDate IS NOT NULL OR wi.dueDate IS NOT NULL)
           ORDER BY wi.startDate ASC NULLS LAST, wi.dueDate ASC NULLS LAST
           """)
    List<WorkItem> findGanttItems(@Param("projectId") Long projectId);

    /**
     * Stránkovaný seznam položek s filtrováním.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           WHERE wi.project.id = :projectId
             AND (:type   IS NULL OR wi.type   = :type)
             AND (:status IS NULL OR wi.status = :status)
             AND (:search IS NULL OR :search = ''
                  OR LOWER(wi.title) LIKE LOWER(CONCAT('%', :search, '%')))
           """)
    Page<WorkItem> findWithFilters(
            @Param("projectId") Long           projectId,
            @Param("type")      WorkItemType   type,
            @Param("status")    WorkItemStatus status,
            @Param("search")    String         search,
            Pageable            pageable);

    /**
     * Vrátí statistiky (počty dle stavu) pro dashboard projektu.
     */
    @Query("""
           SELECT wi.status AS status, COUNT(wi) AS cnt
           FROM WorkItem wi
           WHERE wi.project.id = :projectId
             AND wi.type IN ('TASK','ISSUE','STORY')
           GROUP BY wi.status
           """)
    List<Object[]> countByStatus(@Param("projectId") Long projectId);

    /**
     * Vrátí počty story pointů dle stavu pro burn-down chart.
     */
    @Query("""
           SELECT wi.status AS status, COALESCE(SUM(wi.storyPoints), 0) AS points
           FROM WorkItem wi
           WHERE wi.sprint.id = :sprintId
           GROUP BY wi.status
           """)
    List<Object[]> sumStoryPointsByStatusInSprint(@Param("sprintId") Long sprintId);

    /**
     * Vrátí položky s překročeným termínem (overdue) pro daného uživatele.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           WHERE wi.assignee.id = :userId
             AND wi.dueDate < :today
             AND wi.status NOT IN ('DONE','CANCELLED')
           ORDER BY wi.dueDate ASC
           """)
    List<WorkItem> findOverdueForUser(
            @Param("userId") Long      userId,
            @Param("today")  LocalDate today);

    /**
     * Vrátí položky přiřazené uživateli ve všech projektech.
     */
    @Query("""
           SELECT wi FROM WorkItem wi
           WHERE wi.assignee.id = :userId
             AND wi.status NOT IN ('DONE','CANCELLED')
           ORDER BY wi.dueDate ASC NULLS LAST, wi.priority DESC
           """)
    List<WorkItem> findAssignedToUser(@Param("userId") Long userId);

    /**
     * Vrátí počet nedokončených položek ve sprintu.
     */
    @Query("""
           SELECT COUNT(wi) FROM WorkItem wi
           WHERE wi.sprint.id = :sprintId
             AND wi.status NOT IN ('DONE','CANCELLED')
           """)
    long countIncompleteInSprint(@Param("sprintId") Long sprintId);

    /**
     * Vrátí články (ARTICLE typ) projektu.
     */
    List<WorkItem> findByProjectIdAndTypeOrderByUpdatedAtDesc(Long projectId, WorkItemType type);
}
