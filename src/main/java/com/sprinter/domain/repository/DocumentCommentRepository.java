package com.sprinter.domain.repository;

import com.sprinter.domain.entity.DocumentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {
}
