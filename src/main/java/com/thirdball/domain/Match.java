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
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_one_id")
    private Player playerOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_id")
    private Player playerTwo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Player winner;

    @Column(name = "player_one_score")
    private Integer playerOneScore;

    @Column(name = "player_two_score")
    private Integer playerTwoScore;

    @Column(name = "player_one_rating_before")
    private Integer playerOneRatingBefore;
    @Column(name = "player_one_rating_after")
    private Integer playerOneRatingAfter;
    @Column(name = "player_two_rating_before")
    private Integer playerTwoRatingBefore;
    @Column(name = "player_two_rating_after")
    private Integer playerTwoRatingAfter;

    @Column(name = "round_number", nullable = false)
    private int roundNumber = 1;

    @Column(name = "bracket_slot")
    private Integer bracketSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    @Column(name = "next_match_player_slot")
    private Integer nextMatchPlayerSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    public Long getId() { return id; }
    public Tournament getTournament() { return tournament; }
    public void setTournament(Tournament tournament) { this.tournament = tournament; }
    public Player getPlayerOne() { return playerOne; }
    public void setPlayerOne(Player playerOne) { this.playerOne = playerOne; }
    public Player getPlayerTwo() { return playerTwo; }
    public void setPlayerTwo(Player playerTwo) { this.playerTwo = playerTwo; }
    public Player getWinner() { return winner; }
    public void setWinner(Player winner) { this.winner = winner; }
    public Integer getPlayerOneScore() { return playerOneScore; }
    public void setPlayerOneScore(Integer playerOneScore) { this.playerOneScore = playerOneScore; }
    public Integer getPlayerTwoScore() { return playerTwoScore; }
    public void setPlayerTwoScore(Integer playerTwoScore) { this.playerTwoScore = playerTwoScore; }
    public Integer getPlayerOneRatingBefore() { return playerOneRatingBefore; }
    public void setPlayerOneRatingBefore(Integer value) { this.playerOneRatingBefore = value; }
    public Integer getPlayerOneRatingAfter() { return playerOneRatingAfter; }
    public void setPlayerOneRatingAfter(Integer value) { this.playerOneRatingAfter = value; }
    public Integer getPlayerTwoRatingBefore() { return playerTwoRatingBefore; }
    public void setPlayerTwoRatingBefore(Integer value) { this.playerTwoRatingBefore = value; }
    public Integer getPlayerTwoRatingAfter() { return playerTwoRatingAfter; }
    public void setPlayerTwoRatingAfter(Integer value) { this.playerTwoRatingAfter = value; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public Integer getBracketSlot() { return bracketSlot; }
    public void setBracketSlot(Integer bracketSlot) { this.bracketSlot = bracketSlot; }
    public Match getNextMatch() { return nextMatch; }
    public void setNextMatch(Match nextMatch) { this.nextMatch = nextMatch; }
    public Integer getNextMatchPlayerSlot() { return nextMatchPlayerSlot; }
    public void setNextMatchPlayerSlot(Integer value) { this.nextMatchPlayerSlot = value; }
    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
