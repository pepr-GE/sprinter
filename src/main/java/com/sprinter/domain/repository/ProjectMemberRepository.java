package com.sprinter.domain.repository;

import com.sprinter.domain.entity.ProjectMember;
import com.sprinter.domain.enums.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pro entitu {@link ProjectMember}.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** Vyhledá členství uživatele v projektu. */
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    /** Vrátí všechna členství v daném projektu. */
    List<ProjectMember> findByProjectIdOrderByUserLastNameAsc(Long projectId);

    /** Vrátí všechna členství daného uživatele. */
    List<ProjectMember> findByUserId(Long userId);

    /** Vrátí true, pokud uživatel je členem projektu. */
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * Vrátí roli uživatele v daném projektu.
     * Vrací Optional.empty() pokud uživatel není členem.
     */
    @Query("""
           SELECT pm.projectRole FROM ProjectMember pm
           WHERE pm.project.id = :projectId AND pm.user.id = :userId
           """)
    Optional<ProjectRole> findRoleByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId")    Long userId);

    /** Vrátí počet členů v projektu. */
    long countByProjectId(Long projectId);

    /** Odstraní členství uživatele z projektu. */
    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
