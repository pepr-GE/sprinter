package com.sprinter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Konfigurace pro BCrypt password encoder.
 * Oddělen od SecurityConfig, aby se zabránilo cirkulární závislosti
 * SecurityConfig → LoginSuccessHandler → UserService → PasswordEncoder.
 */
@Configuration
public class PasswordConfig {

    @Value("${sprinter.security.bcrypt-strength:12}")
    private int bcryptStrength;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }
}
