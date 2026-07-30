package com.thirdball.api.response;

import com.thirdball.domain.MemberFeedback;
import java.time.Instant;

/** Administrative read model; it deliberately omits member contact details. */
public class MemberFeedbackResponse {
    private final Long id;
    private final Long playerId;
    private final String playerName;
    private final String subject;
    private final String message;
    private final Instant submittedAt;

    private MemberFeedbackResponse(MemberFeedback feedback) {
        id = feedback.getId();
        playerId = feedback.getPlayer().getId();
        playerName = feedback.getPlayer().getDisplayName();
        subject = feedback.getSubject();
        message = feedback.getMessage();
        submittedAt = feedback.getSubmittedAt();
    }

    public static MemberFeedbackResponse from(MemberFeedback feedback) { return new MemberFeedbackResponse(feedback); }
    public Long getId() { return id; }
    public Long getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public Instant getSubmittedAt() { return submittedAt; }
}
