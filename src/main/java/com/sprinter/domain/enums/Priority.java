package com.sprinter.domain.enums;

/**
 * Priorita pracovní položky.
 * Pořadí definuje závažnost od nejnižší (LOWEST) po kritickou (CRITICAL).
 */
public enum Priority {
    LOWEST("Nejnižší", "bi-arrow-down-circle", "priority-lowest", 1),
    LOW("Nízká", "bi-arrow-down", "priority-low", 2),
    MEDIUM("Střední", "bi-dash-circle", "priority-medium", 3),
    HIGH("Vysoká", "bi-arrow-up", "priority-high", 4),
    HIGHEST("Nejvyšší", "bi-arrow-up-circle", "priority-highest", 5),
    CRITICAL("Kritická", "bi-exclamation-octagon", "priority-critical", 6);

    private final String displayName;
    private final String iconClass;
    private final String cssClass;
    private final int level;

    Priority(String displayName, String iconClass, String cssClass, int level) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.cssClass = cssClass;
        this.level = level;
    }

    public String getDisplayName() { return displayName; }
    public String getIconClass()   { return iconClass; }
    public String getCssClass()    { return cssClass; }
    public int getLevel()          { return level; }
}
