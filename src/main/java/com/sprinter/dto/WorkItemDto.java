package com.sprinter.dto;

import com.sprinter.domain.enums.Priority;
import com.sprinter.domain.enums.WorkItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO pro vytvoření nebo aktualizaci pracovní položky.
 * Mapuje data z HTML formuláře nebo REST requestu.
 */
@Data
public class WorkItemDto {

    @NotBlank(message = "Název položky nesmí být prázdný")
    @Size(max = 500, message = "Název může mít maximálně 500 znaků")
    private String title;

    private String description;

    @NotNull(message = "Typ položky musí být vybrán")
    private WorkItemType type;

    private Priority priority = Priority.MEDIUM;

    private Long assigneeId;

    private Long parentId;

    private Long sprintId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    private Integer storyPoints;

    private Double estimatedHours;

    private Integer progressPct = 0;

    private Set<Long> labelIds = new HashSet<>();
}
