package com.sprinter.domain.entity;

import com.sprinter.domain.enums.SprintStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint – časově ohraničená iterace v rámci projektu.
 *
 * <p>Sprint sdružuje pracovní položky ({@link WorkItem}), které mají být
 * dokončeny v daném časovém úseku. V projektu může probíhat vždy jen jeden
 * aktivní sprint ({@link SprintStatus#ACTIVE}).</p>
 */
@Entity
@Table(name = "sprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"project", "workItems"})
@EqualsAndHashCode(of = "id")
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sprints_seq")
    @SequenceGenerator(name = "sprints_seq", sequenceName = "sprints_id_seq", allocationSize = 1)
    private Long id;

    /** Název sprintu (např. "Sprint 1", "Sprint Q1/2025"). */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    /** Cíl sprintu – stručný popis co má sprint přinést. */
    @Column(columnDefinition = "TEXT")
    private String goal;

    /** Stav sprintu. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNING;

    /** Datum zahájení sprintu. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Datum ukončení sprintu. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Datum faktického dokončení (kdy byl sprint uzavřen). */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Projekt, ke kterému sprint patří. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Pracovní položky přiřazené k tomuto sprintu. */
    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkItem> workItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Pomocné metody ----

    /** Vrátí true, pokud je sprint aktivní. */
    public boolean isActive() {
        return SprintStatus.ACTIVE == status;
    }

    /** Vrátí true, pokud je sprint dokončen nebo zrušen. */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
