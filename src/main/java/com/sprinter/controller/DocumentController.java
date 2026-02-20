package com.sprinter.controller;

import com.sprinter.dto.DocumentDto;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.DocumentService;
import com.sprinter.service.FavoriteService;
import com.sprinter.service.ProjectService;
import com.sprinter.domain.repository.ProjectRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller pro správu dokumentů.
 */
@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService    documentService;
    private final ProjectService     projectService;
    private final ProjectRepository  projectRepository;
    private final WorkItemRepository workItemRepository;
    private final FavoriteService    favoriteService;

    // ---- Seznam dokumentů (globální) ----

    @GetMapping("/documents")
    public String listDocuments(@RequestParam(required = false) Long folderId, Model model) {
        model.addAttribute("documents",        documentService.findAll(folderId));
        model.addAttribute("folders",          documentService.findFolders(null));
        model.addAttribute("foldersFlat",      documentService.findFoldersFlat(null));
        model.addAttribute("selectedFolderId", folderId);
        model.addAttribute("pageTitle",        "Dokumenty");
        model.addAttribute("activeNav",        "documents");
        return "documents/list";
    }

    // ---- Formulář nového dokumentu ----

    @GetMapping("/documents/new")
    public String newDocumentForm(@RequestParam(required = false) Long projectId, Model model) {
        var dto = new DocumentDto();
        if (projectId != null) {
            dto.setProjectId(projectId);
            projectService.requireAccess(projectId);
            model.addAttribute("project", projectRepository.findById(projectId).orElse(null));
        }
        addFormAttributes(model, projectId);
        model.addAttribute("documentDto", dto);
        model.addAttribute("pageTitle", "Nový dokument");
        return "documents/form";
    }

    @PostMapping("/documents")
    public String createDocument(@Valid @ModelAttribute("documentDto") DocumentDto dto,
                                 BindingResult binding,
                                 RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            addFormAttributes(model, dto.getProjectId());
            if (dto.getProjectId() != null) {
                model.addAttribute("project", projectRepository.findById(dto.getProjectId()).orElse(null));
            }
            return "documents/form";
        }
        try {
            var doc = documentService.createDocument(dto.getTitle(), dto.getContent(), dto.getProjectId());
            flash.addFlashAttribute("successMessage", "Dokument byl vytvořen.");
            return "redirect:/documents/" + doc.getId();
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents/new";
        }
    }

    // ---- Detail dokumentu ----

    @GetMapping("/documents/{id}")
    public String documentDetail(@PathVariable Long id, Model model) {
        var doc = documentService.findById(id);
        if (doc.getProject() != null) projectService.requireAccess(doc.getProject().getId());

        model.addAttribute("document",       doc);
        model.addAttribute("pageTitle",      doc.getTitle());
        model.addAttribute("currentUserId",  SecurityUtils.getCurrentUserId().orElse(null));
        model.addAttribute("allProjects",    projectRepository.findAll());
        model.addAttribute("allFolders",     documentService.findAllAccessibleFolders(
                doc.getProject() != null ? doc.getProject().getId() : null));
        model.addAttribute("isFavorite",     favoriteService.isFavorite("document", id));
        if (doc.getProject() != null) {
            model.addAttribute("project",         doc.getProject());
            model.addAttribute("currentUserRole",
                    projectService.getCurrentUserRole(doc.getProject().getId()).orElse(null));
            model.addAttribute("activeTab", "documents");
        }
        return "documents/detail";
    }

    // ---- Editace dokumentu ----

    @GetMapping("/documents/{id}/edit")
    public String editDocumentForm(@PathVariable Long id, Model model) {
        var doc = documentService.findById(id);
        if (doc.getProject() != null) projectService.requireContentEditAccess(doc.getProject().getId());

        var dto = new DocumentDto();
        dto.setTitle(doc.getTitle());
        dto.setContent(doc.getContent());
        dto.setProjectId(doc.getProject() != null ? doc.getProject().getId() : null);
        doc.getLinkedWorkItems().forEach(wi -> dto.getLinkedWorkItemIds().add(wi.getId()));

        addFormAttributes(model, dto.getProjectId());
        model.addAttribute("documentDto", dto);
        model.addAttribute("document",    doc);
        model.addAttribute("pageTitle",   "Editace: " + doc.getTitle());
        if (doc.getProject() != null) {
            model.addAttribute("project", doc.getProject());
        }
        return "documents/form";
    }

    @PostMapping("/documents/{id}/edit")
    public String updateDocument(@PathVariable Long id,
                                 @Valid @ModelAttribute("documentDto") DocumentDto dto,
                                 BindingResult binding,
                                 RedirectAttributes flash, Model model) {
        if (binding.hasErrors()) {
            var doc = documentService.findById(id);
            addFormAttributes(model, dto.getProjectId());
            model.addAttribute("document", doc);
            if (doc.getProject() != null) model.addAttribute("project", doc.getProject());
            return "documents/form";
        }
        try {
            documentService.updateDocument(id, dto.getTitle(), dto.getContent(), dto.getLinkedWorkItemIds());
            flash.addFlashAttribute("successMessage", "Dokument byl aktualizován.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    // ---- Smazání ----

    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes flash) {
        var doc = documentService.findById(id);
        Long projectId = doc.getProject() != null ? doc.getProject().getId() : null;
        try {
            documentService.deleteDocument(id);
            flash.addFlashAttribute("successMessage", "Dokument byl smazán.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents/" + id;
        }
        return projectId != null ? "redirect:/projects/" + projectId + "/documents" : "redirect:/documents";
    }

    // ---- Komentáře ----

    @PostMapping("/documents/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             RedirectAttributes flash) {
        try {
            documentService.addComment(id, content);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + id + "#comments";
    }

    @PostMapping("/documents/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam Long documentId,
                                RedirectAttributes flash) {
        try {
            documentService.deleteComment(commentId);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + documentId + "#comments";
    }

    // ---- Přiřazení k projektu ----

    @PostMapping("/documents/{id}/project")
    public String assignProject(@PathVariable Long id,
                                @RequestParam(required = false) Long projectId,
                                RedirectAttributes flash) {
        try {
            documentService.assignToProject(id, projectId);
            flash.addFlashAttribute("successMessage",
                    projectId != null ? "Dokument byl přiřazen k projektu." : "Dokument byl odpojen od projektu.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    // ---- Vazby na pracovní položky ----

    @PostMapping("/documents/{id}/links")
    public String addLink(@PathVariable Long id,
                          @RequestParam Long workItemId,
                          RedirectAttributes flash) {
        try {
            documentService.linkWorkItem(id, workItemId);
            flash.addFlashAttribute("successMessage", "Vazba přidána.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    @PostMapping("/documents/{id}/links/{workItemId}/remove")
    public String removeLink(@PathVariable Long id,
                             @PathVariable Long workItemId,
                             RedirectAttributes flash) {
        try {
            documentService.unlinkWorkItem(id, workItemId);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/documents/" + id;
    }

    // ---- Dokumenty projektu ----

    @GetMapping("/projects/{projectId}/documents")
    public String projectDocuments(@PathVariable Long projectId,
                                   @RequestParam(required = false) Long folderId,
                                   Model model) {
        var project = projectService.findById(projectId);
        projectService.requireAccess(projectId);
        model.addAttribute("project",         project);
        model.addAttribute("documents",        documentService.findByProject(projectId, folderId));
        model.addAttribute("folders",          documentService.findFolders(projectId));
        model.addAttribute("foldersFlat",      documentService.findFoldersFlat(projectId));
        model.addAttribute("selectedFolderId", folderId);
        model.addAttribute("currentUserRole",
                projectService.getCurrentUserRole(projectId).orElse(null));
        model.addAttribute("activeTab",       "documents");
        model.addAttribute("pageTitle",       project.getName() + " – Dokumenty");
        return "documents/project-list";
    }

    // ---- Složky dokumentů ----

    @PostMapping("/documents/folders")
    public String createFolder(@RequestParam String name,
                               @RequestParam(required = false) Long projectId,
                               @RequestParam(required = false) Long parentId,
                               RedirectAttributes flash) {
        try {
            documentService.createFolder(name, projectId, parentId);
            flash.addFlashAttribute("successMessage", "Složka byla vytvořena.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return projectId != null
                ? "redirect:/projects/" + projectId + "/documents"
                : "redirect:/documents";
    }

    @PostMapping("/documents/folders/{id}/delete")
    public String deleteFolder(@PathVariable Long id,
                               @RequestParam(required = false) Long projectId,
                               RedirectAttributes flash) {
        try {
            documentService.deleteFolder(id);
            flash.addFlashAttribute("successMessage", "Složka byla smazána.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return projectId != null
                ? "redirect:/projects/" + projectId + "/documents"
                : "redirect:/documents";
    }

    @PostMapping("/documents/{id}/folder")
    public String moveToFolder(@PathVariable Long id,
                               @RequestParam(required = false) Long folderId,
                               @RequestParam(defaultValue = "") String returnTo,
                               RedirectAttributes flash) {
        try {
            documentService.moveToFolder(id, folderId);
            flash.addFlashAttribute("successMessage", "Dokument přesunut.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return returnTo.isBlank() ? "redirect:/documents/" + id : "redirect:" + returnTo;
    }

    // ---- Pomocné ----

    private void addFormAttributes(Model model, Long projectId) {
        if (projectId != null) {
            // Pracovní položky projektu pro výběr vazeb
            model.addAttribute("projectWorkItems",
                    workItemRepository.findByProjectIdOrderByItemNumberDesc(projectId));
        }
        model.addAttribute("projects", projectRepository.findAll());
    }
}
