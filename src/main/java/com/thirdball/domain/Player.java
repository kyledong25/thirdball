package com.thirdball.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "players")
public class Player {
    public static final int UNRATED_RATING = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private int rating = UNRATED_RATING;

    @Column(name = "rating_established", nullable = false)
    private boolean ratingEstablished;

    @Column(name = "provisional_match_count", nullable = false)
    private int provisionalMatchCount;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "skill_level", length = 30)
    private String skillLevel;

    @Column(length = 30)
    private String phone;

    @Column(name = "dues_paid", nullable = false)
    private boolean duesPaid;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public boolean isRatingEstablished() { return ratingEstablished; }
    public void setRatingEstablished(boolean ratingEstablished) { this.ratingEstablished = ratingEstablished; }
    public int getProvisionalMatchCount() { return provisionalMatchCount; }
    public void setProvisionalMatchCount(int provisionalMatchCount) { this.provisionalMatchCount = provisionalMatchCount; }
    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isDuesPaid() { return duesPaid; }
    public void setDuesPaid(boolean duesPaid) { this.duesPaid = duesPaid; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
