package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pro entitu {@link Comment}.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Vrátí komentáře k pracovní položce seřazené chronologicky. */
    List<Comment> findByWorkItemIdOrderByCreatedAtAsc(Long workItemId);

    /** Vrátí počet komentářů k pracovní položce. */
    long countByWorkItemId(Long workItemId);

    /** Nedávné komentáře v projektech přístupných uživateli (pro activity feed). */
    @Query("""
           SELECT c FROM Comment c
           WHERE c.workItem.project.id IN :projectIds
             AND c.createdAt >= :since
           ORDER BY c.createdAt DESC
           """)
    List<Comment> findRecentInProjects(
            @Param("projectIds") List<Long>    projectIds,
            @Param("since")      LocalDateTime since,
            Pageable             pageable);
}
