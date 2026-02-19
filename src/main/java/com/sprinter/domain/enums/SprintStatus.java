package com.sprinter.domain.enums;

/**
 * Stav sprintu.
 */
public enum SprintStatus {
    PLANNING("Plánování", "sprint-planning"),
    ACTIVE("Aktivní", "sprint-active"),
    COMPLETED("Dokončen", "sprint-completed"),
    CANCELLED("Zrušen", "sprint-cancelled");

    private final String displayName;
    private final String cssClass;

    SprintStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }

    public String getDisplayName() { return displayName; }
    public String getCssClass()    { return cssClass; }

    public boolean isActive()    { return this == ACTIVE; }
    public boolean isTerminal()  { return this == COMPLETED || this == CANCELLED; }
}
