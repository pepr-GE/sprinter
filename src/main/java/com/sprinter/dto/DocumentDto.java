package com.sprinter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO pro vytvoření nebo aktualizaci dokumentu.
 */
@Data
public class DocumentDto {

    @NotBlank(message = "Název dokumentu nesmí být prázdný")
    @Size(max = 500, message = "Název může mít maximálně 500 znaků")
    private String title;

    private String content;

    private Long projectId;

    /** IDs pracovních položek propojených s tímto dokumentem. */
    private Set<Long> linkedWorkItemIds = new HashSet<>();
}
