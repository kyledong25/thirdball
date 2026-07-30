package com.thirdball.service;

import com.thirdball.api.response.CalendarEventResponse;
import com.thirdball.repository.PracticeSessionRepository;
import com.thirdball.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ClubCalendarService {
    private final PracticeSessionRepository practiceSessionRepository;
    private final TournamentRepository tournamentRepository;

    public ClubCalendarService(PracticeSessionRepository practiceSessionRepository,
                               TournamentRepository tournamentRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> listUpcoming() {
        Instant now = Instant.now();
        return Stream.concat(
                        practiceSessionRepository.findAll().stream()
                                .filter(session -> session.getEndsAt().isAfter(now))
                                .map(CalendarEventResponse::from),
                        tournamentRepository.findAll().stream()
                                .filter(tournament -> tournament.getEndsAt().isAfter(now))
                                .map(CalendarEventResponse::from))
                .sorted(Comparator.comparing(CalendarEventResponse::getStartsAt))
                .collect(Collectors.toList());
    }
}
