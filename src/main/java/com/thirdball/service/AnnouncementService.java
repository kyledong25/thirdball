package com.thirdball.service;

import com.thirdball.api.request.SaveAnnouncementRequest;
import com.thirdball.api.response.AnnouncementResponse;
import com.thirdball.domain.Announcement;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listForAdministrators() {
        return announcementRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(AnnouncementResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listPublished() {
        return announcementRepository.findByPublishedTrueOrderByPublishedAtDescIdDesc().stream()
                .map(AnnouncementResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponse create(SaveAnnouncementRequest request) {
        Announcement announcement = new Announcement();
        apply(announcement, request);
        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementResponse update(Long announcementId, SaveAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new NotFoundException("Announcement " + announcementId + " was not found"));
        boolean publishingForFirstTime = !announcement.isPublished() && request.getPublished();
        apply(announcement, request);
        if (publishingForFirstTime) {
            announcement.setPublishedAt(Instant.now());
        }
        return AnnouncementResponse.from(announcement);
    }

    private void apply(Announcement announcement, SaveAnnouncementRequest request) {
        announcement.setTitle(request.getTitle().trim());
        announcement.setBody(request.getBody().trim());
        announcement.setPublished(request.getPublished());
    }
}
