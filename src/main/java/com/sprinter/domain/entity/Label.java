package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Štítek (label/tag) pro kategorizaci pracovních položek.
 * Štítky jsou globální v rámci celého systému (nebo projektu).
 */
@Entity
@Table(name = "labels",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_labels_project_name", columnNames = {"project_id", "name"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "workItems")
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "labels_seq")
    @SequenceGenerator(name = "labels_seq", sequenceName = "labels_id_seq", allocationSize = 1)
    private Long id;

    /** Název štítku. */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Barva štítku (hex kód, např. "#FF5630").
     * Výchozí barva je holubí modrá dle designu aplikace.
     */
    @Column(length = 10)
    @Builder.Default
    private String color = "#6DA3C7";

    /** Projekt, ke kterému štítek patří (null = globální štítek). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Pracovní položky s tímto štítkem. */
    @ManyToMany(mappedBy = "labels")
    @Builder.Default
    private Set<WorkItem> workItems = new HashSet<>();
}
