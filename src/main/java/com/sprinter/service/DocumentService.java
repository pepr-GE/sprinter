package com.sprinter.service;

import com.sprinter.domain.entity.Document;
import com.sprinter.domain.entity.DocumentComment;
import com.sprinter.domain.entity.DocumentFolder;
import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.repository.DocumentCommentRepository;
import com.sprinter.domain.repository.DocumentFolderRepository;
import com.sprinter.domain.repository.DocumentRepository;
import com.sprinter.domain.repository.ProjectRepository;
import com.sprinter.domain.repository.UserRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.exception.AccessDeniedException;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Plochá reprezentace složky pro select dropdown (zahrnuje hloubku v hierarchii). */
record FolderOption(Long id, String name, int depth) {
    public String displayName() {
        return "—".repeat(depth) + (depth > 0 ? " " : "") + name;
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DocumentService {

    private final DocumentRepository        documentRepository;
    private final DocumentFolderRepository  folderRepository;
    private final DocumentCommentRepository commentRepository;
    private final ProjectRepository         projectRepository;
    private final UserRepository            userRepository;
    private final WorkItemRepository        workItemRepository;
    private final ProjectService            projectService;

    // ---- Vyhledávání ----

    @Transactional(readOnly = true)
    public Document findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dokument nenalezen: " + id));
    }

    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return findAll(null);
    }

    @Transactional(readOnly = true)
    public List<Document> findAll(Long folderId) {
        var projectIds = resolveAccessibleProjectIds(null);
        if (folderId != null) {
            return projectIds.isEmpty()
                    ? List.of()
                    : documentRepository.findByFolderAndProjects(folderId, projectIds);
        }
        return projectIds.isEmpty()
                ? documentRepository.findByProjectIsNullOrderByUpdatedAtDescCreatedAtDesc()
                : documentRepository.findAllForProjects(projectIds);
    }

    /**
     * Vrátí všechny dokumenty přístupné aktuálnímu uživateli v kontextu dané pracovní položky.
     * Zahrnuje dokumenty z projektů, jichž je uživatel členem, + dokumenty z explicitně
     * předaného projektu (pokud user nemá přímé členství, ale má přístup přes dědičnost/admin).
     */
    @Transactional(readOnly = true)
    public List<Document> findAllAccessible(Long contextProjectId) {
        var projectIds = resolveAccessibleProjectIds(contextProjectId);
        return projectIds.isEmpty()
                ? documentRepository.findByProjectIsNullOrderByUpdatedAtDescCreatedAtDesc()
                : documentRepository.findAllForProjects(projectIds);
    }

