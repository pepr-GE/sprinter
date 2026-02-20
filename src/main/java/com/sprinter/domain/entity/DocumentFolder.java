package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Složka pro organizaci dokumentů – může být víceúrovňová (parent → děti).
 */
@Entity
@Table(name = "document_folders",
       indexes = {
           @Index(name = "idx_doc_folders_project", columnList = "project_id"),
           @Index(name = "idx_doc_folders_parent",  columnList = "parent_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"project", "parent", "children"})
public class DocumentFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_folders_seq")
    @SequenceGenerator(name = "document_folders_seq", sequenceName = "document_folders_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String name;

    /** Projekt, ke kterému složka patří (null = globální složka). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Nadřazená složka (null = kořenová složka). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DocumentFolder parent;

    /** Podsložky. */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("name ASC")
    @Builder.Default
    private List<DocumentFolder> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
