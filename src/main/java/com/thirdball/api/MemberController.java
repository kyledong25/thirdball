package com.thirdball.api;

import com.thirdball.api.response.PracticeSessionResponse;
import com.thirdball.api.response.TournamentResponse;
import com.thirdball.domain.Player;
import com.thirdball.service.AuthenticationService;
import com.thirdball.service.PracticeSessionService;
import com.thirdball.service.TournamentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** General-member schedule and self-service event registration routes. */
@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberController {
    private final AuthenticationService authenticationService;
    private final PracticeSessionService practiceSessionService;
    private final TournamentService tournamentService;

    public MemberController(AuthenticationService authenticationService,
                            PracticeSessionService practiceSessionService,
                            TournamentService tournamentService) {
        this.authenticationService = authenticationService;
        this.practiceSessionService = practiceSessionService;
        this.tournamentService = tournamentService;
    }

    @GetMapping("/practice-sessions")
    public List<PracticeSessionResponse> upcomingPracticeSessions() {
        return practiceSessionService.listUpcoming();
    }

    @GetMapping("/tournaments")
    public List<TournamentResponse> upcomingTournaments() {
        return tournamentService.listUpcoming();
    }

    @PostMapping("/practice-sessions/{sessionId}/signup")
    public PracticeSessionResponse signUpForPractice(@PathVariable Long sessionId, Authentication authentication) {
        Player player = authenticationService.currentMemberPlayer(authentication);
        return practiceSessionService.registerPlayer(sessionId, player.getId());
    }

    @PostMapping("/tournaments/{tournamentId}/signup")
    public TournamentResponse signUpForTournament(@PathVariable Long tournamentId, Authentication authentication) {
        Player player = authenticationService.currentMemberPlayer(authentication);
        return tournamentService.registerPlayer(tournamentId, player.getId());
    }
}
