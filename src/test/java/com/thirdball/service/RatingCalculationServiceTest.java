package com.thirdball.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
    void calculatesWinnerAndLoserRatingsFromTheirPreMatchRatings() {
        RatingCalculationService.RatingUpdate expectedWin =
                ratingCalculationService.calculateNewRatings(1400, 1300);
        assertEquals(1404, expectedWin.getWinnerRating());
        assertEquals(1296, expectedWin.getLoserRating());
        assertEquals(4, expectedWin.getPointsExchanged());

        RatingCalculationService.RatingUpdate upset =
                ratingCalculationService.calculateNewRatings(1300, 1400);
        assertEquals(1320, upset.getWinnerRating());
        assertEquals(1380, upset.getLoserRating());
        assertEquals(20, upset.getPointsExchanged());
    }

    @Test
    void preservesTheExactZeroSumExchangeEvenWhenTheLoserBecomesNegative() {
        RatingCalculationService.RatingUpdate update = ratingCalculationService.calculateRatingChange(4, 12, false);

        assertEquals(-4, update.getPlayer1Rating());
        assertEquals(20, update.getPlayer2Rating());
    }

    @Test
    void initializesMixedResultsAtTheMidpointOfBestWinAndWorstLoss() {
        int rating = ratingCalculationService.initializeProvisionalRating(Arrays.asList(
                result(1450, true), result(1320, false), result(1200, true), result(1500, false)));

        assertEquals(1385, rating);
    }

    @Test
    void initializesAnUndefeatedPlayerAboveTheirBestWin() {
        int rating = ratingCalculationService.initializeProvisionalRating(Arrays.asList(
                result(1350, true), result(1510, true), result(1460, true)));

        assertEquals(1760, rating);
    }

    @Test
    void initializesAWinlessPlayerBelowTheirLowestRatedLoss() {
        int rating = ratingCalculationService.initializeProvisionalRating(Arrays.asList(
                result(1400, false), result(1180, false), result(1300, false)));

        assertEquals(1130, rating);
    }

    @Test
    void rejectsAnEmptyProvisionalRecord() {
        assertThrows(IllegalArgumentException.class,
                () -> ratingCalculationService.initializeProvisionalRating(Arrays.asList()));
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

    private RatingCalculationService.ProvisionalMatchResult result(int opponentRating, boolean won) {
        return new RatingCalculationService.ProvisionalMatchResult(opponentRating, won);
    }
}
