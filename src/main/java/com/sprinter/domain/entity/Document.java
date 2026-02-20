package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dokument – samostatný objekt pro delší texty (dokumentace, manuály, blog).
 * Podporuje WYSIWYG editaci, komentáře a vazby na pracovní položky.
 */
@Entity
@Table(name = "documents",
       indexes = {
           @Index(name = "idx_documents_project", columnList = "project_id"),
           @Index(name = "idx_documents_author",  columnList = "author_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"project", "author", "comments", "linkedWorkItems"})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documents_seq")
    @SequenceGenerator(name = "documents_seq", sequenceName = "documents_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String title;

    /** HTML obsah z WYSIWYG editoru. */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Projekt, ke kterému dokument patří (může být null = globální dokument). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Složka, do které dokument patří (může být null = kořenová úroveň). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private DocumentFolder folder;

    /** Autor dokumentu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<DocumentComment> comments = new ArrayList<>();

    /** Pracovní položky propojené s tímto dokumentem. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "document_work_item_links",
        joinColumns        = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "work_item_id")
    )
    @Builder.Default
    private Set<WorkItem> linkedWorkItems = new HashSet<>();
}
