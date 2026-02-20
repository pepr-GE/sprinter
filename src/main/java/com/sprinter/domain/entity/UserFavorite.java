package com.sprinter.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Oblíbená položka uživatele (projekt, dokument, pracovní položka, složka).
 */
@Entity
@Table(name = "user_favorites",
       indexes = @Index(name = "idx_user_favorites_user", columnList = "user_id"),
       uniqueConstraints = @UniqueConstraint(
               name = "uq_user_favorite",
               columnNames = {"user_id", "entity_type", "entity_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class UserFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_favorites_seq")
    @SequenceGenerator(name = "user_favorites_seq", sequenceName = "user_favorites_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;   // 'project', 'document', 'work_item', 'folder'

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 100)
    private String icon;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
