package com.sprinter.domain.entity;

import com.sprinter.domain.enums.DependencyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Závislost mezi dvěma pracovními položkami.
 *
 * <p>Modeluje vztah "predecessor → successor" s typem závislosti.
 * Používá se pro Ganttův diagram a analýzu kritické cesty.</p>
 */
@Entity
@Table(name = "work_item_dependencies",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_work_item_dep", columnNames = {"predecessor_id", "successor_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class WorkItemDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_item_deps_seq")
    @SequenceGenerator(name = "work_item_deps_seq", sequenceName = "work_item_deps_id_seq", allocationSize = 1)
    private Long id;

    /** Předchůdce (na které položce závisíme). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predecessor_id", nullable = false)
    private WorkItem predecessor;

    /** Nástupník (která položka závisí na předchůdci). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "successor_id", nullable = false)
    private WorkItem successor;

    /** Typ závislosti. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    @Builder.Default
    private DependencyType dependencyType = DependencyType.FINISH_TO_START;

    /** Volitelný lag (posun v dnech – kladný = zpoždění, záporný = překryv). */
    @Column(name = "lag_days")
    @Builder.Default
    private Integer lagDays = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
