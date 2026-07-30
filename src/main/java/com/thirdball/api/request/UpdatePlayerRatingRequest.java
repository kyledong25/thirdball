package com.thirdball.api.request;

import javax.validation.constraints.NotNull;

/** Administrator-only correction of a member's current rating. */
public class UpdatePlayerRatingRequest {
    @NotNull
    private Integer rating;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
