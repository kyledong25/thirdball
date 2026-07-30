package com.thirdball.service;

import com.thirdball.api.request.SaveAnnouncementRequest;
import com.thirdball.api.response.AnnouncementResponse;
import com.thirdball.domain.Announcement;
import com.thirdball.repository.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {

    @Test
    void publishesDraftWhenAdministratorTurnsOnVisibility() {
        AnnouncementRepository repository = mock(AnnouncementRepository.class);
        AnnouncementService service = new AnnouncementService(repository);
        Announcement announcement = announcement(5L, false);
        SaveAnnouncementRequest request = request("Practice moved", "Practice is in REC 201.", true);
        when(repository.findById(5L)).thenReturn(Optional.of(announcement));

        AnnouncementResponse response = service.update(5L, request);

        assertTrue(response.isPublished());
        assertEquals("Practice moved", response.getTitle());
        assertEquals("Practice is in REC 201.", response.getBody());
        assertTrue(response.getPublishedAt().isAfter(Instant.parse("2026-07-01T00:00:00Z")));
    }

    private Announcement announcement(Long id, boolean published) {
        Announcement announcement = new Announcement();
        ReflectionTestUtils.setField(announcement, "id", id);
        ReflectionTestUtils.setField(announcement, "createdAt", Instant.parse("2026-07-01T00:00:00Z"));
        announcement.setTitle("Draft");
        announcement.setBody("Not ready yet.");
        announcement.setPublished(published);
        return announcement;
    }

    private SaveAnnouncementRequest request(String title, String body, boolean published) {
        SaveAnnouncementRequest request = new SaveAnnouncementRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setPublished(published);
        return request;
    }
}
