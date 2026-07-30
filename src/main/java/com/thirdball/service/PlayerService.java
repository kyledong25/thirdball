package com.thirdball.service;

import com.thirdball.api.request.CreatePlayerRequest;
import com.thirdball.api.request.UpdatePlayerRatingRequest;
import com.thirdball.api.response.PlayerResponse;
import com.thirdball.domain.Player;
import com.thirdball.repository.PlayerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) { this.playerRepository = playerRepository; }

    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        Player player = new Player();
        player.setDisplayName(request.getDisplayName().trim());
        player.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        try {
            return PlayerResponse.from(playerRepository.saveAndFlush(player));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A player with that email already exists");
        }
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> list() {
        return playerRepository.findAll().stream().map(PlayerResponse::from).collect(Collectors.toList());
    }

    /** Administrator override; a manually rated player becomes established. */
    @Transactional
    public PlayerResponse updateRating(Long playerId, UpdatePlayerRatingRequest request) {
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new com.thirdball.exception.NotFoundException("Player " + playerId + " was not found"));
        player.setRating(request.getRating());
        player.setRatingEstablished(true);
        return PlayerResponse.from(player);
    }
}
