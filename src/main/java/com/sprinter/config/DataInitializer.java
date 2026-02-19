package com.sprinter.config;

import com.sprinter.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializátor dat při startu aplikace.
 *
 * <p>Zajišťuje, aby systém měl vždy alespoň jednoho správce.
 * Flyway migrace V2 by měla vytvořit výchozího admina,
 * ale tato třída slouží jako záložní mechanismus.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("SPRINTER startuje – kontrola výchozích dat...");
        userService.ensureDefaultAdminExists();
        log.info("SPRINTER je připraven.");
    }
}
