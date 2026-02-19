package com.sprinter.config;

import com.sprinter.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * Konfigurace Spring Security.
 *
 * <p>Používá form-based login (session cookies), takže funguje přirozeně
 * s Thymeleaf šablonami bez potřeby JWT. Remember-me tokeny jsou uloženy
 * v databázi (persistent token repository) pro bezpečnost.</p>
 *
 * <p>Oprávnění (authorization) na úrovni metod servisní vrstvy jsou
 * povolena pomocí {@code @EnableMethodSecurity}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // povolí @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final DataSource dataSource;

    @Value("${sprinter.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Value("${sprinter.security.remember-me-key:sprinter-remember-me}")
    private String rememberMeKey;

    @Value("${sprinter.security.remember-me-validity-seconds:2592000}")
    private int rememberMeValiditySeconds;

    /**
     * BCrypt encoder pro hashování hesel.
     * Síla 12 je vhodný kompromis mezi bezpečností a výkonem.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * Authentication provider – ověřuje uživatele v databázi.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Authentication manager – vstupní bod pro autentizaci.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    /**
     * Perzistentní repozitář pro Remember-Me tokeny.
     * Tokeny jsou uloženy v tabulce {@code persistent_logins}.
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        var repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    /**
     * Hlavní bezpečnostní filtr řetězce.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ---- Autorizace požadavků ----
            .authorizeHttpRequests(auth -> auth
                // Veřejně dostupné zdroje
                .requestMatchers("/login", "/login/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Admin sekce pouze pro roli ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Vše ostatní vyžaduje přihlášení
                .anyRequest().authenticated()
            )

            // ---- Přihlašovací formulář ----
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )

            // ---- Odhlášení ----
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("SPRINTER_SESSION", "remember-me")
                .permitAll()
            )

            // ---- Remember Me (perzistentní přihlášení) ----
            .rememberMe(rm -> rm
                .tokenRepository(persistentTokenRepository())
                .tokenValiditySeconds(rememberMeValiditySeconds)
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .userDetailsService(userDetailsService)
            )

            // ---- CSRF ----
            // CSRF je zapnuta (výchozí). Pro AJAX požadavky z HTMX se token předává
            // v hlavičce X-CSRF-TOKEN, která je nastavena v base šabloně.
            .csrf(AbstractHttpConfigurer::disable); // TODO: pro produkci zapnout CSRF

        return http.build();
    }
}
