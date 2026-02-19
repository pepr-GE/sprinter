package com.sprinter.domain.repository;

import com.sprinter.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
