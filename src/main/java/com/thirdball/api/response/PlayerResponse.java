package com.thirdball.api.response;

import com.thirdball.domain.Player;
import java.time.Instant;

public class PlayerResponse {
    private final Long id;
    private final String displayName;
    private final String email;
    private final int rating;
    private final boolean ratingEstablished;
    private final int provisionalMatchCount;
    private final Integer graduationYear;
    private final String skillLevel;
    private final String phone;
    private final boolean duesPaid;
    private final boolean active;
    private final Instant createdAt;

    private PlayerResponse(Player player) {
        id = player.getId();
        displayName = player.getDisplayName();
        email = player.getEmail();
        rating = player.getRating();
        ratingEstablished = player.isRatingEstablished();
        provisionalMatchCount = player.getProvisionalMatchCount();
        graduationYear = player.getGraduationYear();
        skillLevel = player.getSkillLevel();
        phone = player.getPhone();
        duesPaid = player.isDuesPaid();
        active = player.isActive();
        createdAt = player.getCreatedAt();
    }

    public static PlayerResponse from(Player player) { return new PlayerResponse(player); }
    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public int getRating() { return rating; }
    public boolean isRatingEstablished() { return ratingEstablished; }
    public int getProvisionalMatchCount() { return provisionalMatchCount; }
    public Integer getGraduationYear() { return graduationYear; }
    public String getSkillLevel() { return skillLevel; }
    public String getPhone() { return phone; }
    public boolean isDuesPaid() { return duesPaid; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
