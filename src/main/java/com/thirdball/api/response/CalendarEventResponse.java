package com.thirdball.api.response;

import com.thirdball.domain.PracticeSession;
import com.thirdball.domain.Tournament;

import java.time.Instant;

/** Normalized practice or tournament entry in the club-wide calendar. */
public class CalendarEventResponse {
    private final Long id;
    private final String type;
    private final String title;
    private final String description;
    private final String location;
    private final Instant startsAt;
    private final Instant endsAt;

    private CalendarEventResponse(Long id, String type, String title, String description,
                                  String location, Instant startsAt, Instant endsAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.location = location;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static CalendarEventResponse from(PracticeSession session) {
        return new CalendarEventResponse(session.getId(), "PRACTICE", session.getTitle(), session.getDescription(),
                session.getLocation(), session.getStartsAt(), session.getEndsAt());
    }

    public static CalendarEventResponse from(Tournament tournament) {
        return new CalendarEventResponse(tournament.getId(), "TOURNAMENT", tournament.getName(),
                tournament.getDescription(), tournament.getLocation(), tournament.getStartsAt(), tournament.getEndsAt());
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
}
