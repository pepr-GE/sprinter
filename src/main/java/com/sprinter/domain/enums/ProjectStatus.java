package com.sprinter.domain.enums;

/**
 * Stav projektu nebo podprojektu.
 */
public enum ProjectStatus {
    ACTIVE("Aktivní", "proj-active"),
    ON_HOLD("Pozastaveno", "proj-onhold"),
    COMPLETED("Dokončeno", "proj-completed"),
    ARCHIVED("Archivováno", "proj-archived");

    private final String displayName;
    private final String cssClass;

    ProjectStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() { return displayName; }
    public String getCssClass()    { return cssClass; }

    public boolean isActive()   { return this == ACTIVE; }
    public boolean isArchived() { return this == ARCHIVED; }
}
