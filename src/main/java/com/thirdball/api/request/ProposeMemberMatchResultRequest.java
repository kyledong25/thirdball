package com.thirdball.api.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class ProposeMemberMatchResultRequest {
    @NotNull @Min(1)
    private Long opponentId;
    @NotNull @Min(0)
    private Integer reporterScore;
    @NotNull @Min(0)
    private Integer opponentScore;

    public Long getOpponentId() { return opponentId; }
    public void setOpponentId(Long opponentId) { this.opponentId = opponentId; }
    public Integer getReporterScore() { return reporterScore; }
    public void setReporterScore(Integer reporterScore) { this.reporterScore = reporterScore; }
    public Integer getOpponentScore() { return opponentScore; }
    public void setOpponentScore(Integer opponentScore) { this.opponentScore = opponentScore; }
}