    /** Sestaví seznam ID projektů přístupných aktuálnímu uživateli. */
    private List<Long> resolveAccessibleProjectIds(Long extraProjectId) {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return projectRepository.findAll().stream().map(p -> p.getId()).toList();
        }
        var userId = SecurityUtils.getCurrentUserId().orElseThrow();
        var ids = new ArrayList<>(
                projectRepository.findProjectsForUser(userId)
                        .stream().map(p -> p.getId()).toList());
        if (extraProjectId != null && !ids.contains(extraProjectId)) {
            ids.add(extraProjectId);
        }
        return ids;
    }

    @Transactional(readOnly = true)
    public List<Document> findByProject(Long projectId) {
        return findByProject(projectId, null);
    }

    @Transactional(readOnly = true)
    public List<Document> findByProject(Long projectId, Long folderId) {
        projectService.requireAccess(projectId);
        if (folderId != null) {
            return documentRepository.findByProjectAndFolder(projectId, folderId);
        }
        return documentRepository.findByProjectIdOrderByUpdatedAtDescCreatedAtDesc(projectId);
    }

    // ---- Vytváření / editace ----

    public Document createDocument(String title, String content, Long projectId) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow(
                () -> new AccessDeniedException("Nepřihlášený uživatel"));
        var author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel nenalezen"));

        var doc = Document.builder()
                .title(title)
                .content(content)
                .author(author)
                .build();

        if (projectId != null) {
            projectService.requireContentEditAccess(projectId);
            var project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projekt nenalezen"));
            doc.setProject(project);
        }

        return documentRepository.save(doc);
    }

    public Document updateDocument(Long id, String title, String content, Set<Long> linkedWorkItemIds) {
        var doc = findById(id);
        requireEditAccess(doc);

        doc.setTitle(title);
        doc.setContent(content);

        // Aktualizace vazeb na pracovní položky
        if (linkedWorkItemIds != null) {
            Set<WorkItem> items = linkedWorkItemIds.stream()
                    .map(wiId -> workItemRepository.findById(wiId)
                            .orElseThrow(() -> new ResourceNotFoundException("Položka nenalezena: " + wiId)))
                    .collect(Collectors.toSet());
            doc.getLinkedWorkItems().clear();
            doc.getLinkedWorkItems().addAll(items);
        }

        return documentRepository.save(doc);
    }

    public void deleteDocument(Long id) {
        var doc = findById(id);
        requireEditAccess(doc);
        documentRepository.delete(doc);
    }

    // ---- Komentáře ----

    public DocumentComment addComment(Long documentId, String content) {
        var doc = findById(documentId);
        var userId = SecurityUtils.getCurrentUserId().orElseThrow(
                () -> new AccessDeniedException("Nepřihlášený uživatel"));
        var author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel nenalezen"));

        var comment = DocumentComment.builder()
                .content(content)
                .document(doc)
                .author(author)
                .build();
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Komentář nenalezen"));
        var userId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean isAuthor  = userId != null && comment.getAuthor().getId().equals(userId);
        boolean canManage = comment.getDocument().getProject() != null
                && projectService.getCurrentUserRole(comment.getDocument().getProject().getId())
                        .map(r -> r.canManageProject()).orElse(false);
        if (!isAuthor && !canManage) {
            throw new AccessDeniedException("Nemůžete smazat tento komentář.");
        }
        commentRepository.delete(comment);
    }

    // ---- Přiřazení k projektu ----

    public Document assignToProject(Long docId, Long projectId) {
        var doc = findById(docId);
        requireEditAccess(doc);
        if (projectId == null) {
            doc.setProject(null);
        } else {
            projectService.requireAccess(projectId);
            var project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projekt nenalezen: " + projectId));
            doc.setProject(project);
        }
        return documentRepository.save(doc);
    }

    // ---- Vazby na pracovní položky ----

    public void linkWorkItem(Long documentId, Long workItemId) {
        var doc = findById(documentId);
        requireEditAccess(doc);
        var item = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Položka nenalezena: " + workItemId));
        doc.getLinkedWorkItems().add(item);
        documentRepository.save(doc);
    }

    public void unlinkWorkItem(Long documentId, Long workItemId) {
        var doc = findById(documentId);
        requireEditAccess(doc);
        doc.getLinkedWorkItems().removeIf(wi -> wi.getId().equals(workItemId));
        documentRepository.save(doc);
    }

    // ---- Složky dokumentů ----

    @Transactional(readOnly = true)
    public List<DocumentFolder> findFolders(Long projectId) {
        return projectId != null
                ? folderRepository.findByProjectIdAndParentIsNullOrderByNameAsc(projectId)
                : folderRepository.findByProjectIsNullAndParentIsNullOrderByNameAsc();
    }

    /**
     * Vrátí všechny přístupné složky jako plochou strukturu s hloubkou (pro select dropdown).
     * Pořadí je depth-first: kořenová složka, pak její podsložky (rekurzivně), pak další kořenová...
     */
    @Transactional(readOnly = true)
    public List<FolderOption> findFoldersFlat(Long projectId) {
        var result = new ArrayList<FolderOption>();
        folderRepository.findByProjectIsNullAndParentIsNullOrderByNameAsc()
                .forEach(f -> flattenFolder(f, 0, result));
        if (projectId != null) {
            folderRepository.findByProjectIdAndParentIsNullOrderByNameAsc(projectId)
                    .forEach(f -> flattenFolder(f, 0, result));
        }
        return result;
    }

    private void flattenFolder(DocumentFolder folder, int depth, List<FolderOption> result) {
        result.add(new FolderOption(folder.getId(), folder.getName(), depth));
        folder.getChildren().forEach(child -> flattenFolder(child, depth + 1, result));
    }

    /**
     * Vrátí všechny složky dostupné pro daný dokument:
     * globální složky (bez projektu) + složky daného projektu (pokud je uveden).
     */
    @Transactional(readOnly = true)
    public List<DocumentFolder> findAllAccessibleFolders(Long projectId) {
        var result = new java.util.ArrayList<DocumentFolder>();
        result.addAll(folderRepository.findByProjectIsNullOrderByNameAsc());
        if (projectId != null) {
            result.addAll(folderRepository.findByProjectIdOrderByNameAsc(projectId));
        }
        result.sort(java.util.Comparator.comparing(DocumentFolder::getName));
        return result;
    }

    public DocumentFolder createFolder(String name, Long projectId, Long parentId) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow(
                () -> new AccessDeniedException("Nepřihlášený uživatel"));
        var author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel nenalezen"));

        var folder = DocumentFolder.builder()
                .name(name.trim())
                .createdBy(author)
                .build();

        if (projectId != null) {
            projectService.requireContentEditAccess(projectId);
            var project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projekt nenalezen"));
            folder.setProject(project);
        }
        if (parentId != null) {
            var parent = folderRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Nadřazená složka nenalezena"));
            folder.setParent(parent);
        }
        return folderRepository.save(folder);
    }

    public void deleteFolder(Long folderId) {
        var folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Složka nenalezena"));
        if (folder.getProject() != null) {
            projectService.requireContentEditAccess(folder.getProject().getId());
        }
        folderRepository.delete(folder);
    }

    public Document moveToFolder(Long docId, Long folderId) {
        var doc = findById(docId);
        requireEditAccess(doc);
        if (folderId == null) {
            doc.setFolder(null);
        } else {
            var folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Složka nenalezena"));
            doc.setFolder(folder);
        }
        return documentRepository.save(doc);
    }

    // ---- Pomocné ----

    private void requireEditAccess(Document doc) {
        var userId = SecurityUtils.getCurrentUserId().orElseThrow(
                () -> new AccessDeniedException("Nepřihlášený uživatel"));
        boolean isAuthor = doc.getAuthor().getId().equals(userId);
        boolean canManage = doc.getProject() != null
                && projectService.getCurrentUserRole(doc.getProject().getId())
                        .map(r -> r.canManageProject()).orElse(false);
        if (!isAuthor && !canManage) {
            throw new AccessDeniedException("Nemáte oprávnění upravovat tento dokument.");
        }
    }
}
