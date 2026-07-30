package com.thirdball.service;

import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.domain.Tournament;
import com.thirdball.domain.TournamentStatus;
import com.thirdball.repository.MatchRepository;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TournamentServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void generatesARatingSeededTreeAndAdvancesOpeningByes() {
        TournamentRepository tournamentRepository = mock(TournamentRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        TournamentService tournamentService = new TournamentService(
                tournamentRepository, mock(PlayerRepository.class), matchRepository);
        Tournament tournament = tournamentWithFiveRatedPlayers();

        when(tournamentRepository.findByIdForUpdate(44L)).thenReturn(Optional.of(tournament));
        when(matchRepository.existsByTournament_Id(44L)).thenReturn(false);

        tournamentService.generateBracket(44L);

        ArgumentCaptor<Iterable<Match>> matchesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(matchRepository).saveAll(matchesCaptor.capture());
        List<Match> matches = new ArrayList<>();
        matchesCaptor.getValue().forEach(matches::add);

        assertEquals(7, matches.size());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());

        Match firstMatch = matches.get(0);
        assertEquals("Seed 1", firstMatch.getPlayerOne().getDisplayName());
        assertEquals(MatchStatus.BYE, firstMatch.getStatus());
        assertEquals("Seed 1", firstMatch.getWinner().getDisplayName());
        assertEquals(1, firstMatch.getRoundNumber());
        assertEquals("Seed 1", firstMatch.getNextMatch().getPlayerOne().getDisplayName());

        Match secondMatch = matches.get(1);
        assertEquals("Seed 4", secondMatch.getPlayerOne().getDisplayName());
        assertEquals("Seed 5", secondMatch.getPlayerTwo().getDisplayName());
        assertEquals(MatchStatus.SCHEDULED, secondMatch.getStatus());

        Match thirdMatch = matches.get(2);
        assertEquals("Seed 2", thirdMatch.getPlayerOne().getDisplayName());
        assertEquals(MatchStatus.BYE, thirdMatch.getStatus());
        Match fourthMatch = matches.get(3);
        assertEquals("Seed 3", fourthMatch.getPlayerOne().getDisplayName());
        assertEquals(MatchStatus.BYE, fourthMatch.getStatus());
    }

    private Tournament tournamentWithFiveRatedPlayers() {
        Tournament tournament = new Tournament();
        ReflectionTestUtils.setField(tournament, "id", 44L);
        tournament.setName("Aggie Open");
        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        tournament.getParticipants().add(ratedPlayer(1L, "Seed 1", 1900));
        tournament.getParticipants().add(ratedPlayer(2L, "Seed 2", 1800));
        tournament.getParticipants().add(ratedPlayer(3L, "Seed 3", 1700));
        tournament.getParticipants().add(ratedPlayer(4L, "Seed 4", 1600));
        tournament.getParticipants().add(ratedPlayer(5L, "Seed 5", 1500));
        return tournament;
    }

    private Player ratedPlayer(Long id, String name, int rating) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setDisplayName(name);
        player.setRating(rating);
        player.setRatingEstablished(true);
        return player;
    }
}
