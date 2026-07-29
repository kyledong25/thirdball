package com.thirdball.service;

import com.thirdball.api.request.CreatePracticeSessionRequest;
import com.thirdball.api.response.PracticeSessionResponse;
import com.thirdball.domain.Player;
import com.thirdball.domain.PracticeSession;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.PracticeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PracticeSessionService {
    private final PracticeSessionRepository practiceSessionRepository;
    private final PlayerRepository playerRepository;

    public PracticeSessionService(PracticeSessionRepository practiceSessionRepository,
                                  PlayerRepository playerRepository) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public PracticeSessionResponse create(CreatePracticeSessionRequest request) {
        validateSchedule(request.getStartsAt(), request.getEndsAt(), request.getRegistrationDeadline());
        PracticeSession session = new PracticeSession();
        session.setTitle(request.getTitle().trim());
        session.setDescription(request.getDescription());
        session.setLocation(request.getLocation().trim());
        session.setStartsAt(request.getStartsAt());
        session.setEndsAt(request.getEndsAt());
        session.setRegistrationDeadline(request.getRegistrationDeadline());
        session.setCapacity(request.getCapacity());
        return PracticeSessionResponse.from(practiceSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<PracticeSessionResponse> list() {
        return practiceSessionRepository.findAll().stream()
                .map(PracticeSessionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public PracticeSessionResponse registerPlayer(Long sessionId, Long playerId) {
        // Locking the session serializes capacity checks, preventing an overbooked block.
        PracticeSession session = practiceSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("Practice session " + sessionId + " was not found"));
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new NotFoundException("Player " + playerId + " was not found"));

        if (!player.isActive()) {
            throw new ConflictException("Inactive players cannot register for practice");
        }
        if (session.getRegistrationDeadline() != null && Instant.now().isAfter(session.getRegistrationDeadline())) {
            throw new ConflictException("Practice registration has closed");
        }
        if (session.getRegisteredPlayers().contains(player)) {
            throw new ConflictException("Player is already registered for this practice session");
        }
        if (session.getRegisteredPlayers().size() >= session.getCapacity()) {
            throw new ConflictException("Practice session is full");
        }

        session.getRegisteredPlayers().add(player);
        return PracticeSessionResponse.from(session);
    }

    private void validateSchedule(Instant startsAt, Instant endsAt, Instant registrationDeadline) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Practice end time must be after its start time");
        }
        if (registrationDeadline != null && registrationDeadline.isAfter(startsAt)) {
            throw new IllegalArgumentException("Registration deadline must be on or before the practice start time");
        }
    }
}
