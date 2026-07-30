package com.thirdball.service;

import com.thirdball.api.request.ProposeMemberMatchResultRequest;
import com.thirdball.api.response.MemberMatchResultProposalResponse;
import com.thirdball.domain.ClubRole;
import com.thirdball.domain.Match;
import com.thirdball.domain.MemberMatchResultProposal;
import com.thirdball.domain.MemberMatchResultStatus;
import com.thirdball.domain.Player;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.ClubUserRepository;
import com.thirdball.repository.MemberMatchResultProposalRepository;
import com.thirdball.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberMatchResultServiceTest {

    @Test
    void createsPendingResultRequestForAnotherActiveMember() {
        MemberMatchResultProposalRepository proposalRepository = mock(MemberMatchResultProposalRepository.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        ClubUserRepository clubUserRepository = mock(ClubUserRepository.class);
        MemberMatchResultService service = new MemberMatchResultService(proposalRepository, playerRepository,
                clubUserRepository, mock(MatchService.class));
        Player reporter = player(1L, "Maya");
        Player opponent = player(2L, "Jordan");
        ProposeMemberMatchResultRequest request = request(2L, 3, 1);

        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reporter));
        when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(opponent));
        when(clubUserRepository.existsByPlayer_IdAndRole(2L, ClubRole.MEMBER)).thenReturn(true);
        when(proposalRepository.save(any(MemberMatchResultProposal.class))).thenAnswer(invocation -> {
            MemberMatchResultProposal proposal = invocation.getArgument(0);
            ReflectionTestUtils.setField(proposal, "id", 10L);
            ReflectionTestUtils.setField(proposal, "proposedAt", java.time.Instant.parse("2026-07-30T12:00:00Z"));
            return proposal;
        });

        MemberMatchResultProposalResponse response = service.propose(reporter, request);

        assertEquals(MemberMatchResultStatus.PENDING, response.getStatus());
        assertEquals("Maya", response.getReporterName());
        assertEquals("Jordan", response.getOpponentName());
        assertEquals(1L, response.getWinnerId());
    }

    @Test
    void onlyNamedOpponentCanAgreeAndCreateOfficialMatch() {
        MemberMatchResultProposalRepository proposalRepository = mock(MemberMatchResultProposalRepository.class);
        MatchService matchService = mock(MatchService.class);
        MemberMatchResultService service = new MemberMatchResultService(proposalRepository, mock(PlayerRepository.class),
                mock(ClubUserRepository.class), matchService);
        Player reporter = player(1L, "Maya");
        Player opponent = player(2L, "Jordan");
        MemberMatchResultProposal proposal = proposal(reporter, opponent, 3, 1);
        Match officialMatch = new Match();
        ReflectionTestUtils.setField(officialMatch, "id", 77L);

        when(proposalRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(proposal));
        when(matchService.recordConfirmedMemberResult(1L, 2L, 3, 1)).thenReturn(officialMatch);

        MemberMatchResultProposalResponse response = service.agree(10L, opponent);

        verify(matchService).recordConfirmedMemberResult(1L, 2L, 3, 1);
        assertEquals(MemberMatchResultStatus.AGREED, response.getStatus());
        assertEquals(77L, response.getOfficialMatchId());
    }

    @Test
    void reporterCannotAgreeWithTheirOwnResultRequest() {
        MemberMatchResultProposalRepository proposalRepository = mock(MemberMatchResultProposalRepository.class);
        MemberMatchResultService service = new MemberMatchResultService(proposalRepository, mock(PlayerRepository.class),
                mock(ClubUserRepository.class), mock(MatchService.class));
        Player reporter = player(1L, "Maya");
        Player opponent = player(2L, "Jordan");

        when(proposalRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(proposal(reporter, opponent, 3, 1)));

        assertThrows(NotFoundException.class, () -> service.agree(10L, reporter));
    }

    private ProposeMemberMatchResultRequest request(Long opponentId, int reporterScore, int opponentScore) {
        ProposeMemberMatchResultRequest request = new ProposeMemberMatchResultRequest();
        request.setOpponentId(opponentId);
        request.setReporterScore(reporterScore);
        request.setOpponentScore(opponentScore);
        return request;
    }

    private MemberMatchResultProposal proposal(Player reporter, Player opponent, int reporterScore, int opponentScore) {
        MemberMatchResultProposal proposal = new MemberMatchResultProposal();
        ReflectionTestUtils.setField(proposal, "id", 10L);
        ReflectionTestUtils.setField(proposal, "proposedAt", java.time.Instant.parse("2026-07-30T12:00:00Z"));
        proposal.setReporter(reporter);
        proposal.setOpponent(opponent);
        proposal.setReporterScore(reporterScore);
        proposal.setOpponentScore(opponentScore);
        return proposal;
    }

    private Player player(Long id, String displayName) {
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", id);
        player.setDisplayName(displayName);
        player.setActive(true);
        return player;
    }
}
