package com.thirdball.service;

import com.thirdball.api.response.RatingHistoryPointResponse;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.repository.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberRatingHistoryServiceTest {

    @Test
    void returnsBaselineAndCompletedRatingChangesForTheAuthenticatedMember() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        MemberRatingHistoryService historyService = new MemberRatingHistoryService(matchRepository);
        Player member = player(1L, "Maya");
        Player opponent = player(2L, "Jordan");
        Match firstMatch = completedMatch(10L, member, opponent, 1500, 1508, 1480, 1472, true,
                "2026-07-01T18:00:00Z");
        Match secondMatch = completedMatch(11L, member, opponent, 1508, 1501, 1472, 1479, false,
                "2026-07-08T18:00:00Z");

        when(matchRepository.findCompletedForPlayer(1L, MatchStatus.COMPLETED))
                .thenReturn(Arrays.asList(firstMatch, secondMatch));

        List<RatingHistoryPointResponse> points = historyService.listFor(member);

        assertEquals(3, points.size());
        assertTrue(points.get(0).isBaseline());
        assertEquals(1500, points.get(0).getRating());
        assertEquals(1508, points.get(1).getRating());
        assertEquals("Jordan", points.get(1).getOpponentName());
        assertTrue(points.get(1).isWon());
        assertEquals(1501, points.get(2).getRating());
    }

    private Match completedMatch(Long id, Player playerOne, Player playerTwo,
                                 int playerOneBefore, int playerOneAfter,
                                 int playerTwoBefore, int playerTwoAfter,
                                 boolean playerOneWon, String completedAt) {
        Match match = new Match();
        ReflectionTestUtils.setField(match, "id", id);
        match.setPlayerOne(playerOne);
        match.setPlayerTwo(playerTwo);
        match.setWinner(playerOneWon ? playerOne : playerTwo);
        match.setPlayerOneRatingBefore(playerOneBefore);
        match.setPlayerOneRatingAfter(playerOneAfter);
        match.setPlayerTwoRatingBefore(playerTwoBefore);
        match.setPlayerTwoRatingAfter(playerTwoAfter);
        match.setCompletedAt(Instant.parse(completedAt));
        match.setStatus(MatchStatus.COMPLETED);
        return match;
    }

    private Player player(Long id, String displayName) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setDisplayName(displayName);
        return player;
    }
}
