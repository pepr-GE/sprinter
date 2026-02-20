package com.sprinter.domain.entity;

import com.sprinter.domain.enums.SystemRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entita uživatele systému SPRINTER.
 *
 * <p>Každý uživatel má systémovou roli ({@link SystemRole}), která určuje základní
 * oprávnění. Konkrétní přístup k projektům je dále řízen přes {@link ProjectMember}.</p>
 */
@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_users_username", columnNames = "username"),
           @UniqueConstraint(name = "uq_users_email",    columnNames = "email")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"passwordHash", "projectMemberships", "assignedItems", "reportedItems"})
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_id_seq", allocationSize = 1)
    private Long id;

    /** Přihlašovací jméno (unikátní). */
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, length = 50)
    private String username;

    /** E-mailová adresa (unikátní). */
    @NotBlank
    @Email
    @Column(nullable = false, length = 150)
    private String email;

    /** BCrypt hash hesla. */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Křestní jméno. */
    @NotBlank
    @Size(max = 80)
    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    /** Příjmení. */
    @NotBlank
    @Size(max = 80)
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    /** Cesta k avataru (relativní vůči uploads adresáři). Null = výchozí avatar. */
    @Column(name = "avatar_path")
    private String avatarPath;

    /** Systémová role – ADMIN nebo USER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_role", nullable = false, length = 20)
    @Builder.Default
    private SystemRole systemRole = SystemRole.USER;

    /** Preferované UI téma uživatele (light/dark). */
    @Column(name = "ui_theme", length = 10)
    @Builder.Default
    private String uiTheme = "light";

    /** Jazyk rozhraní (pro budoucí i18n). */
    @Column(length = 5)
    @Builder.Default
    private String locale = "cs";

    /** Zda je účet aktivní (deaktivovaný uživatel se nemůže přihlásit). */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Časové razítko vytvoření záznamu. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Časové razítko poslední aktualizace záznamu. */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Čas posledního přihlášení. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** Čas přihlášení před posledním (pro activity feed na dashboardu). */
    @Column(name = "previous_last_login_at")
    private LocalDateTime previousLastLoginAt;

    // ---- Vztahy ----

    /** Členství v projektech. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProjectMember> projectMemberships = new HashSet<>();

    /** Pracovní položky přiřazené tomuto uživateli. */
    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WorkItem> assignedItems = new HashSet<>();

    /** Pracovní položky nahlášené (vytvořené) tímto uživatelem. */
    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WorkItem> reportedItems = new HashSet<>();

    // ---- Pomocné metody ----

    /** Vrátí celé jméno uživatele. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Vrátí iniciály pro avatar (pokud není nastaven obrázek). */
    public String getInitials() {
        String fi = firstName != null && !firstName.isEmpty() ? String.valueOf(firstName.charAt(0)) : "";
        String li = lastName  != null && !lastName.isEmpty()  ? String.valueOf(lastName.charAt(0))  : "";
        return (fi + li).toUpperCase();
    }

    /** Vrátí true, pokud je uživatel správce systému. */
    public boolean isAdmin() {
        return SystemRole.ADMIN == systemRole;
    }
}
