package com.thirdball.api.response;

import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import java.time.Instant;

public class MatchResponse {
    private final Long id;
    private final Long tournamentId;
    private final Long playerOneId;
    private final String playerOneName;
    private final Long playerTwoId;
    private final String playerTwoName;
    private final Long winnerId;
    private final Integer playerOneScore;
    private final Integer playerTwoScore;
    private final Integer playerOneRatingBefore;
    private final Integer playerOneRatingAfter;
    private final Integer playerTwoRatingBefore;
    private final Integer playerTwoRatingAfter;
    private final int roundNumber;
    private final Integer bracketSlot;
    private final MatchStatus status;
    private final Instant completedAt;

    private MatchResponse(Match match) {
        id = match.getId();
        tournamentId = match.getTournament() == null ? null : match.getTournament().getId();
        playerOneId = match.getPlayerOne().getId();
        playerOneName = match.getPlayerOne().getDisplayName();
        playerTwoId = match.getPlayerTwo().getId();
        playerTwoName = match.getPlayerTwo().getDisplayName();
        winnerId = match.getWinner() == null ? null : match.getWinner().getId();
        playerOneScore = match.getPlayerOneScore();
        playerTwoScore = match.getPlayerTwoScore();
        playerOneRatingBefore = match.getPlayerOneRatingBefore();
        playerOneRatingAfter = match.getPlayerOneRatingAfter();
        playerTwoRatingBefore = match.getPlayerTwoRatingBefore();
        playerTwoRatingAfter = match.getPlayerTwoRatingAfter();
        roundNumber = match.getRoundNumber();
        bracketSlot = match.getBracketSlot();
        status = match.getStatus();
        completedAt = match.getCompletedAt();
    }

    public static MatchResponse from(Match match) { return new MatchResponse(match); }
    public Long getId() { return id; }
    public Long getTournamentId() { return tournamentId; }
    public Long getPlayerOneId() { return playerOneId; }
    public String getPlayerOneName() { return playerOneName; }
    public Long getPlayerTwoId() { return playerTwoId; }
    public String getPlayerTwoName() { return playerTwoName; }
    public Long getWinnerId() { return winnerId; }
    public Integer getPlayerOneScore() { return playerOneScore; }
    public Integer getPlayerTwoScore() { return playerTwoScore; }
    public Integer getPlayerOneRatingBefore() { return playerOneRatingBefore; }
    public Integer getPlayerOneRatingAfter() { return playerOneRatingAfter; }
    public Integer getPlayerTwoRatingBefore() { return playerTwoRatingBefore; }
    public Integer getPlayerTwoRatingAfter() { return playerTwoRatingAfter; }
    public int getRoundNumber() { return roundNumber; }
    public Integer getBracketSlot() { return bracketSlot; }
    public MatchStatus getStatus() { return status; }
    public Instant getCompletedAt() { return completedAt; }
}
