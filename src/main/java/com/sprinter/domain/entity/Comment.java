package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Komentář k pracovní položce.
 *
 * <p>Komentáře mohou přidávat všichni uživatelé s přístupem k projektu
 * (včetně pozorovatelů). Obsah komentáře podporuje Markdown formátování.</p>
 */
@Entity
@Table(name = "comments",
       indexes = {
           @Index(name = "idx_comments_work_item", columnList = "work_item_id"),
           @Index(name = "idx_comments_author",    columnList = "author_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"workItem", "author"})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comments_seq")
    @SequenceGenerator(name = "comments_seq", sequenceName = "comments_id_seq", allocationSize = 1)
    private Long id;

    /** Obsah komentáře (Markdown). */
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Pracovní položka, ke které komentář patří. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private WorkItem workItem;

    /** Autor komentáře. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** True pokud byl komentář editován po prvotním vytvoření. */
    @Column(name = "is_edited")
    @Builder.Default
    private boolean edited = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
