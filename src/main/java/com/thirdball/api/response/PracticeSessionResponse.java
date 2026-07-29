package com.thirdball.api.response;

import com.thirdball.domain.PracticeSession;
import java.time.Instant;

public class PracticeSessionResponse {
    private final Long id;
    private final String title;
    private final String description;
    private final String location;
    private final Instant startsAt;
    private final Instant endsAt;
    private final Instant registrationDeadline;
    private final int capacity;
    private final int registeredCount;

    private PracticeSessionResponse(PracticeSession session) {
        id = session.getId();
        title = session.getTitle();
        description = session.getDescription();
        location = session.getLocation();
        startsAt = session.getStartsAt();
        endsAt = session.getEndsAt();
        registrationDeadline = session.getRegistrationDeadline();
        capacity = session.getCapacity();
        registeredCount = session.getRegisteredPlayers().size();
    }

    public static PracticeSessionResponse from(PracticeSession session) { return new PracticeSessionResponse(session); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getRegistrationDeadline() { return registrationDeadline; }
    public int getCapacity() { return capacity; }
    public int getRegisteredCount() { return registeredCount; }
}
