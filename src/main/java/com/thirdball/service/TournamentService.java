package com.thirdball.service;

import com.thirdball.api.request.CreateTournamentRequest;
import com.thirdball.api.response.TournamentResponse;
import com.thirdball.domain.Player;
import com.thirdball.domain.Tournament;
import com.thirdball.domain.TournamentStatus;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;

    public TournamentService(TournamentRepository tournamentRepository, PlayerRepository playerRepository) {
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public TournamentResponse create(CreateTournamentRequest request) {
        validateSchedule(request.getStartsAt(), request.getEndsAt());
        Tournament tournament = new Tournament();
        tournament.setName(request.getName().trim());
        tournament.setDescription(request.getDescription());
        tournament.setLocation(request.getLocation());
        tournament.setStartsAt(request.getStartsAt());
        tournament.setEndsAt(request.getEndsAt());
        tournament.setMaxParticipants(request.getMaxParticipants());
        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        return TournamentResponse.from(tournamentRepository.save(tournament));
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> list() {
        return tournamentRepository.findAll().stream()
                .map(TournamentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> listUpcoming() {
        Instant now = Instant.now();
        return tournamentRepository.findAll().stream()
                .filter(tournament -> tournament.getEndsAt().isAfter(now))
                .sorted((first, second) -> first.getStartsAt().compareTo(second.getStartsAt()))
                .map(TournamentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TournamentResponse registerPlayer(Long tournamentId, Long playerId) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament " + tournamentId + " was not found"));
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new ConflictException("Tournament registration is not open");
        }

        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new NotFoundException("Player " + playerId + " was not found"));
        if (!player.isActive()) {
            throw new ConflictException("Inactive players cannot register for tournaments");
        }
        if (tournament.getParticipants().contains(player)) {
            throw new ConflictException("Player is already registered for this tournament");
        }
        if (tournament.getParticipants().size() >= tournament.getMaxParticipants()) {
            throw new ConflictException("Tournament registration is full");
        }

        tournament.getParticipants().add(player);
        return TournamentResponse.from(tournament);
    }

    private void validateSchedule(java.time.Instant startsAt, java.time.Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Tournament end time must be after its start time");
        }
    }
}
