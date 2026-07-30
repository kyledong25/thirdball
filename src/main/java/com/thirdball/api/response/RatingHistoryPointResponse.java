package com.thirdball.api.response;

import java.time.Instant;

/** One plotted point in the authenticated member's recorded rating history. */
public class RatingHistoryPointResponse {
    private final Long matchId;
    private final Instant occurredAt;
    private final int rating;
    private final String opponentName;
    private final boolean won;
    private final boolean baseline;

    private RatingHistoryPointResponse(Long matchId, Instant occurredAt, int rating,
                                       String opponentName, boolean won, boolean baseline) {
        this.matchId = matchId;
        this.occurredAt = occurredAt;
        this.rating = rating;
        this.opponentName = opponentName;
        this.won = won;
        this.baseline = baseline;
    }

    public static RatingHistoryPointResponse baseline(Long matchId, Instant occurredAt, int rating) {
        return new RatingHistoryPointResponse(matchId, occurredAt, rating, null, false, true);
    }

    public static RatingHistoryPointResponse result(Long matchId, Instant occurredAt, int rating,
                                                    String opponentName, boolean won) {
        return new RatingHistoryPointResponse(matchId, occurredAt, rating, opponentName, won, false);
    }

    public Long getMatchId() { return matchId; }
    public Instant getOccurredAt() { return occurredAt; }
    public int getRating() { return rating; }
    public String getOpponentName() { return opponentName; }
    public boolean isWon() { return won; }
    public boolean isBaseline() { return baseline; }
}
