package com.sprinter.dto;

import com.sprinter.domain.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO pro vytvoření nebo aktualizaci projektu/podprojektu.
 */
@Data
public class ProjectDto {

    @NotBlank(message = "Název projektu nesmí být prázdný")
    @Size(max = 200, message = "Název může mít maximálně 200 znaků")
    private String name;

    @NotBlank(message = "Klíč projektu nesmí být prázdný")
    @Pattern(regexp = "[A-Z][A-Z0-9]{1,9}",
             message = "Klíč musí začínat písmenem a obsahovat jen velká písmena a číslice (2-10 znaků)")
    private String projectKey;

    private String description;

    private ProjectStatus status = ProjectStatus.ACTIVE;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Long ownerId;
}
