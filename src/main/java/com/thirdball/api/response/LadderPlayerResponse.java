package com.thirdball.api.response;

import com.thirdball.domain.Player;

/**
 * The intentionally limited player information published on the member ladder.
 * Contact details, dues state, and administrative flags stay private.
 */
public class LadderPlayerResponse {
    private final Long id;
    private final String displayName;
    private final int rating;
    private final boolean ratingEstablished;
    private final int provisionalMatchCount;

    private LadderPlayerResponse(Player player) {
        id = player.getId();
        displayName = player.getDisplayName();
        rating = player.getRating();
        ratingEstablished = player.isRatingEstablished();
        provisionalMatchCount = player.getProvisionalMatchCount();
    }

    public static LadderPlayerResponse from(Player player) {
        return new LadderPlayerResponse(player);
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getRating() { return rating; }
    public boolean isRatingEstablished() { return ratingEstablished; }
    public int getProvisionalMatchCount() { return provisionalMatchCount; }
}
