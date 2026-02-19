package com.sprinter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.sprinter.security.SecurityUtils;
import java.util.Optional;

/**
 * Obecná konfigurace aplikace – JPA Auditing, beany apod.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AppConfig {

    /**
     * Poskytuje JPA Auditing přihlašovací jméno aktuálního uživatele.
     * Používá se pro automatické vyplňování polí @CreatedBy / @LastModifiedBy.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> SecurityUtils.getCurrentUserDetails()
                .map(ud -> ud.getUsername())
                .or(() -> Optional.of("system"));
    }
}
