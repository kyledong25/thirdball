package com.thirdball.service;

import com.thirdball.api.request.RegisterMemberRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    @Test
    void registrationCreatesAMemberThatCanSignInImmediately() {
        ClubUserRepository userRepository = mock(ClubUserRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        AuthenticationService service = new AuthenticationService(userRepository, playerRepository,
                new BCryptPasswordEncoder());
        Player player = player(7L);
        when(userRepository.findByEmail("member@tamu.edu")).thenReturn(Optional.empty());
        when(playerRepository.findByEmail("member@tamu.edu")).thenReturn(Optional.of(player));
        when(userRepository.existsByPlayer_Id(7L)).thenReturn(false);
        when(userRepository.save(any(ClubUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerMember(registerRequest());

        ArgumentCaptor<ClubUser> userCaptor = ArgumentCaptor.forClass(ClubUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("member@tamu.edu", userCaptor.getValue().getEmail());
        assertEquals(ClubRole.MEMBER, userCaptor.getValue().getRole());
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
}
