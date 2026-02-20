package com.sprinter.domain.repository;

import com.sprinter.domain.entity.UserFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavorite> findByUserIdAndEntityTypeAndEntityId(
            Long userId, String entityType, Long entityId);

    void deleteByUserIdAndEntityTypeAndEntityId(
            Long userId, String entityType, Long entityId);

    boolean existsByUserIdAndEntityTypeAndEntityId(
            Long userId, String entityType, Long entityId);
}
