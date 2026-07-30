package com.thirdball.api.response;

import com.thirdball.domain.MemberMatchResultProposal;
import com.thirdball.domain.MemberMatchResultStatus;

import java.time.Instant;

/** Read model for the two-player confirmation flow; it contains no contact or dues information. */
public class MemberMatchResultProposalResponse {
    private final Long id;
    private final Long reporterId;
    private final String reporterName;
    private final Long opponentId;
    private final String opponentName;
    private final int reporterScore;
    private final int opponentScore;
    private final Long winnerId;
    private final MemberMatchResultStatus status;
    private final Long officialMatchId;
    private final Instant proposedAt;
    private final Instant respondedAt;

    private MemberMatchResultProposalResponse(MemberMatchResultProposal proposal) {
        id = proposal.getId();
        reporterId = proposal.getReporter().getId();
        reporterName = proposal.getReporter().getDisplayName();
        opponentId = proposal.getOpponent().getId();
        opponentName = proposal.getOpponent().getDisplayName();
        reporterScore = proposal.getReporterScore();
        opponentScore = proposal.getOpponentScore();
        winnerId = reporterScore > opponentScore ? reporterId : opponentId;
        status = proposal.getStatus();
        officialMatchId = proposal.getOfficialMatch() == null ? null : proposal.getOfficialMatch().getId();
        proposedAt = proposal.getProposedAt();
        respondedAt = proposal.getRespondedAt();
    }

    public static MemberMatchResultProposalResponse from(MemberMatchResultProposal proposal) {
        return new MemberMatchResultProposalResponse(proposal);
    }

    public Long getId() { return id; }
    public Long getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public Long getOpponentId() { return opponentId; }
    public String getOpponentName() { return opponentName; }
    public int getReporterScore() { return reporterScore; }
    public int getOpponentScore() { return opponentScore; }
    public Long getWinnerId() { return winnerId; }
    public MemberMatchResultStatus getStatus() { return status; }
    public Long getOfficialMatchId() { return officialMatchId; }
    public Instant getProposedAt() { return proposedAt; }
    public Instant getRespondedAt() { return respondedAt; }
}
