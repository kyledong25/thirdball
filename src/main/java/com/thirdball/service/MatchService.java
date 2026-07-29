package com.thirdball.service;

import com.thirdball.api.request.CreateMatchRequest;
import com.thirdball.api.request.SubmitMatchResultRequest;
import com.thirdball.api.response.MatchResponse;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.domain.Tournament;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.MatchRepository;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final RatingCalculationService ratingCalculationService;

    public MatchService(MatchRepository matchRepository, PlayerRepository playerRepository,
                        TournamentRepository tournamentRepository, RatingCalculationService ratingCalculationService) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.ratingCalculationService = ratingCalculationService;
    }

    @Transactional
    public MatchResponse schedule(CreateMatchRequest request) {
        if (request.getPlayerOneId().equals(request.getPlayerTwoId())) {
            throw new IllegalArgumentException("A match requires two distinct players");
        }
        Player playerOne = findPlayer(request.getPlayerOneId());
        Player playerTwo = findPlayer(request.getPlayerTwoId());
        if (!playerOne.isActive() || !playerTwo.isActive()) {
            throw new ConflictException("Inactive players cannot be scheduled for a match");
        }

        Match match = new Match();
        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setRoundNumber(request.getRoundNumber());
        match.setBracketSlot(request.getBracketSlot());

        if (request.getTournamentId() != null) {
            Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                    .orElseThrow(() -> new NotFoundException("Tournament " + request.getTournamentId() + " was not found"));
            if (!tournament.getParticipants().contains(playerOne) || !tournament.getParticipants().contains(playerTwo)) {
                throw new ConflictException("Tournament matches require both players to be registered");
            }
            match.setTournament(tournament);
        }

        return MatchResponse.from(matchRepository.save(match));
    }

    @Transactional
    public MatchResponse recordResult(Long matchId, SubmitMatchResultRequest request) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new NotFoundException("Match " + matchId + " was not found"));
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new ConflictException("A result can only be submitted once for a scheduled match");
        }
        if (request.getPlayerOneScore().equals(request.getPlayerTwoScore())) {
            throw new IllegalArgumentException("Table tennis matches cannot end in a tie");
        }

        // Acquire player row locks in a stable order to avoid lost rating updates and lock-order deadlocks.
        Long firstId = match.getPlayerOne().getId();
        Long secondId = match.getPlayerTwo().getId();
        Player lowerIdPlayer = playerRepository.findByIdForUpdate(Math.min(firstId, secondId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        Player higherIdPlayer = playerRepository.findByIdForUpdate(Math.max(firstId, secondId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        Player playerOne = lowerIdPlayer.getId().equals(firstId) ? lowerIdPlayer : higherIdPlayer;
        Player playerTwo = lowerIdPlayer.getId().equals(secondId) ? lowerIdPlayer : higherIdPlayer;

        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setPlayerOneRatingBefore(playerOne.getRating());
        match.setPlayerTwoRatingBefore(playerTwo.getRating());
        match.setPlayerOneScore(request.getPlayerOneScore());
        match.setPlayerTwoScore(request.getPlayerTwoScore());

        boolean playerOneWon = request.getPlayerOneScore() > request.getPlayerTwoScore();
        Player winner = playerOneWon ? playerOne : playerTwo;
        RatingCalculationService.RatingUpdate updatedRatings = ratingCalculationService.calculateRatingChange(
                playerOne.getRating(), playerTwo.getRating(), playerOneWon);
        playerOne.setRating(updatedRatings.getPlayer1Rating());
        playerTwo.setRating(updatedRatings.getPlayer2Rating());

        match.setWinner(winner);
        match.setPlayerOneRatingAfter(playerOne.getRating());
        match.setPlayerTwoRatingAfter(playerTwo.getRating());
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(Instant.now());

        return MatchResponse.from(match);
    }

    @Transactional(readOnly = true)
    public MatchResponse get(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match " + matchId + " was not found"));
        return MatchResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listTournamentMatches(Long tournamentId) {
        return matchRepository.findByTournament_IdOrderByRoundNumberAscBracketSlotAsc(tournamentId).stream()
                .map(MatchResponse::from)
                .collect(Collectors.toList());
    }

    private Player findPlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player " + playerId + " was not found"));
    }
}
