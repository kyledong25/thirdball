package com.thirdball.api.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class SubmitMatchResultRequest {
    @NotNull @Min(0)
    private Integer playerOneScore;
    @NotNull @Min(0)
    private Integer playerTwoScore;

    public Integer getPlayerOneScore() { return playerOneScore; }
    public void setPlayerOneScore(Integer playerOneScore) { this.playerOneScore = playerOneScore; }
    public Integer getPlayerTwoScore() { return playerTwoScore; }
    public void setPlayerTwoScore(Integer playerTwoScore) { this.playerTwoScore = playerTwoScore; }
}
