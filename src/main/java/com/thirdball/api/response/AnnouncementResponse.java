package com.thirdball.api.response;

import com.thirdball.domain.Announcement;
import java.time.Instant;

public class AnnouncementResponse {
    private final Long id;
    private final String title;
    private final String body;
    private final boolean published;
    private final Instant publishedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private AnnouncementResponse(Announcement announcement) {
        id = announcement.getId();
        title = announcement.getTitle();
        body = announcement.getBody();
        published = announcement.isPublished();
        publishedAt = announcement.getPublishedAt();
        createdAt = announcement.getCreatedAt();
        updatedAt = announcement.getUpdatedAt();
    }

    public static AnnouncementResponse from(Announcement announcement) { return new AnnouncementResponse(announcement); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
