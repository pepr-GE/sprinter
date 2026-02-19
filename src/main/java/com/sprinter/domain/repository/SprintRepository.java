package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Sprint;
import com.sprinter.domain.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pro entitu {@link Sprint}.
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    /** Vrátí všechny sprinty daného projektu seřazené dle data zahájení. */
    List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);

    /** Vrátí aktivní sprint projektu (měl by být maximálně jeden). */
    Optional<Sprint> findByProjectIdAndStatus(Long projectId, SprintStatus status);

    /** Vrátí sprinty projektu s daným stavem. */
    List<Sprint> findByProjectIdAndStatusOrderByStartDateDesc(Long projectId, SprintStatus status);

    /** Vrátí true, pokud projekt má aktivní sprint. */
    @Query("SELECT COUNT(s) > 0 FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    boolean hasActiveSprint(@Param("projectId") Long projectId);

    /**
     * Vrátí sprint s počtem přiřazených a dokončených položek pro výpočet burn-down.
     */
    @Query("""
           SELECT s,
                  COUNT(wi)                                           AS totalItems,
                  SUM(CASE WHEN wi.status IN ('DONE','CANCELLED') THEN 1 ELSE 0 END) AS doneItems,
                  COALESCE(SUM(wi.storyPoints), 0)                   AS totalPoints,
                  COALESCE(SUM(CASE WHEN wi.status IN ('DONE','CANCELLED') THEN wi.storyPoints ELSE 0 END), 0) AS donePoints
           FROM Sprint s
           LEFT JOIN s.workItems wi
           WHERE s.id = :sprintId
           GROUP BY s
           """)
    Object[] findSprintWithStats(@Param("sprintId") Long sprintId);
}
