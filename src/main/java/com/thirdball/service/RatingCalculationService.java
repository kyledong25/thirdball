package com.thirdball.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Applies the USATT point-exchange chart for one completed two-player match.
 *
 * <p>The calculation uses the players' ratings before the match. The higher
 * rated winner is the expected result; a lower rated winner is an upset. The
 * chart then supplies the fixed number of points exchanged by both players.</p>
 */
@Service
public class RatingCalculationService {
    private static final int UNDEFEATED_BONUS = 250;
    private static final int WINLESS_DEDUCTION = 50;

    /**
     * Ordered USATT rating-spread bands. Each row stores the inclusive upper
     * bound and the fixed exchange for an expected win and an upset.
     */
    private static final ExchangeBand[] EXCHANGE_BANDS = {
            new ExchangeBand(12, 8, 8),
            new ExchangeBand(37, 7, 10),
            new ExchangeBand(62, 6, 13),
            new ExchangeBand(87, 5, 16),
            new ExchangeBand(112, 4, 20),
            new ExchangeBand(137, 3, 25),
            new ExchangeBand(162, 2, 30),
            new ExchangeBand(187, 2, 35),
            new ExchangeBand(212, 1, 40),
            new ExchangeBand(237, 1, 45),
            new ExchangeBand(Integer.MAX_VALUE, 0, 50)
    };

    /**
     * Calculates post-match ratings using the official USATT point spread chart.
     *
     * @param player1Rating player one's rating before the match
     * @param player2Rating player two's rating before the match
     * @param player1Won {@code true} when player one won the match
     * @return both updated ratings and the number of points exchanged
     */
    public RatingUpdate calculateRatingChange(int player1Rating, int player2Rating, boolean player1Won) {
        RatingUpdate winnerAndLoser = player1Won
                ? calculateNewRatings(player1Rating, player2Rating)
                : calculateNewRatings(player2Rating, player1Rating);

        return player1Won
                ? winnerAndLoser
                : new RatingUpdate(
                        winnerAndLoser.getLoserRating(),
                        winnerAndLoser.getWinnerRating(),
                        winnerAndLoser.getPointsExchanged());
    }

    /**
     * Calculates new ratings when the winner and loser are already known.
     * The result is an expected win when the winner's pre-match rating is at
     * least the loser's; otherwise it is an upset.
     *
     * @param winnerRating winner's pre-match rating
     * @param loserRating loser's pre-match rating
     * @return winner and loser ratings after the fixed USATT exchange
     */
    public RatingUpdate calculateNewRatings(int winnerRating, int loserRating) {
        int spread = Math.abs(winnerRating - loserRating);
        boolean expectedWin = winnerRating >= loserRating;
        int pointsExchanged = pointsForSpread(spread, expectedWin);

        return new RatingUpdate(
                winnerRating + pointsExchanged,
                loserRating - pointsExchanged,
                pointsExchanged);
    }

    /**
     * Returns the chart value for a pre-match rating spread.
     *
     * @param spread absolute rating difference (zero or higher)
     * @param expectedWin whether the higher-rated player won
     */
    public int pointsForSpread(int spread, boolean expectedWin) {
        if (spread < 0) {
            throw new IllegalArgumentException("The rating spread cannot be negative");
        }

        for (ExchangeBand band : EXCHANGE_BANDS) {
            if (spread <= band.maxSpread) {
                return expectedWin ? band.expectedWinPoints : band.upsetWinPoints;
            }
        }
        throw new IllegalStateException("USATT exchange chart does not cover spread " + spread);
    }

    /**
     * Produces a player's first rating from matches played while unrated.
     * Mixed results use the midpoint of the strongest win and weakest loss;
     * undefeated and winless records use the USATT offsets supplied by the
     * club's policy. Ratings are stored as whole points, so a half-point
     * midpoint is rounded to the nearest integer.
     */
    public int initializeProvisionalRating(List<ProvisionalMatchResult> matchResults) {
        if (matchResults == null || matchResults.isEmpty()) {
            throw new IllegalArgumentException("At least one provisional match result is required");
        }

        Integer highestRatedWin = null;
        Integer lowestRatedLoss = null;
        for (ProvisionalMatchResult result : matchResults) {
            if (result.isWon()) {
                highestRatedWin = highestRatedWin == null
                        ? result.getOpponentRating()
                        : Math.max(highestRatedWin, result.getOpponentRating());
            } else {
                lowestRatedLoss = lowestRatedLoss == null
                        ? result.getOpponentRating()
                        : Math.min(lowestRatedLoss, result.getOpponentRating());
            }
        }

        if (highestRatedWin != null && lowestRatedLoss != null) {
            return Math.round((highestRatedWin + lowestRatedLoss) / 2.0f);
        }
        if (highestRatedWin != null) {
            return highestRatedWin + UNDEFEATED_BONUS;
        }
        return lowestRatedLoss - WINLESS_DEDUCTION;
    }

    /** Immutable result returned by a rating calculation. */
    public static final class RatingUpdate {
        private final int firstRating;
        private final int secondRating;
        private final int pointsExchanged;

        private RatingUpdate(int firstRating, int secondRating, int pointsExchanged) {
            this.firstRating = firstRating;
            this.secondRating = secondRating;
            this.pointsExchanged = pointsExchanged;
        }

        /** Player-one/two accessors used by {@link #calculateRatingChange(int, int, boolean)}. */
        public int getPlayer1Rating() { return firstRating; }
        public int getPlayer2Rating() { return secondRating; }

        /** Winner/loser accessors used by {@link #calculateNewRatings(int, int)}. */
        public int getWinnerRating() { return firstRating; }
        public int getLoserRating() { return secondRating; }
        public int getPointsExchanged() { return pointsExchanged; }
    }

    /** A single result used to initialize an unrated player. */
    public static final class ProvisionalMatchResult {
        private final int opponentRating;
        private final boolean won;

        public ProvisionalMatchResult(int opponentRating, boolean won) {
            this.opponentRating = opponentRating;
            this.won = won;
        }

        public int getOpponentRating() { return opponentRating; }
        public boolean isWon() { return won; }
    }

    private static final class ExchangeBand {
        private final int maxSpread;
        private final int expectedWinPoints;
        private final int upsetWinPoints;

        private ExchangeBand(int maxSpread, int expectedWinPoints, int upsetWinPoints) {
            this.maxSpread = maxSpread;
            this.expectedWinPoints = expectedWinPoints;
            this.upsetWinPoints = upsetWinPoints;
        }
    }
}
