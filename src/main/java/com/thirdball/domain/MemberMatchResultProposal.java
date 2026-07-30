package com.thirdball.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.Instant;

/** A score report that requires the opponent's agreement before rating either player. */
@Entity
@Table(name = "member_match_result_proposals")
public class MemberMatchResultProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_player_id", nullable = false)
    private Player reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_player_id", nullable = false)
    private Player opponent;

    @Column(name = "reporter_score", nullable = false)
    private int reporterScore;

    @Column(name = "opponent_score", nullable = false)
    private int opponentScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberMatchResultStatus status = MemberMatchResultStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "official_match_id", unique = true)
    private Match officialMatch;

    @Column(name = "proposed_at", nullable = false, updatable = false)
    private Instant proposedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        proposedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Player getReporter() { return reporter; }
    public void setReporter(Player reporter) { this.reporter = reporter; }
    public Player getOpponent() { return opponent; }
    public void setOpponent(Player opponent) { this.opponent = opponent; }
    public int getReporterScore() { return reporterScore; }
    public void setReporterScore(int reporterScore) { this.reporterScore = reporterScore; }
    public int getOpponentScore() { return opponentScore; }
    public void setOpponentScore(int opponentScore) { this.opponentScore = opponentScore; }
    public MemberMatchResultStatus getStatus() { return status; }
    public void setStatus(MemberMatchResultStatus status) { this.status = status; }
    public Match getOfficialMatch() { return officialMatch; }
    public void setOfficialMatch(Match officialMatch) { this.officialMatch = officialMatch; }
    public Instant getProposedAt() { return proposedAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
}
