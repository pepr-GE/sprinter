package com.sprinter.web;

import com.sprinter.domain.entity.Document;
import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.repository.DocumentRepository;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.security.SecurityUtils;
import com.sprinter.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Přidává do každého modelu seznam nedávno zobrazených objektů a oblíbených pro levý sidebar.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final WorkItemRepository workItemRepository;
    private final DocumentRepository documentRepository;
    private final FavoriteService    favoriteService;

    /**
     * Přidá 5 naposledy pracovaných objektů do modelu pro sidebar.
     * Kombinuje pracovní položky (assignee/reporter) a dokumenty (autor).
     */
    @ModelAttribute
    public void addSidebarData(Model model) {
        SecurityUtils.getCurrentUserId().ifPresent(userId -> {
            // ---- Naposledy pracované ----
            var items  = workItemRepository.findRecentForUser(userId, PageRequest.of(0, 5));
            var docs   = documentRepository.findRecentByAuthor(userId, PageRequest.of(0, 5));

            var recent = new ArrayList<SidebarItem>();
            items.stream().map(SidebarItem::fromWorkItem).forEach(recent::add);
            docs.stream().map(SidebarItem::fromDocument).forEach(recent::add);
            recent.sort(Comparator.comparing(SidebarItem::updatedAt).reversed());
            model.addAttribute("sidebarRecent", recent.stream().limit(5).toList());

            // ---- Oblíbené ----
            model.addAttribute("sidebarFavorites", favoriteService.findForCurrentUser());
        });
    }

    /**
     * Lightweight DTO pro položku v sidebaru (nedávné).
     */
    public record SidebarItem(String title, String url, String icon,
                               java.time.LocalDateTime updatedAt) {

        static SidebarItem fromWorkItem(WorkItem wi) {
            return new SidebarItem(
                    "[" + wi.getItemKey() + "] " + wi.getTitle(),
                    "/items/" + wi.getId(),
                    wi.getType().getIconClass(),
                    wi.getUpdatedAt() != null ? wi.getUpdatedAt() : wi.getCreatedAt()
            );
        }

        static SidebarItem fromDocument(Document doc) {
            return new SidebarItem(
                    doc.getTitle(),
                    "/documents/" + doc.getId(),
                    "bi-file-earmark-text",
                    doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt()
            );
        }
    }
}
