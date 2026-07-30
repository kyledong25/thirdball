package com.thirdball.config;

import com.thirdball.domain.ClubRole;
import com.thirdball.domain.ClubUser;
import com.thirdball.repository.ClubUserRepository;
import com.thirdball.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.Instant;

/**
 * Creates the first administrator only when both bootstrap variables are set.
 * The password remains a deployment secret and is never stored in source code.
 */
@Configuration
public class AdminBootstrapConfiguration {
    @Bean
    public ApplicationRunner bootstrapAdministrator(
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword,
            ClubUserRepository clubUserRepository,
            PlayerRepository playerRepository,
            PasswordEncoder passwordEncoder) {
        return new ApplicationRunner() {
            @Override
            @Transactional
            public void run(ApplicationArguments arguments) {
                if (adminEmail.trim().isEmpty() || adminPassword.isEmpty()
                        || clubUserRepository.existsByRole(ClubRole.ADMIN)) {
                    return;
                }

                String email = adminEmail.trim().toLowerCase(Locale.ROOT);
                ClubUser administrator = clubUserRepository.findByEmail(email).orElseGet(ClubUser::new);
                administrator.setEmail(email);
                administrator.setPasswordHash(passwordEncoder.encode(adminPassword));
                administrator.setRole(ClubRole.ADMIN);
                administrator.setEmailVerified(true);
                administrator.setEmailVerifiedAt(Instant.now());
                playerRepository.findByEmail(email).ifPresent(administrator::setPlayer);
                clubUserRepository.save(administrator);
            }
        };
    }
}
