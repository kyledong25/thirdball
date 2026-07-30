package com.thirdball.service;

import com.thirdball.api.request.CreateMatchRequest;
import com.thirdball.api.request.SubmitMatchResultRequest;
import com.thirdball.api.response.MatchResponse;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.domain.Tournament;
import com.thirdball.domain.TournamentStatus;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.MatchRepository;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
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
            if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
                throw new ConflictException("Tournament matches are managed by the generated bracket after registration closes");
            }
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
        if (match.getPlayerOne() == null || match.getPlayerTwo() == null) {
            throw new ConflictException("This bracket match is awaiting a player from an earlier round");
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

        advanceTournamentWinner(match, winner);

        return MatchResponse.from(match);
    }

    /**
     * Invalidates a completed result and restores the affected players to the
     * exact ratings they had before the result. To avoid corrupting a later
     * rating change, only the most recent completed result for both players can
     * be invalidated. The original score and rating snapshots remain stored on
     * the cancelled match as an audit record.
     */
    @Transactional
    public MatchResponse invalidateResult(Long matchId) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new NotFoundException("Match " + matchId + " was not found"));
        if (match.getStatus() != MatchStatus.COMPLETED || match.getCompletedAt() == null) {
            throw new ConflictException("Only a completed match result can be invalidated");
        }

        Long playerOneId = match.getPlayerOne().getId();
        Long playerTwoId = match.getPlayerTwo().getId();
        Player lowerIdPlayer = playerRepository.findByIdForUpdate(Math.min(playerOneId, playerTwoId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        Player higherIdPlayer = playerRepository.findByIdForUpdate(Math.max(playerOneId, playerTwoId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        Player playerOne = lowerIdPlayer.getId().equals(playerOneId) ? lowerIdPlayer : higherIdPlayer;
        Player playerTwo = lowerIdPlayer.getId().equals(playerTwoId) ? lowerIdPlayer : higherIdPlayer;

        if (matchRepository.existsCompletedMatchAfterForEitherPlayer(
                matchId, playerOneId, playerTwoId, match.getCompletedAt(), MatchStatus.COMPLETED)) {
            throw new ConflictException("This result cannot be invalidated because one of these players has a later completed match. "
                    + "Invalidate the newer result first or correct the ratings manually.");
        }

        withdrawAdvancedTournamentWinner(match);

        restoreRatingBeforeMatch(match, playerOne, true);
        restoreRatingBeforeMatch(match, playerTwo, false);
        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setStatus(MatchStatus.CANCELLED);
        return MatchResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> list() {
        Comparator<Match> newestFirst = Comparator
                .comparing(Match::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Match::getId, Comparator.reverseOrder());
        return matchRepository.findAll().stream()
                .sorted(newestFirst)
                .map(MatchResponse::from)
                .collect(Collectors.toList());
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

    private void advanceTournamentWinner(Match match, Player winner) {
        if (match.getTournament() == null) {
            return;
        }
        if (match.getNextMatch() == null) {
            Tournament tournament = tournamentRepository.findByIdForUpdate(match.getTournament().getId())
                    .orElseThrow(() -> new NotFoundException("Tournament " + match.getTournament().getId() + " was not found"));
            if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
                tournament.setStatus(TournamentStatus.COMPLETED);
            }
            return;
        }

        Match nextMatch = matchRepository.findByIdForUpdate(match.getNextMatch().getId())
                .orElseThrow(() -> new NotFoundException("The next bracket match was not found"));
        if (match.getNextMatchPlayerSlot() == 1) {
            if (nextMatch.getPlayerOne() != null && !nextMatch.getPlayerOne().getId().equals(winner.getId())) {
                throw new ConflictException("The next bracket match already has a different player in this slot");
            }
            nextMatch.setPlayerOne(winner);
        } else {
            if (nextMatch.getPlayerTwo() != null && !nextMatch.getPlayerTwo().getId().equals(winner.getId())) {
                throw new ConflictException("The next bracket match already has a different player in this slot");
            }
            nextMatch.setPlayerTwo(winner);
        }
    }

    private void withdrawAdvancedTournamentWinner(Match match) {
        if (match.getTournament() == null) {
            return;
        }
        if (match.getNextMatch() == null) {
            Tournament tournament = tournamentRepository.findByIdForUpdate(match.getTournament().getId())
                    .orElseThrow(() -> new NotFoundException("Tournament " + match.getTournament().getId() + " was not found"));
            if (tournament.getStatus() == TournamentStatus.COMPLETED) {
                tournament.setStatus(TournamentStatus.IN_PROGRESS);
            }
            return;
        }

        Match nextMatch = matchRepository.findByIdForUpdate(match.getNextMatch().getId())
                .orElseThrow(() -> new NotFoundException("The next bracket match was not found"));
        if (nextMatch.getStatus() != MatchStatus.SCHEDULED) {
            throw new ConflictException("This result cannot be invalidated after its next bracket match has progressed");
        }
        if (match.getNextMatchPlayerSlot() == 1) {
            if (nextMatch.getPlayerOne() != null && nextMatch.getPlayerOne().getId().equals(match.getWinner().getId())) {
                nextMatch.setPlayerOne(null);
            }
        } else if (nextMatch.getPlayerTwo() != null && nextMatch.getPlayerTwo().getId().equals(match.getWinner().getId())) {
            nextMatch.setPlayerTwo(null);
        }
    }

    private void restoreRatingBeforeMatch(Match match, Player player, boolean isPlayerOne) {
        Integer ratingBefore = isPlayerOne ? match.getPlayerOneRatingBefore() : match.getPlayerTwoRatingBefore();
        Integer ratingAfter = isPlayerOne ? match.getPlayerOneRatingAfter() : match.getPlayerTwoRatingAfter();
        if (ratingBefore != null) {
            if (ratingAfter == null || player.getRating() != ratingAfter) {
                throw new ConflictException("The player's rating has changed since this result. Correct the rating manually instead.");
            }
            player.setRating(ratingBefore);
            return;
        }

        rollbackProvisionalResult(player, ratingAfter);
    }

    private void rollbackProvisionalResult(Player player, Integer ratingAfter) {
        if (player.getProvisionalMatchCount() < 1) {
            throw new ConflictException("The provisional result history is no longer consistent for this player");
        }
        if (ratingAfter != null) {
            if (!player.isRatingEstablished() || player.getRating() != ratingAfter) {
                throw new ConflictException("The player's rating has changed since this provisional result. Correct the rating manually instead.");
            }
            player.setRating(Player.UNRATED_RATING);
            player.setRatingEstablished(false);
        } else if (player.isRatingEstablished()) {
            throw new ConflictException("The player has since received a rating. Correct the rating manually instead.");
        }
        player.setProvisionalMatchCount(player.getProvisionalMatchCount() - 1);
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
