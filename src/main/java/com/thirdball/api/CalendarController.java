package com.thirdball.api;

import com.thirdball.api.response.CalendarEventResponse;
import com.thirdball.service.ClubCalendarService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Shared upcoming-event feed for every authenticated club account. */
@RestController
@RequestMapping("/api/calendar")
@PreAuthorize("isAuthenticated()")
public class CalendarController {
    private final ClubCalendarService clubCalendarService;

    public CalendarController(ClubCalendarService clubCalendarService) {
        this.clubCalendarService = clubCalendarService;
    }

    @GetMapping
    public List<CalendarEventResponse> upcomingEvents() {
        return clubCalendarService.listUpcoming();
    }
}
