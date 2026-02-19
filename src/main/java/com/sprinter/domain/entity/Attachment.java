package com.sprinter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Příloha pracovní položky.
 *
 * <p>Soubory jsou ukládány na disk do adresáře definovaného v konfiguraci
 * ({@code sprinter.uploads.dir}). V databázi se ukládá pouze metadata.</p>
 */
@Entity
@Table(name = "attachments",
       indexes = {
           @Index(name = "idx_attachments_work_item", columnList = "work_item_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"workItem", "uploadedBy"})
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attachments_seq")
    @SequenceGenerator(name = "attachments_seq", sequenceName = "attachments_id_seq", allocationSize = 1)
    private Long id;

    /** Originální název souboru (jak ho uživatel nahrál). */
    @NotBlank
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** Interní název souboru na disku (UUID-based pro unikátnost). */
    @NotBlank
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    /** MIME typ souboru. */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Velikost souboru v bajtech. */
    @Column(name = "file_size")
    private Long fileSize;

    /** Pracovní položka, ke které příloha patří. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private WorkItem workItem;

    /** Uživatel, který soubor nahrál. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Vrátí přibližnou lidsky čitelnou velikost souboru. */
    public String getHumanReadableSize() {
        if (fileSize == null) return "?";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024));
    }
}
