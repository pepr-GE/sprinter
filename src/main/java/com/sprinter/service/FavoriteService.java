package com.sprinter.service;

import com.sprinter.domain.entity.UserFavorite;
import com.sprinter.domain.repository.UserFavoriteRepository;
import com.sprinter.domain.repository.UserRepository;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final UserFavoriteRepository favoriteRepository;
    private final UserRepository         userRepository;

    @Transactional(readOnly = true)
    public List<UserFavorite> findForCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .map(favoriteRepository::findByUserIdOrderByCreatedAtDesc)
                .orElse(List.of());
    }

    public void addFavorite(String entityType, Long entityId, String title, String url, String icon) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow();
        if (favoriteRepository.existsByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId)) {
            return; // Already favorited
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel nenalezen"));
        var fav = UserFavorite.builder()
                .user(user)
                .entityType(entityType)
                .entityId(entityId)
                .title(title)
                .url(url)
                .icon(icon)
                .build();
        favoriteRepository.save(fav);
    }

    public void removeFavorite(String entityType, Long entityId) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow();
        favoriteRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);
    }

    public void removeFavoriteById(Long favoriteId) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow();
        var fav = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new ResourceNotFoundException("Oblíbená položka nenalezena"));
        if (!fav.getUser().getId().equals(userId)) {
            throw new com.sprinter.exception.AccessDeniedException("Nemůžete smazat tuto oblíbenou položku.");
        }
        favoriteRepository.delete(fav);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String entityType, Long entityId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> favoriteRepository.existsByUserIdAndEntityTypeAndEntityId(
                        userId, entityType, entityId))
                .orElse(false);
    }
}
