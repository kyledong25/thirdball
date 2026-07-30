package com.thirdball.service;

import com.thirdball.api.response.LadderPlayerResponse;
import com.thirdball.domain.Player;
import com.thirdball.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberLadderServiceTest {

    @Test
    void listsOnlyActivePlayersInPublishedLadderOrder() {
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        MemberLadderService service = new MemberLadderService(playerRepository);
        Player unrated = player(1L, "Alex", 0, false, 2);
        Player lowerRated = player(2L, "Casey", 1450, true, 0);
        Player higherRated = player(3L, "Blake", 1625, true, 0);

        when(playerRepository.findByActiveTrue()).thenReturn(Arrays.asList(unrated, lowerRated, higherRated));

        List<LadderPlayerResponse> ladder = service.list();

        assertEquals(Arrays.asList("Blake", "Casey", "Alex"), Arrays.asList(
                ladder.get(0).getDisplayName(), ladder.get(1).getDisplayName(), ladder.get(2).getDisplayName()));
        assertEquals(1625, ladder.get(0).getRating());
        assertEquals(2, ladder.get(2).getProvisionalMatchCount());
    }

    private Player player(Long id, String displayName, int rating, boolean established, int provisionalMatchCount) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setDisplayName(displayName);
        player.setRating(rating);
        player.setRatingEstablished(established);
        player.setProvisionalMatchCount(provisionalMatchCount);
        return player;
    }
}
