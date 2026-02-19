package com.sprinter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hlavní třída aplikace SPRINTER.
 *
 * <p>Aplikace lze spustit dvěma způsoby:
 * <ul>
 *   <li>Jako samostatný JAR/WAR s embedded Tomcatem: {@code java -jar sprinter.war}</li>
 *   <li>Jako WAR nasazený na externím Tomcatu – {@link SpringBootServletInitializer}
 *       zajišťuje správnou inicializaci v tomto režimu.</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling   // pro plánované úlohy (např. připomínky termínů)
public class SprinterApplication extends SpringBootServletInitializer {

    /**
     * Vstupní bod pro spuštění jako standalone aplikace.
     */
    public static void main(String[] args) {
        SpringApplication.run(SprinterApplication.class, args);
    }

    /**
     * Konfigurace pro nasazení do externího Tomcatu.
     * Tato metoda je volána servletem při deployi WAR souboru.
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SprinterApplication.class);
    }
}
