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
    public static final int PROVISIONAL_MATCHES_REQUIRED = 5;

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

        if (!playerOne.isRatingEstablished() && !playerTwo.isRatingEstablished()) {
            throw new ConflictException("An unrated player must complete provisional matches against a rated player");
        }

        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setPlayerOneRatingBefore(playerOne.isRatingEstablished() ? playerOne.getRating() : null);
        match.setPlayerTwoRatingBefore(playerTwo.isRatingEstablished() ? playerTwo.getRating() : null);
        match.setPlayerOneScore(request.getPlayerOneScore());
        match.setPlayerTwoScore(request.getPlayerTwoScore());

        boolean playerOneWon = request.getPlayerOneScore() > request.getPlayerTwoScore();
        Player winner = playerOneWon ? playerOne : playerTwo;
        match.setWinner(winner);
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(Instant.now());

        if (playerOne.isRatingEstablished() && playerTwo.isRatingEstablished()) {
            applyEstablishedRatingExchange(match, playerOne, playerTwo, playerOneWon);
        } else {
            recordProvisionalResult(match, playerOne, playerTwo);
        }

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

    private void applyEstablishedRatingExchange(Match match, Player playerOne, Player playerTwo, boolean playerOneWon) {
        RatingCalculationService.RatingUpdate updatedRatings = ratingCalculationService.calculateRatingChange(
                playerOne.getRating(), playerTwo.getRating(), playerOneWon);
        playerOne.setRating(updatedRatings.getPlayer1Rating());
        playerTwo.setRating(updatedRatings.getPlayer2Rating());
        match.setPlayerOneRatingAfter(playerOne.getRating());
        match.setPlayerTwoRatingAfter(playerTwo.getRating());
    }

    /**
     * Stores an unrated player's result without changing the rated opponent's
     * rating. Once five results are available, the player receives a
     * provisional starting rating and future matches use the zero-sum chart.
     */
    private void recordProvisionalResult(Match match, Player playerOne, Player playerTwo) {
        Player provisionalPlayer = playerOne.isRatingEstablished() ? playerTwo : playerOne;
        provisionalPlayer.setProvisionalMatchCount(provisionalPlayer.getProvisionalMatchCount() + 1);

        match.setPlayerOneRatingAfter(playerOne.isRatingEstablished() ? playerOne.getRating() : null);
        match.setPlayerTwoRatingAfter(playerTwo.isRatingEstablished() ? playerTwo.getRating() : null);

        if (provisionalPlayer.getProvisionalMatchCount() < PROVISIONAL_MATCHES_REQUIRED) {
            return;
        }

        int provisionalRating = ratingCalculationService.initializeProvisionalRating(
                matchRepository.findCompletedForPlayer(provisionalPlayer.getId(), MatchStatus.COMPLETED).stream()
                        .map(completedMatch -> provisionalResultFor(completedMatch, provisionalPlayer))
                        .collect(Collectors.toList()));
        provisionalPlayer.setRating(provisionalRating);
        provisionalPlayer.setRatingEstablished(true);

        if (provisionalPlayer.getId().equals(playerOne.getId())) {
            match.setPlayerOneRatingAfter(provisionalRating);
        } else {
            match.setPlayerTwoRatingAfter(provisionalRating);
        }
    }

    private RatingCalculationService.ProvisionalMatchResult provisionalResultFor(Match completedMatch,
                                                                                  Player provisionalPlayer) {
        boolean isPlayerOne = completedMatch.getPlayerOne().getId().equals(provisionalPlayer.getId());
        Integer opponentRating = isPlayerOne
                ? completedMatch.getPlayerTwoRatingBefore()
                : completedMatch.getPlayerOneRatingBefore();
        if (opponentRating == null) {
            throw new ConflictException("Provisional results require a rated opponent");
        }

        boolean won = completedMatch.getWinner().getId().equals(provisionalPlayer.getId());
        return new RatingCalculationService.ProvisionalMatchResult(opponentRating, won);
    }
}
