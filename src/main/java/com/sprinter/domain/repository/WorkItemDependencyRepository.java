package com.sprinter.domain.repository;

import com.sprinter.domain.entity.WorkItemDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pro entitu {@link WorkItemDependency}.
 */
@Repository
public interface WorkItemDependencyRepository extends JpaRepository<WorkItemDependency, Long> {

    /** Vrátí závislosti, kde daná položka je předchůdcem. */
    List<WorkItemDependency> findByPredecessorId(Long predecessorId);

    /** Vrátí závislosti, kde daná položka je nástupníkem. */
    List<WorkItemDependency> findBySuccessorId(Long successorId);

    /** Vrátí true, pokud závislost již existuje (v libovolném směru). */
    @Query("""
           SELECT COUNT(d) > 0 FROM WorkItemDependency d
           WHERE (d.predecessor.id = :a AND d.successor.id = :b)
              OR (d.predecessor.id = :b AND d.successor.id = :a)
           """)
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    /** Vrátí všechny závislosti pro položky daného projektu (pro Gantt). */
    @Query("""
           SELECT d FROM WorkItemDependency d
           JOIN d.predecessor p
           WHERE p.project.id = :projectId
           """)
    List<WorkItemDependency> findByProjectId(@Param("projectId") Long projectId);
}
