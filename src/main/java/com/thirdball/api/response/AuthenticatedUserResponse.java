package com.thirdball.api.response;

import com.thirdball.domain.ClubUser;

/** Safe account information returned after Basic authentication succeeds. */
public class AuthenticatedUserResponse {
    private final Long id;
    private final String email;
    private final String role;
    private final Long playerId;
    private final String displayName;

    private AuthenticatedUserResponse(ClubUser user) {
        id = user.getId();
        email = user.getEmail();
        role = user.getRole().name();
        playerId = user.getPlayer() == null ? null : user.getPlayer().getId();
        displayName = user.getPlayer() == null ? user.getEmail() : user.getPlayer().getDisplayName();
    }

    public static AuthenticatedUserResponse from(ClubUser user) { return new AuthenticatedUserResponse(user); }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getPlayerId() { return playerId; }
    public String getDisplayName() { return displayName; }
}
