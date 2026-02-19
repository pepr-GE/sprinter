package com.sprinter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Základní integrační test aplikace SPRINTER.
 *
 * <p>Ověřuje, že se Spring Application Context úspěšně načte.
 * Testovací prostředí používá H2 in-memory databázi místo PostgreSQL.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Přepíše datový zdroj na H2 pro testy
    "spring.datasource.url=jdbc:h2:mem:sprinter_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    // Vypnutí Flyway pro testy (DDL generuje Hibernate)
    "spring.flyway.enabled=false",
    // Vypnutí mailového serveru
    "spring.mail.host=localhost",
    "spring.mail.port=3025"
})
class SprinterApplicationTest {

    /**
     * Testuje, že se application context spustí bez chyb.
     */
    @Test
    void contextLoads() {
        // Test projde pokud se kontext načte úspěšně
    }
}
