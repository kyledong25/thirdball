package com.thirdball.service;

import com.thirdball.api.request.ProposeMemberMatchResultRequest;
import com.thirdball.api.response.MemberMatchResultProposalResponse;
import com.thirdball.domain.ClubRole;
import com.thirdball.domain.Match;
import com.thirdball.domain.MemberMatchResultProposal;
import com.thirdball.domain.MemberMatchResultStatus;
import com.thirdball.domain.Player;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.ClubUserRepository;
import com.thirdball.repository.MemberMatchResultProposalRepository;
import com.thirdball.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/** Coordinates a two-player agreement before a member result can affect the ladder. */
@Service
public class MemberMatchResultService {
    private final MemberMatchResultProposalRepository proposalRepository;
    private final PlayerRepository playerRepository;
    private final ClubUserRepository clubUserRepository;
    private final MatchService matchService;

    public MemberMatchResultService(MemberMatchResultProposalRepository proposalRepository,
                                    PlayerRepository playerRepository,
                                    ClubUserRepository clubUserRepository,
                                    MatchService matchService) {
        this.proposalRepository = proposalRepository;
        this.playerRepository = playerRepository;
        this.clubUserRepository = clubUserRepository;
        this.matchService = matchService;
    }

    @Transactional
    public MemberMatchResultProposalResponse propose(Player currentMember,
                                                      ProposeMemberMatchResultRequest request) {
        if (currentMember.getId().equals(request.getOpponentId())) {
            throw new IllegalArgumentException("You cannot report a match against yourself");
        }
        validateNonTiedScore(request.getReporterScore(), request.getOpponentScore());

        Player[] players = lockPlayers(currentMember.getId(), request.getOpponentId());
        Player reporter = players[0];
        Player opponent = players[1];
        if (!reporter.isActive() || !opponent.isActive()) {
            throw new ConflictException("Inactive players cannot be included in a match result");
        }
        if (!clubUserRepository.existsByPlayer_IdAndRole(opponent.getId(), ClubRole.MEMBER)) {
            throw new ConflictException("The selected opponent does not have a member account to confirm this result");
        }

        MemberMatchResultProposal proposal = new MemberMatchResultProposal();
        proposal.setReporter(reporter);
        proposal.setOpponent(opponent);
        proposal.setReporterScore(request.getReporterScore());
        proposal.setOpponentScore(request.getOpponentScore());
        return MemberMatchResultProposalResponse.from(proposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public List<MemberMatchResultProposalResponse> listFor(Player currentMember) {
        return proposalRepository.findForPlayer(currentMember.getId()).stream()
                .map(MemberMatchResultProposalResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberMatchResultProposalResponse agree(Long proposalId, Player currentMember) {
        MemberMatchResultProposal proposal = findPendingProposalForOpponent(proposalId, currentMember.getId());
        Match officialMatch = matchService.recordConfirmedMemberResult(
                proposal.getReporter().getId(), proposal.getOpponent().getId(),
                proposal.getReporterScore(), proposal.getOpponentScore());
        proposal.setOfficialMatch(officialMatch);
        proposal.setStatus(MemberMatchResultStatus.AGREED);
        proposal.setRespondedAt(Instant.now());
        return MemberMatchResultProposalResponse.from(proposal);
    }

    @Transactional
    public MemberMatchResultProposalResponse decline(Long proposalId, Player currentMember) {
        MemberMatchResultProposal proposal = findPendingProposalForOpponent(proposalId, currentMember.getId());
        proposal.setStatus(MemberMatchResultStatus.DECLINED);
        proposal.setRespondedAt(Instant.now());
        return MemberMatchResultProposalResponse.from(proposal);
    }

    private MemberMatchResultProposal findPendingProposalForOpponent(Long proposalId, Long playerId) {
        MemberMatchResultProposal proposal = proposalRepository.findByIdForUpdate(proposalId)
                .orElseThrow(() -> new NotFoundException("Match result request " + proposalId + " was not found"));
        if (!proposal.getOpponent().getId().equals(playerId)) {
            throw new NotFoundException("Match result request " + proposalId + " was not found");
        }
        if (proposal.getStatus() != MemberMatchResultStatus.PENDING) {
            throw new ConflictException("This match result request has already been answered");
        }
        return proposal;
    }

    private Player[] lockPlayers(Long reporterId, Long opponentId) {
        Player lowerIdPlayer = playerRepository.findByIdForUpdate(Math.min(reporterId, opponentId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        Player higherIdPlayer = playerRepository.findByIdForUpdate(Math.max(reporterId, opponentId))
                .orElseThrow(() -> new NotFoundException("A match player was not found"));
        return lowerIdPlayer.getId().equals(reporterId)
                ? new Player[] { lowerIdPlayer, higherIdPlayer }
                : new Player[] { higherIdPlayer, lowerIdPlayer };
    }

    private void validateNonTiedScore(Integer reporterScore, Integer opponentScore) {
        if (reporterScore.equals(opponentScore)) {
            throw new IllegalArgumentException("Table tennis matches cannot end in a tie");
        }
    }
}
