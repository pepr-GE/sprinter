package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** Dokumenty daného projektu seřazené dle data úpravy. */
    List<Document> findByProjectIdOrderByUpdatedAtDescCreatedAtDesc(Long projectId);

    /** Globální dokumenty (bez projektu) seřazené dle data úpravy. */
    List<Document> findByProjectIsNullOrderByUpdatedAtDescCreatedAtDesc();

    /** Všechny dokumenty dostupné uživateli (dle projektů, ke kterým má přístup). */
    @Query("""
           SELECT d FROM Document d
           WHERE d.project IS NULL
              OR d.project.id IN :projectIds
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findAllForProjects(@Param("projectIds") List<Long> projectIds);

    /** Dokumenty navázané na danou pracovní položku. */
    @Query("SELECT d FROM Document d JOIN d.linkedWorkItems wi WHERE wi.id = :workItemId")
    List<Document> findByLinkedWorkItemId(@Param("workItemId") Long workItemId);

    /** Nedávno upravené dokumenty v projektech (pro activity feed). */
    @Query("""
           SELECT d FROM Document d
           WHERE (d.project IS NULL OR d.project.id IN :projectIds)
             AND COALESCE(d.updatedAt, d.createdAt) >= :since
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findRecentlyUpdatedInProjects(
            @Param("projectIds") List<Long>    projectIds,
            @Param("since")      LocalDateTime since,
            Pageable             pageable);

    /** Nedávno upravené dokumenty daného autora (sidebar – nedávné). */
    @Query("""
           SELECT d FROM Document d
           WHERE d.author.id = :authorId
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findRecentByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    /** Dokumenty ve složce (nebo kořenové – bez složky) v daném projektu. */
    @Query("""
           SELECT d FROM Document d
           WHERE d.project.id = :projectId
             AND (:folderId IS NULL AND d.folder IS NULL
                  OR d.folder.id = :folderId)
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findByProjectAndFolder(
            @Param("projectId") Long projectId,
            @Param("folderId")  Long folderId);

    /** Globální dokumenty ve složce (nebo kořenové). */
    @Query("""
           SELECT d FROM Document d
           WHERE d.project IS NULL
             AND (:folderId IS NULL AND d.folder IS NULL
                  OR d.folder.id = :folderId)
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findGlobalByFolder(@Param("folderId") Long folderId);

    /** Dokumenty v dané složce napříč přístupnými projekty (pro filtraci v globálním seznamu). */
    @Query("""
           SELECT d FROM Document d
           WHERE d.folder.id = :folderId
             AND (d.project IS NULL OR d.project.id IN :projectIds)
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> findByFolderAndProjects(
            @Param("folderId")   Long       folderId,
            @Param("projectIds") List<Long> projectIds);

    /** Fulltext hledání v dokumentech. */
    @Query("""
           SELECT d FROM Document d
           WHERE (d.project IS NULL OR d.project.id IN :projectIds)
             AND LOWER(d.title) LIKE LOWER(CONCAT('%', :term, '%'))
           ORDER BY COALESCE(d.updatedAt, d.createdAt) DESC
           """)
    List<Document> searchInProjects(
            @Param("projectIds") List<Long> projectIds,
            @Param("term")       String     term);
}
