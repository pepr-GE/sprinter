package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Komentář k dokumentu.
 */
@Entity
@Table(name = "document_comments",
       indexes = {
           @Index(name = "idx_doc_comments_document", columnList = "document_id"),
           @Index(name = "idx_doc_comments_author",   columnList = "author_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"document", "author"})
public class DocumentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_comments_seq")
    @SequenceGenerator(name = "document_comments_seq", sequenceName = "document_comments_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
