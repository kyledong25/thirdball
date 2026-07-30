package com.thirdball.service;

import com.thirdball.api.request.RegisterMemberRequest;
import com.thirdball.api.request.VerifyEmailRequest;
import com.thirdball.api.response.EmailVerificationPendingResponse;
import com.thirdball.domain.ClubRole;
import com.thirdball.domain.ClubUser;
import com.thirdball.domain.Player;
import com.thirdball.repository.ClubUserRepository;
import com.thirdball.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Test
    void registrationCreatesAnUnverifiedMemberAndSendsACode() {
        ClubUserRepository userRepository = mock(ClubUserRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        AuthenticationService service = new AuthenticationService(userRepository, playerRepository,
                new BCryptPasswordEncoder(), emailVerificationService);
        Player player = player(7L);
        when(userRepository.findByEmail("member@tamu.edu")).thenReturn(Optional.empty());
        when(playerRepository.findByEmail("member@tamu.edu")).thenReturn(Optional.of(player));
        when(userRepository.existsByPlayer_Id(7L)).thenReturn(false);
        when(userRepository.save(any(ClubUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationPendingResponse response = service.registerMember(registerRequest());

        assertEquals("member@tamu.edu", response.getEmail());
        ArgumentCaptor<ClubUser> userCaptor = ArgumentCaptor.forClass(ClubUser.class);
        verify(emailVerificationService).issueCode(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
    }

    @Test
    void verificationActivatesTheAccountAndClearsTheOneTimeCode() {
        ClubUserRepository userRepository = mock(ClubUserRepository.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        ClubUser user = new ClubUser();
        user.setEmail("member@tamu.edu");
        user.setPasswordHash("hash");
        user.setRole(ClubRole.MEMBER);
        user.setEmailVerified(false);
        user.setEmailVerificationCodeHash("one-time-code-hash");
        when(userRepository.findByEmail("member@tamu.edu")).thenReturn(Optional.of(user));
        when(emailVerificationService.matchesActiveCode(user, "123456")).thenReturn(true);
        AuthenticationService service = new AuthenticationService(userRepository, mock(PlayerRepository.class),
                new BCryptPasswordEncoder(), emailVerificationService);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("member@tamu.edu");
        request.setCode("123456");

        service.verifyEmail(request);

        assertTrue(user.isEmailVerified());
        assertNull(user.getEmailVerificationCodeHash());
        assertNotNullVerifiedAt(user);
    }

    private RegisterMemberRequest registerRequest() {
        RegisterMemberRequest request = new RegisterMemberRequest();
        request.setDisplayName("Member Name");
        request.setEmail("MEMBER@TAMU.EDU");
        request.setPassword("club-password");
        return request;
    }

    private Player player(Long id) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setDisplayName("Member Name");
        player.setEmail("member@tamu.edu");
        return player;
    }

    private void assertNotNullVerifiedAt(ClubUser user) {
        assertTrue(user.getEmailVerifiedAt() != null);
    }
}
