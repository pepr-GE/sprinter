package com.sprinter.domain.repository;

import com.sprinter.domain.entity.User;
import com.sprinter.domain.enums.SystemRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pro entitu {@link User}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Vyhledá uživatele dle přihlašovacího jména (case-insensitive). */
    Optional<User> findByUsernameIgnoreCase(String username);

    /** Vyhledá uživatele dle e-mailu (case-insensitive). */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Vrátí true, pokud username již existuje. */
    boolean existsByUsernameIgnoreCase(String username);

    /** Vrátí true, pokud e-mail již existuje. */
    boolean existsByEmailIgnoreCase(String email);

    /** Vrátí true, pokud e-mail existuje pro jiného uživatele. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Vrátí true, pokud username existuje pro jiného uživatele. */
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    /** Stránkovaný seznam aktivních uživatelů, volitelně filtrovaný dle jména/e-mailu. */
    @Query("""
           SELECT u FROM User u
           WHERE (:search IS NULL OR :search = ''
                  OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%')))
           ORDER BY u.lastName ASC, u.firstName ASC
           """)
    Page<User> findBySearchTerm(@Param("search") String search, Pageable pageable);

    /** Vrátí všechny uživatele se systémovou rolí ADMIN. */
    List<User> findBySystemRole(SystemRole systemRole);

    /**
     * Vrátí uživatele, kteří jsou členy daného projektu.
     * Používá se pro doplňování přiřazení.
     */
    @Query("""
           SELECT u FROM User u
           JOIN u.projectMemberships pm
           WHERE pm.project.id = :projectId AND u.active = true
           ORDER BY u.lastName ASC, u.firstName ASC
           """)
    List<User> findActiveProjectMembers(@Param("projectId") Long projectId);

    /**
     * Vrátí uživatele, kteří NEJSOU členy daného projektu
     * (kandidáti na přidání do projektu).
     */
    @Query("""
           SELECT u FROM User u
           WHERE u.active = true
             AND u.id NOT IN (
                 SELECT pm.user.id FROM ProjectMember pm WHERE pm.project.id = :projectId
             )
           ORDER BY u.lastName ASC, u.firstName ASC
           """)
    List<User> findUsersNotInProject(@Param("projectId") Long projectId);
}
