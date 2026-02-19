package com.sprinter.service;

import com.sprinter.domain.entity.Project;
import com.sprinter.domain.entity.ProjectMember;
import com.sprinter.domain.entity.User;
import com.sprinter.domain.enums.ProjectRole;
import com.sprinter.domain.enums.ProjectStatus;
import com.sprinter.domain.repository.ProjectMemberRepository;
import com.sprinter.domain.repository.ProjectRepository;
import com.sprinter.exception.AccessDeniedException;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.exception.ValidationException;
import com.sprinter.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servisní třída pro správu projektů a podprojektů.
 *
 * <p>Implementuje business logiku pro:
 * <ul>
 *   <li>CRUD projektů a podprojektů</li>
 *   <li>Správu projektového týmu (přidávání/odebírání členů)</li>
 *   <li>Dědičnost oprávnění z nadřazeného projektu</li>
 *   <li>Kontrolu přístupu k projektům</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectService {

    private final ProjectRepository        projectRepository;
    private final ProjectMemberRepository  memberRepository;
    private final UserService              userService;

    // ---- Čtení ----

    /**
     * Vrátí projekt dle ID.
     *
     * @throws ResourceNotFoundException pokud projekt neexistuje
     */
    @Transactional(readOnly = true)
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projekt", id));
    }

    /**
     * Vrátí projekt dle klíče.
     *
     * @throws ResourceNotFoundException pokud projekt neexistuje
     */
    @Transactional(readOnly = true)
    public Project findByKey(String projectKey) {
        return projectRepository.findByProjectKeyIgnoreCase(projectKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projekt s klíčem '" + projectKey + "' nebyl nalezen."));
    }

    /**
     * Vrátí projekty viditelné pro aktuálního uživatele.
     * Administrátor vidí všechny projekty; ostatní vidí jen projekty, kde jsou členy.
     */
    @Transactional(readOnly = true)
    public List<Project> findProjectsForCurrentUser() {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return projectRepository.findByParentIsNullAndStatusOrderByNameAsc(ProjectStatus.ACTIVE);
        }
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Není přihlášen žádný uživatel."));
        return projectRepository.findRootProjectsForUser(userId);
    }

    /**
     * Vrátí všechny projekty (pouze pro adminy).
     */
    @Transactional(readOnly = true)
    public List<Project> findAllProjects() {
        return projectRepository.findRootProjects();
    }

    /**
     * Vrátí podprojekty daného projektu.
     */
    @Transactional(readOnly = true)
    public List<Project> findSubprojects(Long parentId) {
        return projectRepository.findByParentIdOrderByNameAsc(parentId);
    }

    // ---- Oprávnění ----

    /**
     * Vrátí roli aktuálního uživatele v projektu.
     * Zahrnuje dědičnost z nadřazeného projektu.
     * Admin vždy dostane roli MANAGER.
     *
     * @return Optional s rolí, nebo prázdný Optional pokud uživatel nemá přístup
     */
    @Transactional(readOnly = true)
    public Optional<ProjectRole> getCurrentUserRole(Long projectId) {
        if (SecurityUtils.isCurrentUserAdmin()) {
            return Optional.of(ProjectRole.MANAGER);
        }
        Long userId = SecurityUtils.getCurrentUserId().orElse(null);
        if (userId == null) return Optional.empty();

        return getEffectiveRole(projectId, userId);
    }

    /**
     * Vrátí efektivní roli uživatele v projektu s dědičností z rodiče.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectRole> getEffectiveRole(Long projectId, Long userId) {
        // Přímé přiřazení
        Optional<ProjectRole> directRole = memberRepository.findRoleByProjectIdAndUserId(projectId, userId);
        if (directRole.isPresent()) return directRole;

        // Dědičnost z nadřazeného projektu
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null && project.getParent() != null) {
            return getEffectiveRole(project.getParent().getId(), userId);
        }

        return Optional.empty();
    }

    /**
     * Ověří, zda má aktuální uživatel přístup k projektu.
     *
     * @throws AccessDeniedException pokud uživatel nemá přístup
     */
    public void requireAccess(Long projectId) {
        if (!getCurrentUserRole(projectId).isPresent()) {
            throw new AccessDeniedException("Nemáte přístup k tomuto projektu.");
        }
    }

    /**
     * Ověří, zda může aktuální uživatel editovat obsah projektu.
     *
     * @throws AccessDeniedException pokud uživatel nemá oprávnění
     */
    public void requireContentEditAccess(Long projectId) {
        ProjectRole role = getCurrentUserRole(projectId)
                .orElseThrow(() -> new AccessDeniedException("Nemáte přístup k tomuto projektu."));
        if (!role.canEditContent()) {
            throw new AccessDeniedException("Nemáte oprávnění upravovat obsah tohoto projektu.");
        }
    }

    /**
     * Ověří, zda může aktuální uživatel spravovat projekt.
     *
     * @throws AccessDeniedException pokud uživatel nemá oprávnění
     */
    public void requireManageAccess(Long projectId) {
        ProjectRole role = getCurrentUserRole(projectId)
                .orElseThrow(() -> new AccessDeniedException("Nemáte přístup k tomuto projektu."));
        if (!role.canManageProject()) {
            throw new AccessDeniedException("Pouze vedoucí projektu může spravovat toto nastavení.");
        }
    }

    // ---- Vytváření ----

    /**
     * Vytvoří nový kořenový projekt.
     */
    public Project createProject(String name, String projectKey, String description,
                                  User owner, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        validateProjectKey(projectKey, null);

        var project = Project.builder()
                .name(name.trim())
                .projectKey(projectKey.trim().toUpperCase())
                .description(description)
                .owner(owner)
                .startDate(startDate)
                .endDate(endDate)
                .status(ProjectStatus.ACTIVE)
                .build();

        project = projectRepository.save(project);

        // Vlastník projektu je automaticky vedoucí (MANAGER)
        addMember(project.getId(), owner.getId(), ProjectRole.MANAGER);

        log.info("Vytvořen projekt: {} ({})", project.getName(), project.getProjectKey());
        return project;
    }

    /**
     * Vytvoří podprojekt.
     */
    public Project createSubproject(Long parentId, String name, String projectKey,
                                     String description, User owner,
                                     java.time.LocalDate startDate, java.time.LocalDate endDate) {
        requireManageAccess(parentId);
        validateProjectKey(projectKey, null);

        var parent = findById(parentId);
        var project = Project.builder()
                .name(name.trim())
                .projectKey(projectKey.trim().toUpperCase())
                .description(description)
                .owner(owner)
                .parent(parent)
                .startDate(startDate)
                .endDate(endDate)
                .status(ProjectStatus.ACTIVE)
                .build();

        project = projectRepository.save(project);

        // Vlastník podprojektu je automaticky vedoucí
        addMember(project.getId(), owner.getId(), ProjectRole.MANAGER);

        log.info("Vytvořen podprojekt: {} ({}) pod projektem {}",
                  project.getName(), project.getProjectKey(), parent.getProjectKey());
        return project;
    }

    /**
     * Aktualizuje projekt.
     */
    public Project updateProject(Long id, String name, String description,
                                  ProjectStatus status, java.time.LocalDate startDate,
                                  java.time.LocalDate endDate) {
        requireManageAccess(id);

        var project = findById(id);
        project.setName(name.trim());
        project.setDescription(description);
        project.setStatus(status);
        project.setStartDate(startDate);
        project.setEndDate(endDate);

        return projectRepository.save(project);
    }

    /**
     * Archivuje projekt.
     */
    public void archiveProject(Long id) {
        requireManageAccess(id);

        var project = findById(id);
        project.setStatus(ProjectStatus.ARCHIVED);
        projectRepository.save(project);
        log.info("Archivován projekt ID={}: {}", id, project.getName());
    }

    // ---- Správa členů ----

    /**
     * Přidá uživatele do projektového týmu.
     */
    public ProjectMember addMember(Long projectId, Long userId, ProjectRole role) {
        if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            // Aktualizace existujícího členství
            return updateMemberRole(projectId, userId, role);
        }

        var project = findById(projectId);
        var user    = userService.findById(userId);

        var member = ProjectMember.builder()
                .project(project)
                .user(user)
                .projectRole(role)
                .build();

        member = memberRepository.save(member);
        log.info("Přidán člen {} do projektu {} s rolí {}",
                  user.getUsername(), project.getProjectKey(), role);
        return member;
    }

    /**
     * Aktualizuje roli člena projektu.
     */
    public ProjectMember updateMemberRole(Long projectId, Long userId, ProjectRole newRole) {
        var member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uživatel není členem projektu."));
        member.setProjectRole(newRole);
        return memberRepository.save(member);
    }

    /**
     * Odebere uživatele z projektového týmu.
     */
    public void removeMember(Long projectId, Long userId) {
        requireManageAccess(projectId);

        var project = findById(projectId);
        // Nesmíme odebrat posledního vedoucího
        long managerCount = project.getMembers().stream()
                .filter(m -> m.getProjectRole() == ProjectRole.MANAGER)
                .count();

        var member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel není členem projektu."));

        if (member.getProjectRole() == ProjectRole.MANAGER && managerCount <= 1) {
            throw new ValidationException("Projekt musí mít alespoň jednoho vedoucího.");
        }

        memberRepository.delete(member);
        log.info("Odebrán člen (userId={}) z projektu ID={}", userId, projectId);
    }

    /**
     * Vrátí seznam členů projektu.
     */
    @Transactional(readOnly = true)
    public List<ProjectMember> findMembers(Long projectId) {
        return memberRepository.findByProjectIdOrderByUserLastNameAsc(projectId);
    }

    // ---- Generování klíče ----

    /**
     * Generuje návrh klíče projektu z názvu.
     * Např. "Nový projekt" → "NP", "Vývoj webu" → "VW"
     */
    public String suggestProjectKey(String name) {
        if (name == null || name.isBlank()) return "PRJ";
        var words = name.trim().toUpperCase().split("\\s+");
        if (words.length == 1) {
            return words[0].replaceAll("[^A-Z0-9]", "")
                           .substring(0, Math.min(4, words[0].length()));
        }
        var sb = new StringBuilder();
        for (var word : words) {
            var cleaned = word.replaceAll("[^A-Z0-9]", "");
            if (!cleaned.isEmpty()) sb.append(cleaned.charAt(0));
            if (sb.length() >= 4) break;
        }
        return sb.length() >= 2 ? sb.toString() : "PRJ";
    }

    // ---- Validace ----

    private void validateProjectKey(String key, Long excludeId) {
        if (!key.matches("[A-Z][A-Z0-9]{1,9}")) {
            throw new ValidationException(
                    "Klíč projektu musí začínat písmenem a obsahovat jen velká písmena a číslice (2-10 znaků).");
        }
        if (excludeId == null) {
            if (projectRepository.existsByProjectKeyIgnoreCase(key)) {
                throw new ValidationException("Klíč projektu '" + key + "' je již použit.");
            }
        } else {
            if (projectRepository.existsByProjectKeyIgnoreCaseAndIdNot(key, excludeId)) {
                throw new ValidationException("Klíč projektu '" + key + "' je již použit.");
            }
        }
    }
}
