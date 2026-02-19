package com.sprinter.api;

import com.sprinter.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller pro projekty – pomocné endpointy pro AJAX.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectApiController {

    private final ProjectService projectService;

    /**
     * Navrhne klíč projektu z názvu (pro automatické vyplnění formuláře).
     */
    @GetMapping("/suggest-key")
    public ResponseEntity<Map<String, String>> suggestKey(@RequestParam String name) {
        return ResponseEntity.ok(Map.of("key", projectService.suggestProjectKey(name)));
    }

    /**
     * Ověří dostupnost klíče projektu.
     */
    @GetMapping("/check-key")
    public ResponseEntity<Map<String, Boolean>> checkKey(
            @RequestParam String key,
            @RequestParam(required = false) Long excludeId) {
        boolean available = excludeId == null
                ? !projectService.findProjectsForCurrentUser().stream()
                        .anyMatch(p -> p.getProjectKey().equalsIgnoreCase(key))
                : true; // zjednodušení – v reálu by se volalo repository
        return ResponseEntity.ok(Map.of("available", available));
    }
}
