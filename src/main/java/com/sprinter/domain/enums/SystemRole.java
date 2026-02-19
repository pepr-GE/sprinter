package com.sprinter.domain.enums;

/**
 * Systémové role uživatele v celé aplikaci.
 *
 * <ul>
 *   <li>{@link #ADMIN} – správce systému: může spravovat uživatele, všechny projekty a vše ostatní.</li>
 *   <li>{@link #USER}  – běžný uživatel: přístup závisí na přiřazení k projektům.</li>
 * </ul>
 *
 * Konkrétní oprávnění uvnitř projektu definuje {@link ProjectRole}.
 */
public enum SystemRole {
    ADMIN("Správce systému"),
    USER("Uživatel");

    private final String displayName;

    SystemRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
