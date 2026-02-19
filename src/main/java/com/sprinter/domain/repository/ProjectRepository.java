package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Project;
import com.sprinter.domain.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pro entitu {@link Project}.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Vyhledá projekt dle klíče (case-insensitive). */
    Optional<Project> findByProjectKeyIgnoreCase(String projectKey);

    /** Vrátí true, pokud klíč projektu již existuje. */
    boolean existsByProjectKeyIgnoreCase(String projectKey);

    /** Vrátí true, pokud klíč projektu existuje u jiného projektu. */
    boolean existsByProjectKeyIgnoreCaseAndIdNot(String projectKey, Long id);

    /** Vrátí všechny kořenové projekty (bez rodiče) seřazené dle názvu. */
    @Query("SELECT p FROM Project p WHERE p.parent IS NULL ORDER BY p.name ASC")
    List<Project> findRootProjects();

    /** Vrátí kořenové projekty s daným stavem. */
    List<Project> findByParentIsNullAndStatusOrderByNameAsc(ProjectStatus status);

    /**
     * Vrátí projekty, do kterých má daný uživatel přístup (je členem).
     * Správci (ADMIN) mají přístup ke všem projektům – to se řeší v service vrstvě.
     */
    @Query("""
           SELECT DISTINCT p FROM Project p
           JOIN p.members pm
           WHERE pm.user.id = :userId AND p.status != 'ARCHIVED'
           ORDER BY p.name ASC
           """)
    List<Project> findProjectsForUser(@Param("userId") Long userId);

    /**
     * Vrátí kořenové projekty, do kterých má uživatel přístup.
     */
    @Query("""
           SELECT DISTINCT p FROM Project p
           JOIN p.members pm
           WHERE pm.user.id = :userId AND p.parent IS NULL AND p.status != 'ARCHIVED'
           ORDER BY p.name ASC
           """)
    List<Project> findRootProjectsForUser(@Param("userId") Long userId);

    /** Vrátí podprojekty daného projektu. */
    List<Project> findByParentIdOrderByNameAsc(Long parentId);

    /**
     * Vyhledání projektů dle názvu nebo klíče (pro fulltextové hledání).
     */
    @Query("""
           SELECT p FROM Project p
           WHERE p.status != 'ARCHIVED'
             AND (LOWER(p.name)       LIKE LOWER(CONCAT('%', :term, '%'))
                  OR LOWER(p.projectKey) LIKE LOWER(CONCAT('%', :term, '%')))
           ORDER BY p.name ASC
           """)
    List<Project> searchProjects(@Param("term") String term);
}
