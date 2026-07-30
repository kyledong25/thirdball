package com.thirdball.api;

import com.thirdball.api.request.CreateTournamentRequest;
import com.thirdball.api.request.PlayerRegistrationRequest;
import com.thirdball.api.response.MatchResponse;
import com.thirdball.api.response.TournamentResponse;
import com.thirdball.service.MatchService;
import com.thirdball.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@PreAuthorize("hasRole('ADMIN')")
public class TournamentController {
    private final TournamentService tournamentService;
    private final MatchService matchService;

    public TournamentController(TournamentService tournamentService, MatchService matchService) {
        this.tournamentService = tournamentService;
        this.matchService = matchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponse create(@Valid @RequestBody CreateTournamentRequest request) {
        return tournamentService.create(request);
    }

    @GetMapping
    public List<TournamentResponse> list() { return tournamentService.list(); }

    @PostMapping("/{tournamentId}/registrations")
    public TournamentResponse register(@PathVariable Long tournamentId,
                                       @Valid @RequestBody PlayerRegistrationRequest request) {
        return tournamentService.registerPlayer(tournamentId, request.getPlayerId());
    }

    @GetMapping("/{tournamentId}/matches")
    public List<MatchResponse> bracketMatches(@PathVariable Long tournamentId) {
        return matchService.listTournamentMatches(tournamentId);
    }
}
