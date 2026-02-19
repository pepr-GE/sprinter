package com.sprinter.domain.entity;

import com.sprinter.domain.enums.Priority;
import com.sprinter.domain.enums.WorkItemStatus;
import com.sprinter.domain.enums.WorkItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pracovní položka (Work Item) – jednotná entita pro úkoly, problémy, stories,
 * epicy a články. Typ je rozlišen přes {@link WorkItemType}.
 *
 * <p>Použití jedné tabulky (single-table) zjednodušuje dotazy a filtrování.
 * Každá položka má unikátní číslo v rámci projektu (projectKey + itemNumber,
 * např. PROJ-42).</p>
 *
 * <p>Položky mohou být hierarchicky zanořeny: epic > story > task. Rodičovský
 * vztah je modelován přes {@link #parent} a {@link #children}.</p>
 */
@Entity
@Table(name = "work_items",
       indexes = {
           @Index(name = "idx_work_items_project",   columnList = "project_id"),
           @Index(name = "idx_work_items_sprint",    columnList = "sprint_id"),
           @Index(name = "idx_work_items_assignee",  columnList = "assignee_id"),
           @Index(name = "idx_work_items_status",    columnList = "status"),
           @Index(name = "idx_work_items_type",      columnList = "type"),
           @Index(name = "idx_work_items_parent",    columnList = "parent_id"),
           @Index(name = "idx_work_items_item_num",  columnList = "project_id, item_number")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"project", "sprint", "assignee", "reporter", "parent", "children",
                     "comments", "attachments", "labels", "dependencies", "dependents"})
@EqualsAndHashCode(of = "id")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_items_seq")
    @SequenceGenerator(name = "work_items_seq", sequenceName = "work_items_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Sekvenční číslo v rámci projektu (1, 2, 3...).
     * Společně s klíčem projektu tvoří identifikátor: PROJ-1.
     */
    @Column(name = "item_number", nullable = false)
    private Long itemNumber;

    /** Typ pracovní položky. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkItemType type;

    /** Název / nadpis položky. */
    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Popis položky v Markdown/HTML formátu.
     * Podporuje bohatý textový obsah (pro Articles obzvláště důležité).
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Aktuální stav (workflow). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkItemStatus status = WorkItemStatus.TO_DO;

    /** Priorita. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    /** Odhad pracnosti v story pointech (Fibonacci řada doporučena). */
    @Column(name = "story_points")
    private Integer storyPoints;

    /** Plánované datum zahájení. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Termín dokončení (deadline). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Skutečné datum dokončení (nastaví se automaticky při přechodu do DONE). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Odhadovaný počet hodin. */
    @Column(name = "estimated_hours")
    private Double estimatedHours;

    /** Reálně odpracované hodiny. */
    @Column(name = "logged_hours")
    private Double loggedHours;

    /** Projekt, ke kterému položka patří. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Sprint, ke kterému je položka přiřazena (null = backlog). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    /** Uživatel, kterému je položka přiřazena. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /** Uživatel, který položku vytvořil/nahlásil. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** Nadřazená položka (epic → story → task hierarchie). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private WorkItem parent;

    /** Podřízené položky. */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemNumber ASC")
    @Builder.Default
    private List<WorkItem> children = new ArrayList<>();

    /** Komentáře k položce. */
    @OneToMany(mappedBy = "workItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    /** Přílohy. */
    @OneToMany(mappedBy = "workItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    /** Štítky (labels). */
    @ManyToMany
    @JoinTable(name = "work_item_labels",
               joinColumns        = @JoinColumn(name = "work_item_id"),
               inverseJoinColumns = @JoinColumn(name = "label_id"))
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    /**
     * Závislosti, kde tato položka je NÁSTUPNÍK (successor).
     * Tj. tato položka závisí na jiných.
     */
    @OneToMany(mappedBy = "successor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkItemDependency> dependencies = new ArrayList<>();

    /**
     * Závislosti, kde tato položka je PŘEDCHŮDCE (predecessor).
     * Tj. jiné položky závisí na této.
     */
    @OneToMany(mappedBy = "predecessor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkItemDependency> dependents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Pomocné metody ----

    /**
     * Vrátí kompletní identifikátor pracovní položky (např. "PROJ-42").
     */
    public String getItemKey() {
        return project.getProjectKey() + "-" + itemNumber;
    }

    /** Vrátí true, pokud je položka dokončena. */
    public boolean isDone() {
        return WorkItemStatus.DONE == status;
    }

    /** Vrátí true, pokud je položka ve sprintu (ne v backlogu). */
    public boolean isInSprint() {
        return sprint != null;
    }

    /** Vrátí true, pokud je položka v backlogu (není ve sprintu). */
    public boolean isInBacklog() {
        return sprint == null;
    }

    /** Vrátí procento dokončení dílčích položek (0-100). */
    public int getChildrenCompletionPercent() {
        if (children.isEmpty()) return 0;
        long done = children.stream().filter(c -> c.getStatus().isTerminal()).count();
        return (int) (done * 100L / children.size());
    }
}
