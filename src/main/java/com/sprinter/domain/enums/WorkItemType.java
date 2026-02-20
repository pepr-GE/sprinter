package com.sprinter.domain.enums;

/**
 * Typy pracovních položek (work items) v projektu.
 *
 * <ul>
 *   <li>{@link #TASK}    – úkol: konkrétní pracovní úkol s termínem a přiřazenou osobou.</li>
 *   <li>{@link #ISSUE}   – problém/bug: chyba nebo incident, který je třeba vyřešit.</li>
 *   <li>{@link #STORY}   – uživatelský příběh (user story): popis funkcionality z pohledu uživatele.</li>
 *   <li>{@link #EPIC}    – epos: velký pracovní celek seskupující stories/tasky.</li>
 *   <li>{@link #ARTICLE} – článek/dokument: dokumentace nebo wiki stránka.</li>
 * </ul>
 */
public enum WorkItemType {
    TASK("Úkol", "bi-check-square", "type-task"),
    ISSUE("Problém", "bi-bug", "type-issue"),
    STORY("User Story", "bi-bookmark", "type-story"),
    EPIC("Epos", "bi-lightning", "type-epic"),
    ARTICLE("Článek", "bi-file-text", "type-article");

    private final String displayName;
    /** Bootstrap Icons třída ikony pro daný typ. */
    private final String iconClass;
    /** CSS třída pro barevné rozlišení. */
    private final String cssClass;

    WorkItemType(String displayName, String iconClass, String cssClass) {
        this.displayName = displayName;
        this.iconClass = iconClass;
        this.cssClass = cssClass;
    }

    public String getDisplayName() { return displayName; }
    public String getIconClass()   { return iconClass; }
    public String getCssClass()    { return cssClass; }

    /** Vrátí true pro typy, které lze přiřadit ke sprintu. */
    public boolean isSprintable() {
        return this == TASK || this == ISSUE || this == STORY;
    }

    /** Vrátí true pro typy, které mohou mít odhad pracnosti (story points). */
    public boolean hasStoryPoints() {
        return this == TASK || this == ISSUE || this == STORY || this == EPIC;
    }

    /** Vrátí true pro typy viditelné v UI pro vytváření nových položek. */
    public boolean isVisible() {
        return this == TASK || this == ISSUE;
    }
}
