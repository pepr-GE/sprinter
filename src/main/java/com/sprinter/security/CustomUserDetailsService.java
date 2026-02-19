package com.sprinter.security;

import com.sprinter.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Načítá uživatelské detaily pro Spring Security z databáze.
 *
 * <p>Umožňuje přihlášení jak přes username, tak přes e-mail.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Načte uživatele dle přihlašovacího jména nebo e-mailu.
     * Pokud uživatel neexistuje nebo je neaktivní, vyhodí výjimku.
     *
     * @param usernameOrEmail přihlašovací jméno nebo e-mail
     * @throws UsernameNotFoundException pokud uživatel nebyl nalezen
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        var user = userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Uživatel nenalezen: " + usernameOrEmail));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("Účet je deaktivován: " + usernameOrEmail);
        }

        return new SprinterUserDetails(user);
    }
}
