package com.sprinter.domain.enums;

/**
 * Role uživatele v rámci konkrétního projektu.
 *
 * <ul>
 *   <li>{@link #MANAGER}     – vedoucí projektu: může projekt upravovat, spravovat členy,
 *                               zakládat sprinty a vše v projektu.</li>
 *   <li>{@link #TEAM_MEMBER} – člen týmu: může vytvářet a upravovat úkoly/problémy/články
 *                               ve svých projektech, ale nemůže upravovat projekt samotný.</li>
 *   <li>{@link #OBSERVER}    – pozorovatel: může pouze číst obsah a přidávat komentáře.</li>
 * </ul>
 */
public enum ProjectRole {
    MANAGER("Vedoucí projektu"),
    TEAM_MEMBER("Člen týmu"),
    OBSERVER("Pozorovatel");

    private final String displayName;

    ProjectRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Vrátí true, pokud má role oprávnění upravovat obsah projektu (úkoly apod.). */
    public boolean canEditContent() {
        return this == MANAGER || this == TEAM_MEMBER;
    }

    /** Vrátí true, pokud má role oprávnění spravovat projekt (nastavení, členy, sprinty). */
    public boolean canManageProject() {
        return this == MANAGER;
    }

    /** Vrátí true, pokud má role oprávnění přidávat komentáře. */
    public boolean canComment() {
        return true;   // všechny role mohou komentovat
    }
}
