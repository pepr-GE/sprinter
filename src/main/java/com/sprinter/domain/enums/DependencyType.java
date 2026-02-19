package com.sprinter.domain.enums;

/**
 * Typ závislosti mezi pracovními položkami (pro Ganttův diagram).
 *
 * <ul>
 *   <li>{@link #FINISH_TO_START}  – Nástupník může začít teprve po dokončení předchůdce (nejčastější).</li>
 *   <li>{@link #START_TO_START}   – Nástupník může začít teprve po zahájení předchůdce.</li>
 *   <li>{@link #FINISH_TO_FINISH} – Nástupník může skončit teprve po dokončení předchůdce.</li>
 *   <li>{@link #START_TO_FINISH}  – Nástupník může skončit teprve po zahájení předchůdce (vzácné).</li>
 *   <li>{@link #BLOCKS}           – Předchůdce blokuje nástupníka (obecná závislost).</li>
 * </ul>
 */
public enum DependencyType {
    FINISH_TO_START("Dokončení → Start", "FS"),
    START_TO_START("Start → Start", "SS"),
    FINISH_TO_FINISH("Dokončení → Dokončení", "FF"),
    START_TO_FINISH("Start → Dokončení", "SF"),
    BLOCKS("Blokuje", "BL");

    private final String displayName;
    private final String abbreviation;

    DependencyType(String displayName, String abbreviation) {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
    }

    public String getDisplayName()  { return displayName; }
    public String getAbbreviation() { return abbreviation; }
}
