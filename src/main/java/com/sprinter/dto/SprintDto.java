package com.sprinter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO pro vytvoření nebo aktualizaci sprintu.
 */
@Data
public class SprintDto {

    @NotBlank(message = "Název sprintu nesmí být prázdný")
    @Size(max = 200)
    private String name;

    private String goal;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
