package com.sprinter.domain.repository;

import com.sprinter.domain.entity.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    /** Kořenové složky daného projektu (bez nadřazené složky). */
    List<DocumentFolder> findByProjectIdAndParentIsNullOrderByNameAsc(Long projectId);

    /** Kořenové globální složky (bez projektu a bez nadřazené). */
    List<DocumentFolder> findByProjectIsNullAndParentIsNullOrderByNameAsc();

    /** Všechny složky daného projektu (pro sestavení stromu). */
    List<DocumentFolder> findByProjectIdOrderByNameAsc(Long projectId);

    /** Globální složky (bez projektu). */
    List<DocumentFolder> findByProjectIsNullOrderByNameAsc();

    /** Podsložky dané složky. */
    @Query("SELECT f FROM DocumentFolder f WHERE f.parent.id = :parentId ORDER BY f.name ASC")
    List<DocumentFolder> findChildren(@Param("parentId") Long parentId);
}
