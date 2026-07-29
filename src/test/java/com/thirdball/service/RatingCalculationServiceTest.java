package com.thirdball.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingCalculationServiceTest {
    private final RatingCalculationService ratingCalculationService = new RatingCalculationService();

    @Test
    void usesExactUsattPointsAtEverySpreadBoundary() {
        int[][] ranges = {
                {0, 12, 8, 8}, {13, 37, 7, 10}, {38, 62, 6, 13},
                {63, 87, 5, 16}, {88, 112, 4, 20}, {113, 137, 3, 25},
                {138, 162, 2, 30}, {163, 187, 2, 35}, {188, 212, 1, 40},
                {213, 237, 1, 45}, {238, 300, 0, 50}
        };

        for (int[] range : ranges) {
            assertExchange(range[0], range[2], range[3]);
            assertExchange(range[1], range[2], range[3]);
        }
    }

    @Test
    void givesEightPointsWhenPlayersHaveEqualRatingsRegardlessOfWinner() {
        RatingCalculationService.RatingUpdate update = ratingCalculationService.calculateRatingChange(1200, 1200, false);

        assertEquals(1192, update.getPlayer1Rating());
        assertEquals(1208, update.getPlayer2Rating());
        assertEquals(8, update.getPointsExchanged());
    }

    @Test
    void neverLetsTheLosingRatingFallBelowZero() {
        RatingCalculationService.RatingUpdate update = ratingCalculationService.calculateRatingChange(4, 12, false);

        assertEquals(0, update.getPlayer1Rating());
        assertEquals(20, update.getPlayer2Rating());
    }

    @Test
    void rejectsNegativePreMatchRatings() {
        assertThrows(IllegalArgumentException.class,
                () -> ratingCalculationService.calculateRatingChange(-1, 1200, true));
    }

    private void assertExchange(int spread, int expectedPoints, int upsetPoints) {
        int lowerRating = 1000;
        int higherRating = lowerRating + spread;

        RatingCalculationService.RatingUpdate expectedUpdate =
                ratingCalculationService.calculateRatingChange(higherRating, lowerRating, true);
        assertEquals(expectedPoints, expectedUpdate.getPointsExchanged(), "expected result at spread " + spread);
        assertEquals(higherRating + expectedPoints, expectedUpdate.getPlayer1Rating());
        assertEquals(lowerRating - expectedPoints, expectedUpdate.getPlayer2Rating());

        RatingCalculationService.RatingUpdate upsetUpdate =
                ratingCalculationService.calculateRatingChange(lowerRating, higherRating, true);
        assertEquals(upsetPoints, upsetUpdate.getPointsExchanged(), "upset result at spread " + spread);
        assertEquals(lowerRating + upsetPoints, upsetUpdate.getPlayer1Rating());
        assertEquals(higherRating - upsetPoints, upsetUpdate.getPlayer2Rating());
    }
}
