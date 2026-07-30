package com.thirdball.api;

import com.thirdball.api.request.CreatePlayerRequest;
import com.thirdball.api.response.PlayerResponse;
import com.thirdball.service.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import com.thirdball.api.request.UpdatePlayerRatingRequest;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@PreAuthorize("hasRole('ADMIN')")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) { this.playerService = playerService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse create(@Valid @RequestBody CreatePlayerRequest request) {
        return playerService.create(request);
    }

    @GetMapping
    public List<PlayerResponse> list() { return playerService.list(); }

    @PutMapping("/{playerId}/rating")
    public PlayerResponse updateRating(@PathVariable Long playerId,
                                       @Valid @RequestBody UpdatePlayerRatingRequest request) {
        return playerService.updateRating(playerId, request);
    }
}
