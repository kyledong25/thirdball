package com.thirdball.service;

import com.thirdball.api.request.RegisterMemberRequest;
import com.thirdball.api.request.UpdateMemberProfileRequest;
import com.thirdball.api.request.VerificationEmailRequest;
import com.thirdball.api.request.VerifyEmailRequest;
import com.thirdball.api.response.AuthenticatedUserResponse;
import com.thirdball.api.response.EmailVerificationPendingResponse;
import com.thirdball.api.response.PlayerResponse;
import com.thirdball.domain.ClubRole;
import com.thirdball.domain.ClubUser;
import com.thirdball.domain.Player;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.ClubUserRepository;
import com.thirdball.repository.PlayerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.Instant;

/** Creates member accounts and resolves the authenticated account's player. */
@Service
public class AuthenticationService {
    private final ClubUserRepository clubUserRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthenticationService(ClubUserRepository clubUserRepository, PlayerRepository playerRepository,
                                 PasswordEncoder passwordEncoder, EmailVerificationService emailVerificationService) {
        this.clubUserRepository = clubUserRepository;
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public EmailVerificationPendingResponse registerMember(RegisterMemberRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (clubUserRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("An account with that email already exists");
        }

        Player player = playerRepository.findByEmail(email).orElseGet(() -> {
            Player newPlayer = new Player();
            newPlayer.setDisplayName(request.getDisplayName().trim());
            newPlayer.setEmail(email);
            return playerRepository.save(newPlayer);
        });
        if (clubUserRepository.existsByPlayer_Id(player.getId())) {
            throw new ConflictException("That player record is already linked to an account");
        }

        ClubUser user = new ClubUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(ClubRole.MEMBER);
        user.setPlayer(player);
        user.setEmailVerified(false);
        ClubUser savedUser = clubUserRepository.save(user);
        emailVerificationService.issueCode(savedUser);
        return EmailVerificationPendingResponse.from(savedUser);
    }

    @Transactional
    public AuthenticatedUserResponse verifyEmail(VerifyEmailRequest request) {
        ClubUser user = clubUserRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email verification code"));
        if (user.isEmailVerified()) {
            return AuthenticatedUserResponse.from(user);
        }
        if (!emailVerificationService.matchesActiveCode(user, request.getCode())) {
            throw new IllegalArgumentException("That verification code is invalid or has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        user.setEmailVerificationCodeHash(null);
        user.setEmailVerificationCodeExpiresAt(null);
        user.setEmailVerificationCodeSentAt(null);
        return AuthenticatedUserResponse.from(user);
    }

    @Transactional
    public void resendVerificationCode(VerificationEmailRequest request) {
        clubUserRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(emailVerificationService::issueCodeWhenAllowed);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser(Authentication authentication) {
        return AuthenticatedUserResponse.from(findByEmail(authentication.getName()));
    }

    @Transactional(readOnly = true)
    public Player currentMemberPlayer(Authentication authentication) {
        ClubUser user = findByEmail(authentication.getName());
        if (user.getRole() != ClubRole.MEMBER || user.getPlayer() == null) {
            throw new NotFoundException("This member account is not linked to a player record");
        }
        return user.getPlayer();
    }

    @Transactional
    public PlayerResponse updateMemberProfile(Authentication authentication, UpdateMemberProfileRequest request) {
        Player currentPlayer = currentMemberPlayer(authentication);
        Player player = playerRepository.findByIdForUpdate(currentPlayer.getId())
                .orElseThrow(() -> new NotFoundException("Your member profile was not found"));
        player.setGraduationYear(request.getGraduationYear());
        player.setSkillLevel(trimToNull(request.getSkillLevel()));
        player.setPhone(trimToNull(request.getPhone()));
        return PlayerResponse.from(player);
    }

    private ClubUser findByEmail(String email) {
        return clubUserRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("Authenticated account was not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
