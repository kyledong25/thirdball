package com.thirdball.api.request;

import javax.validation.constraints.NotNull;

public class PlayerRegistrationRequest {
    @NotNull
    private Long playerId;

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
}
