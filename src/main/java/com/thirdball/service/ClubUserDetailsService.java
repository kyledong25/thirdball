package com.thirdball.service;

import com.thirdball.domain.ClubUser;
import com.thirdball.repository.ClubUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/** Loads database-backed accounts for Spring Security's HTTP Basic provider. */
@Service
public class ClubUserDetailsService implements UserDetailsService {
    private final ClubUserRepository clubUserRepository;

    public ClubUserDetailsService(ClubUserRepository clubUserRepository) {
        this.clubUserRepository = clubUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ClubUser user = clubUserRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Account was not found"));
        return new User(user.getEmail(), user.getPasswordHash(), Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
