package com.thirdball.api.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class CreateMatchRequest {
    private Long tournamentId;
    @NotNull
    private Long playerOneId;
    @NotNull
    private Long playerTwoId;
    @Min(1)
    private int roundNumber = 1;
    @Min(1)
    private Integer bracketSlot;

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }
    public Long getPlayerOneId() { return playerOneId; }
    public void setPlayerOneId(Long playerOneId) { this.playerOneId = playerOneId; }
    public Long getPlayerTwoId() { return playerTwoId; }
    public void setPlayerTwoId(Long playerTwoId) { this.playerTwoId = playerTwoId; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public Integer getBracketSlot() { return bracketSlot; }
    public void setBracketSlot(Integer bracketSlot) { this.bracketSlot = bracketSlot; }
}
