package com.sprinter.dto;

import java.time.LocalDateTime;

/**
 * DTO pro jeden záznam v activity feedu (dashboard + stránka změn).
 */
public record ActivityEntry(
        String        type,          // "work_item" | "document" | "comment"
        String        entityKey,     // "PROJ-1" pro pracovní položky, jinak null
        String        title,         // název/text
        String        url,           // odkaz na detail
        LocalDateTime timestamp,     // čas poslední změny
        String        authorName,    // kdo provedl změnu
        String        icon,          // Bootstrap Icons třída
        String        badgeClass     // CSS třída pro badge (typ)
) implements Comparable<ActivityEntry> {

    @Override
    public int compareTo(ActivityEntry other) {
        return other.timestamp().compareTo(this.timestamp()); // sestupně
    }
}
