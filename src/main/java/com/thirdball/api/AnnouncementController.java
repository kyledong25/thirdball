package com.thirdball.api;

import com.thirdball.api.request.SaveAnnouncementRequest;
import com.thirdball.api.response.AnnouncementResponse;
import com.thirdball.service.AnnouncementService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** Administrator announcement management routes. */
@RestController
@RequestMapping("/api/announcements")
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public List<AnnouncementResponse> list() {
        return announcementService.listForAdministrators();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody SaveAnnouncementRequest request) {
        return announcementService.create(request);
    }

    @PutMapping("/{announcementId}")
    public AnnouncementResponse update(@PathVariable Long announcementId,
                                       @Valid @RequestBody SaveAnnouncementRequest request) {
        return announcementService.update(announcementId, request);
    }
}
