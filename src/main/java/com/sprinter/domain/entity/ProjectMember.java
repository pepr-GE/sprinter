package com.sprinter.domain.entity;

import com.sprinter.domain.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Přiřazení uživatele k projektu s konkrétní rolí.
 *
 * <p>Tato entita reprezentuje N:M vztah mezi {@link User} a {@link Project}
 * s rozšiřujícím atributem – rolí v projektu ({@link ProjectRole}).</p>
 *
 * <p>Projektový tým se dědí hierarchicky: pokud uživatel není explicitně přiřazen
 * k podprojektu, přístup se vyhodnocuje přes rodičovský projekt.</p>
 */
@Entity
@Table(name = "project_members",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_project_members", columnNames = {"project_id", "user_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"project", "user"})
@ToString(exclude = {"project", "user"})
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_members_seq")
    @SequenceGenerator(name = "project_members_seq", sequenceName = "project_members_id_seq", allocationSize = 1)
    private Long id;

    /** Projekt, ke kterému je uživatel přiřazen. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Přiřazený uživatel. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Role uživatele v tomto projektu. */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_role", nullable = false, length = 20)
    @Builder.Default
    private ProjectRole projectRole = ProjectRole.TEAM_MEMBER;

    /** Datum přiřazení uživatele k projektu. */
    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}
