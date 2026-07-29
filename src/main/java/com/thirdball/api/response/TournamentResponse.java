package com.thirdball.api.response;

import com.thirdball.domain.Tournament;
import com.thirdball.domain.TournamentStatus;
import java.time.Instant;

public class TournamentResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String location;
    private final Instant startsAt;
    private final Instant endsAt;
    private final int maxParticipants;
    private final int registeredCount;
    private final TournamentStatus status;

    private TournamentResponse(Tournament tournament) {
        id = tournament.getId();
        name = tournament.getName();
        description = tournament.getDescription();
        location = tournament.getLocation();
        startsAt = tournament.getStartsAt();
        endsAt = tournament.getEndsAt();
        maxParticipants = tournament.getMaxParticipants();
        registeredCount = tournament.getParticipants().size();
        status = tournament.getStatus();
    }

    public static TournamentResponse from(Tournament tournament) { return new TournamentResponse(tournament); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public int getMaxParticipants() { return maxParticipants; }
    public int getRegisteredCount() { return registeredCount; }
    public TournamentStatus getStatus() { return status; }
}
