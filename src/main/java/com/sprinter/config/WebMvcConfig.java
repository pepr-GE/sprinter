package com.sprinter.config;

import com.sprinter.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Konfigurace Spring MVC.
 *
 * <p>Registruje obslužné routery pro statické soubory, včetně uživatelsky
 * nahraných souborů (přílohy, avatary) z externího adresáře.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${sprinter.uploads.dir}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Statické zdroje aplikace (CSS, JS, obrázky)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(86400);   // 1 den cache v prohlížeči

        // Uživatelsky nahrané soubory (přílohy, avatary)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsDir + "/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
}
