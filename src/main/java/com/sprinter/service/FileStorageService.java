package com.sprinter.service;

import com.sprinter.domain.entity.Attachment;
import com.sprinter.domain.entity.WorkItem;
import com.sprinter.domain.repository.WorkItemRepository;
import com.sprinter.exception.ResourceNotFoundException;
import com.sprinter.exception.SprinterException;
import com.sprinter.exception.ValidationException;
import com.sprinter.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Servisní třída pro ukládání a správu souborových příloh.
 *
 * <p>Soubory jsou ukládány na disk v adresářové struktuře
 * {@code uploads/attachments/<workItemId>/}. V databázi se ukládá pouze metadata.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileStorageService {

    @Value("${sprinter.uploads.dir}")
    private String uploadsDir;

    @Value("${sprinter.uploads.max-file-size-mb:25}")
    private long maxFileSizeMb;

    private final WorkItemRepository workItemRepository;
    private final ProjectService     projectService;

    private Path rootPath;

    @PostConstruct
    public void init() {
        rootPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            log.info("Adresář pro nahrávání souborů: {}", rootPath);
        } catch (IOException e) {
            throw new SprinterException("Nelze vytvořit adresář pro nahrávání: " + rootPath, e);
        }
    }

    /**
     * Nahraje přílohu k pracovní položce.
     *
     * @param workItemId ID pracovní položky
     * @param file       nahrávaný soubor
     * @return metadata přílohy (uložené v DB)
     */
    public Attachment uploadAttachment(Long workItemId, MultipartFile file) {
        var workItem = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Pracovní položka", workItemId));

        projectService.requireContentEditAccess(workItem.getProject().getId());

        // Validace velikosti souboru
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new ValidationException(
                    "Soubor je příliš velký. Maximum je " + maxFileSizeMb + " MB.");
        }

        String originalName  = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";
        String extension     = getExtension(originalName);
        String storedName    = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // Adresář pro přílohy dané položky
        Path targetDir  = rootPath.resolve("attachments").resolve(workItemId.toString());
        Path targetPath = targetDir.resolve(storedName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SprinterException("Chyba při ukládání souboru: " + originalName, e);
        }

        var attachment = new Attachment();
        attachment.setWorkItem(workItem);
        attachment.setOriginalFilename(originalName);
        attachment.setStoredFilename(storedName);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setUploadedBy(SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new SprinterException("Není přihlášen žádný uživatel.", HttpStatus.UNAUTHORIZED)));

        workItem.getAttachments().add(attachment);
        workItemRepository.save(workItem);

        log.info("Nahrána příloha '{}' k položce ID={}", originalName, workItemId);
        return attachment;
    }

    /**
     * Vrátí cestu k souboru na disku.
     */
    public Path getAttachmentPath(Long workItemId, String storedFilename) {
        return rootPath.resolve("attachments")
                       .resolve(workItemId.toString())
                       .resolve(storedFilename)
                       .normalize();
    }

    // ---- Pomocné metody ----

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
