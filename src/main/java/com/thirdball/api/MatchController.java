package com.thirdball.api;

import com.thirdball.api.request.CreateMatchRequest;
import com.thirdball.api.request.SubmitMatchResultRequest;
import com.thirdball.api.response.MatchResponse;
import com.thirdball.service.MatchService;
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

@RestController
@RequestMapping("/api/matches")
@PreAuthorize("hasRole('ADMIN')")
public class MatchController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) { this.matchService = matchService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse schedule(@Valid @RequestBody CreateMatchRequest request) {
        return matchService.schedule(request);
    }

    @GetMapping("/{matchId}")
    public MatchResponse get(@PathVariable Long matchId) { return matchService.get(matchId); }

    @PostMapping("/{matchId}/result")
    public MatchResponse recordResult(@PathVariable Long matchId,
                                      @Valid @RequestBody SubmitMatchResultRequest request) {
        return matchService.recordResult(matchId, request);
    }
}
