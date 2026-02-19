package com.sprinter.domain.entity;

import com.sprinter.domain.enums.ProjectStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 * Entita projektu nebo podprojektu.
 *
 * <p>Projekty mohou být hierarchicky vnořené – každý projekt může mít nadřazený projekt
 * ({@link #parent}) a libovolný počet podprojektů ({@link #children}).
 * Projektový tým (členové) se dědí na podprojekty: pokud uživatel není explicitně
 * přiřazen k podprojektu, zdědí přístup z nadřazeného projektu.</p>
 *
 * <p>Každý projekt má unikátní klíč (např. "PROJ"), který se používá pro
 * identifikaci pracovních položek (PROJ-1, PROJ-2...).</p>
 */
@Entity
@Table(name = "projects",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_projects_key", columnNames = "project_key")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"parent", "children", "members", "workItems", "sprints"})
@EqualsAndHashCode(of = "id")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projects_seq")
    @SequenceGenerator(name = "projects_seq", sequenceName = "projects_id_seq", allocationSize = 1)
    private Long id;

    /** Název projektu. */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Unikátní klíč projektu (velká písmena, max 10 znaků).
     * Používá se jako prefix čísla pracovní položky: PROJ-1, PROJ-2...
     */
    @NotBlank
    @Pattern(regexp = "[A-Z][A-Z0-9]{1,9}", message = "Klíč musí začínat písmenem a obsahovat jen velká písmena a číslice (2-10 znaků)")
    @Column(name = "project_key", nullable = false, length = 10)
    private String projectKey;

    /** Popis projektu (může být formátovaný HTML). */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Stav projektu. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    /** Plánované datum zahájení. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Plánované datum ukončení. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Nadřazený projekt (null = kořenový projekt). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Project parent;

    /** Podprojekty. */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    @Builder.Default
    private List<Project> children = new ArrayList<>();

    /** Vlastník projektu (vedoucí). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /** Členové projektového týmu s jejich rolemi. */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProjectMember> members = new HashSet<>();

    /** Pracovní položky v tomto projektu. */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkItem> workItems = new ArrayList<>();

    /** Sprinty v tomto projektu. */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate ASC")
    @Builder.Default
    private List<Sprint> sprints = new ArrayList<>();

    /**
     * Sekvenční čítač pro generování čísla pracovní položky.
     * PROJ-1, PROJ-2 atd. Inkrementuje se při každém vytvoření nové položky.
     */
    @Column(name = "item_counter", nullable = false)
    @Builder.Default
    private Long itemCounter = 0L;

    /** URL ikony / avatar projektu (emoji nebo path k obrázku). */
    @Column(name = "icon_url")
    private String iconUrl;

    /** Barva projektu (hex kód nebo název CSS barvy). */
    @Column(name = "color", length = 20)
    private String color;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Pomocné metody ----

    /** Vrátí true, pokud je projekt kořenový (nemá rodiče). */
    public boolean isRoot() {
        return parent == null;
    }

    /** Vrátí true, pokud je projekt podprojektem. */
    public boolean isSubproject() {
        return parent != null;
    }

    /**
     * Atomicky inkrementuje čítač položek a vrátí nové číslo.
     * Volá se při vytvoření nové pracovní položky.
     */
    public Long nextItemNumber() {
        itemCounter++;
        return itemCounter;
    }

    /** Vrátí hloubku vnořenosti (0 = kořenový projekt). */
    public int getDepth() {
        int depth = 0;
        Project p = this.parent;
        while (p != null) {
            depth++;
            p = p.getParent();
        }
        return depth;
    }
}
