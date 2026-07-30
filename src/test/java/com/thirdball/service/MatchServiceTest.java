package com.thirdball.service;

import com.thirdball.api.response.MatchResponse;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.exception.ConflictException;
import com.thirdball.repository.MatchRepository;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchServiceTest {
    private MatchRepository matchRepository;
    private PlayerRepository playerRepository;
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        playerRepository = mock(PlayerRepository.class);
        matchService = new MatchService(matchRepository, playerRepository,
                mock(TournamentRepository.class), new RatingCalculationService());
    }

    @Test
    void invalidatesTheLatestEstablishedResultAndRestoresBothRatings() {
        Player playerOne = establishedPlayer(1L, 1210);
        Player playerTwo = establishedPlayer(2L, 1190);
        Match match = completedMatch(playerOne, playerTwo);
        Instant completedAt = match.getCompletedAt();

        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(match));
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(playerOne));
        when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(playerTwo));
        when(matchRepository.existsCompletedMatchAfterForEitherPlayer(
                100L, 1L, 2L, completedAt, MatchStatus.COMPLETED)).thenReturn(false);

        MatchResponse response = matchService.invalidateResult(100L);

        assertEquals(MatchStatus.CANCELLED, response.getStatus());
        assertEquals(1200, playerOne.getRating());
        assertEquals(1200, playerTwo.getRating());
        assertEquals(MatchStatus.CANCELLED, match.getStatus());
    }

    @Test
    void doesNotInvalidateAResultWhenAPlayerHasALaterCompletedMatch() {
        Player playerOne = establishedPlayer(1L, 1210);
        Player playerTwo = establishedPlayer(2L, 1190);
        Match match = completedMatch(playerOne, playerTwo);
        Instant completedAt = match.getCompletedAt();

        when(matchRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(match));
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(playerOne));
        when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(playerTwo));
        when(matchRepository.existsCompletedMatchAfterForEitherPlayer(
                100L, 1L, 2L, completedAt, MatchStatus.COMPLETED)).thenReturn(true);

        assertThrows(ConflictException.class, () -> matchService.invalidateResult(100L));

        assertEquals(MatchStatus.COMPLETED, match.getStatus());
        assertEquals(1210, playerOne.getRating());
        assertEquals(1190, playerTwo.getRating());
    }

    private Match completedMatch(Player playerOne, Player playerTwo) {
        Match match = new Match();
        ReflectionTestUtils.setField(match, "id", 100L);
        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setWinner(playerOne);
        match.setPlayerOneScore(3);
        match.setPlayerTwoScore(1);
        match.setPlayerOneRatingBefore(1200);
        match.setPlayerOneRatingAfter(1210);
        match.setPlayerTwoRatingBefore(1200);
        match.setPlayerTwoRatingAfter(1190);
        match.setCompletedAt(Instant.parse("2026-07-29T20:00:00Z"));
        match.setStatus(MatchStatus.COMPLETED);
        return match;
    }

    private Player establishedPlayer(Long id, int rating) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setRating(rating);
        player.setRatingEstablished(true);
        return player;
    }
}
