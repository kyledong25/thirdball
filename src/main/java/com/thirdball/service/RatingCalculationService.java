package com.thirdball.service;

import org.springframework.stereotype.Service;

/**
 * Applies the USATT point-exchange chart for one completed two-player match.
 *
 * <p>The calculation uses the players' ratings before the match. The higher
 * rated winner is the expected result; a lower rated winner is an upset. The
 * chart then supplies the fixed number of points exchanged by both players.</p>
 */
@Service
public class RatingCalculationService {

    /**
     * Calculates post-match ratings using the official USATT point spread chart.
     *
     * @param player1Rating player one's rating before the match (zero or higher)
     * @param player2Rating player two's rating before the match (zero or higher)
     * @param player1Won {@code true} when player one won the match
     * @return both updated ratings and the number of points exchanged
     */
    public RatingUpdate calculateRatingChange(int player1Rating, int player2Rating, boolean player1Won) {
        validateRating(player1Rating, "player one");
        validateRating(player2Rating, "player two");

        int spread = Math.abs(player1Rating - player2Rating);
        boolean expectedResult = player1Rating == player2Rating
                || (player1Rating > player2Rating && player1Won)
                || (player2Rating > player1Rating && !player1Won);

        int pointsExchanged = pointsFor(spread, expectedResult);
        int player1Change = player1Won ? pointsExchanged : -pointsExchanged;
        int player2Change = -player1Change;

        return new RatingUpdate(
                Math.max(0, player1Rating + player1Change),
                Math.max(0, player2Rating + player2Change),
                pointsExchanged
        );
    }

    private int pointsFor(int spread, boolean expectedResult) {
        if (spread <= 12) {
            return 8;
        }
        if (spread <= 37) {
            return expectedResult ? 7 : 10;
        }
        if (spread <= 62) {
            return expectedResult ? 6 : 13;
        }
        if (spread <= 87) {
            return expectedResult ? 5 : 16;
        }
        if (spread <= 112) {
            return expectedResult ? 4 : 20;
        }
        if (spread <= 137) {
            return expectedResult ? 3 : 25;
        }
        if (spread <= 162) {
            return expectedResult ? 2 : 30;
        }
        if (spread <= 187) {
            return expectedResult ? 2 : 35;
        }
        if (spread <= 212) {
            return expectedResult ? 1 : 40;
        }
        if (spread <= 237) {
            return expectedResult ? 1 : 45;
        }
        return expectedResult ? 0 : 50;
    }

    private void validateRating(int rating, String player) {
        if (rating < 0) {
            throw new IllegalArgumentException("The " + player + " rating cannot be negative");
        }
    }

    /** Immutable result returned by {@link #calculateRatingChange(int, int, boolean)}. */
    public static final class RatingUpdate {
        private final int player1Rating;
        private final int player2Rating;
        private final int pointsExchanged;

        private RatingUpdate(int player1Rating, int player2Rating, int pointsExchanged) {
            this.player1Rating = player1Rating;
            this.player2Rating = player2Rating;
            this.pointsExchanged = pointsExchanged;
        }

        public int getPlayer1Rating() { return player1Rating; }
        public int getPlayer2Rating() { return player2Rating; }
        public int getPointsExchanged() { return pointsExchanged; }
    }
}
