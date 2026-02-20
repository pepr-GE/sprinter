package com.sprinter.config;

import com.sprinter.security.CustomUserDetailsService;
import com.sprinter.security.LoginSuccessHandler;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * Konfigurace Spring Security.
 *
 * <p>PasswordEncoder je definován v {@link PasswordConfig}, aby se zabránilo
 * cirkulární závislosti přes LoginSuccessHandler → UserService → PasswordEncoder.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final DataSource               dataSource;
    private final LoginSuccessHandler      loginSuccessHandler;
    private final PasswordEncoder          passwordEncoder;

    @Value("${sprinter.security.remember-me-key:sprinter-remember-me}")
    private String rememberMeKey;

    @Value("${sprinter.security.remember-me-validity-seconds:2592000}")
    private int rememberMeValiditySeconds;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        var repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("SPRINTER_SESSION", "remember-me")
                .permitAll()
            )
            .rememberMe(rm -> rm
                .tokenRepository(persistentTokenRepository())
                .tokenValiditySeconds(rememberMeValiditySeconds)
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .userDetailsService(userDetailsService)
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
