package com.thirdball.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Login account for a club participant. A member account is linked to the
 * player record used for ratings and event registration; an administrator may
 * manage the club without being a participating player.
 */
@Entity
@Table(name = "club_users")
public class ClubUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubRole role;

    @OneToOne
    @JoinColumn(name = "player_id", unique = true)
    private Player player;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public ClubRole getRole() { return role; }
    public void setRole(ClubRole role) { this.role = role; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public Instant getCreatedAt() { return createdAt; }
}
