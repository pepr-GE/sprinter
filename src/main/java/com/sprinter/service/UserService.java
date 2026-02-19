package com.sprinter.service;

import com.sprinter.domain.entity.User;
import com.sprinter.domain.enums.SystemRole;
import com.sprinter.domain.repository.UserRepository;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.exception.ValidationException;
import com.sprinter.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servisní třída pro správu uživatelů.
 *
 * <p>Pokrývá CRUD operace, správu hesel, deaktivaci účtů
 * a aktualizaci preferencí (téma, jazyk).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ---- Čtení ----

    /**
     * Vrátí uživatele dle ID.
     *
     * @throws ResourceNotFoundException pokud uživatel neexistuje
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel", id));
    }

    /**
     * Vrátí uživatele dle přihlašovacího jména nebo e-mailu.
     *
     * @throws ResourceNotFoundException pokud uživatel neexistuje
     */
    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Uživatel nenalezen: " + usernameOrEmail));
    }

    /**
     * Stránkovaný seznam uživatelů s volitelným vyhledáváním.
     */
    @Transactional(readOnly = true)
    public Page<User> findAll(String search, Pageable pageable) {
        return userRepository.findBySearchTerm(search, pageable);
    }

    /**
     * Vrátí seznam aktivních členů projektu.
     */
    @Transactional(readOnly = true)
    public List<User> findProjectMembers(Long projectId) {
        return userRepository.findActiveProjectMembers(projectId);
    }

    /**
     * Vrátí uživatele, kteří nejsou v daném projektu (kandidáti na přidání).
     */
    @Transactional(readOnly = true)
    public List<User> findUsersNotInProject(Long projectId) {
        return userRepository.findUsersNotInProject(projectId);
    }

    // ---- Vytváření ----

    /**
     * Vytvoří nového uživatele.
     *
     * @param username      přihlašovací jméno
     * @param email         e-mail
     * @param plainPassword heslo v čistém textu (bude zahashováno)
     * @param firstName     křestní jméno
     * @param lastName      příjmení
     * @param systemRole    systémová role
     * @return vytvořený uživatel
     * @throws ValidationException pokud username nebo e-mail již existuje
     */
    public User createUser(String username, String email, String plainPassword,
                           String firstName, String lastName, SystemRole systemRole) {
        validateNewUser(username, email, null);

        var user = User.builder()
                .username(username.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(plainPassword))
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .systemRole(systemRole != null ? systemRole : SystemRole.USER)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("Vytvořen nový uživatel: {} ({})", user.getUsername(), user.getSystemRole());
        return user;
    }

    // ---- Aktualizace ----

    /**
     * Aktualizuje základní informace o uživateli.
     */
    public User updateUser(Long id, String username, String email,
                           String firstName, String lastName, SystemRole systemRole) {
        var user = findById(id);
        validateNewUser(username, email, id);

        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        if (systemRole != null) {
            user.setSystemRole(systemRole);
        }

        log.info("Aktualizován uživatel ID={}: {}", id, username);
        return userRepository.save(user);
    }

    /**
     * Změní heslo uživatele.
     *
     * @param id              ID uživatele
     * @param currentPassword aktuální heslo (pro ověření, null pokud mění admin)
     * @param newPassword     nové heslo
     * @throws ValidationException pokud aktuální heslo nesedí
     */
    public void changePassword(Long id, String currentPassword, String newPassword) {
        var user = findById(id);

        // Pokud se mění cizí heslo a volající je admin, přeskočíme ověření
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        if (!isAdmin || id.equals(currentUserId)) {
            // Ověření aktuálního hesla
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new ValidationException("Aktuální heslo není správné.");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Změněno heslo pro uživatele ID={}", id);
    }

    /**
     * Změní UI téma uživatele (light/dark).
     */
    public void updateTheme(Long userId, String theme) {
        var user = findById(userId);
        user.setUiTheme("dark".equalsIgnoreCase(theme) ? "dark" : "light");
        userRepository.save(user);
    }

    /**
     * Deaktivuje uživatelský účet.
     */
    public void deactivateUser(Long id) {
        var user = findById(id);
        user.setActive(false);
        userRepository.save(user);
        log.info("Deaktivován uživatel ID={}: {}", id, user.getUsername());
    }

    /**
     * Aktivuje uživatelský účet.
     */
    public void activateUser(Long id) {
        var user = findById(id);
        user.setActive(true);
        userRepository.save(user);
        log.info("Aktivován uživatel ID={}: {}", id, user.getUsername());
    }

    /**
     * Aktualizuje čas posledního přihlášení.
     * Voláno po úspěšném přihlášení.
     */
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    /**
     * Aktualizuje cestu k avataru uživatele.
     */
    public void updateAvatar(Long userId, String avatarPath) {
        var user = findById(userId);
        user.setAvatarPath(avatarPath);
        userRepository.save(user);
    }

    // ---- Validace ----

    private void validateNewUser(String username, String email, Long excludeId) {
        if (excludeId == null) {
            if (userRepository.existsByUsernameIgnoreCase(username)) {
                throw new ValidationException("Přihlašovací jméno '" + username + "' je již obsazeno.");
            }
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw new ValidationException("E-mail '" + email + "' je již registrován.");
            }
        } else {
            if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludeId)) {
                throw new ValidationException("Přihlašovací jméno '" + username + "' je již obsazeno.");
            }
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeId)) {
                throw new ValidationException("E-mail '" + email + "' je již registrován.");
            }
        }
    }

    // ---- Inicializace ----

    /**
     * Vytvoří výchozího správce systému, pokud ještě žádný neexistuje.
     * Voláno při startu aplikace.
     */
    public void ensureDefaultAdminExists() {
        var admins = userRepository.findBySystemRole(SystemRole.ADMIN);
        if (admins.isEmpty()) {
            createUser("admin", "admin@sprinter.local", "Admin123!",
                    "Správce", "Systému", SystemRole.ADMIN);
            log.warn("Vytvořen výchozí admin účet: admin / Admin123! – okamžitě změňte heslo!");
        }
    }
}
