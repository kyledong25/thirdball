package com.thirdball.service;

import com.thirdball.api.response.RatingHistoryPointResponse;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Player;
import com.thirdball.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Builds a member's chart series solely from completed match rating snapshots. */
@Service
public class MemberRatingHistoryService {
    private final MatchRepository matchRepository;

    public MemberRatingHistoryService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Transactional(readOnly = true)
    public List<RatingHistoryPointResponse> listFor(Player player) {
        List<RatingHistoryPointResponse> points = new ArrayList<>();
        for (Match match : matchRepository.findCompletedForPlayer(player.getId(), MatchStatus.COMPLETED)) {
            boolean isPlayerOne = match.getPlayerOne().getId().equals(player.getId());
            Integer ratingBefore = isPlayerOne ? match.getPlayerOneRatingBefore() : match.getPlayerTwoRatingBefore();
            Integer ratingAfter = isPlayerOne ? match.getPlayerOneRatingAfter() : match.getPlayerTwoRatingAfter();

            // Provisional results may have no rating snapshot until the fifth match.
            if (points.isEmpty() && ratingBefore != null) {
                points.add(RatingHistoryPointResponse.baseline(match.getId(), match.getCompletedAt(), ratingBefore));
            }
            if (ratingAfter == null) {
                continue;
            }

            Player opponent = isPlayerOne ? match.getPlayerTwo() : match.getPlayerOne();
            boolean won = match.getWinner() != null && match.getWinner().getId().equals(player.getId());
            points.add(RatingHistoryPointResponse.result(
                    match.getId(), match.getCompletedAt(), ratingAfter, opponent.getDisplayName(), won));
        }
        return points;
    }
}
