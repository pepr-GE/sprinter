package com.sprinter.domain.enums;

/**
 * Stavy pracovní položky (workflow).
 *
 * Stavy odpovídají sloupcům na Kanban tabuli.
 * Pořadí {@link #ordinal()} slouží k řazení sloupců na boardu.
 */
public enum WorkItemStatus {
    TO_DO("K řešení", "status-todo", false),
    IN_PROGRESS("Probíhá", "status-inprogress", false),
    IN_REVIEW("Kontrola", "status-inreview", false),
    DONE("Dokončeno", "status-done", true),
    CANCELLED("Zrušeno", "status-cancelled", true);

    private final String displayName;
    /** CSS třída pro vizuální odlišení stavu. */
    private final String cssClass;
    /** True = položka je uzavřena (považována za dokončenou nebo zrušenou). */
    private final boolean terminal;

    WorkItemStatus(String displayName, String cssClass, boolean terminal) {
        this.displayName = displayName;
        this.cssClass = cssClass;
        this.terminal = terminal;
    }

    public String getDisplayName() { return displayName; }
    public String getCssClass()    { return cssClass; }
    public boolean isTerminal()    { return terminal; }

    /** Vrátí stavy, které jsou zobrazovány na Kanban tabuli jako sloupce. */
    public static WorkItemStatus[] kanbanStatuses() {
        return new WorkItemStatus[]{TO_DO, IN_PROGRESS, IN_REVIEW, DONE};
    }
}
