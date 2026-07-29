package com.thirdball.api;

import com.thirdball.api.request.CreatePracticeSessionRequest;
import com.thirdball.api.request.PlayerRegistrationRequest;
import com.thirdball.api.response.PracticeSessionResponse;
import com.thirdball.service.PracticeSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/practice-sessions")
public class PracticeSessionController {
    private final PracticeSessionService practiceSessionService;

    public PracticeSessionController(PracticeSessionService practiceSessionService) {
        this.practiceSessionService = practiceSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PracticeSessionResponse create(@Valid @RequestBody CreatePracticeSessionRequest request) {
        return practiceSessionService.create(request);
    }

    @GetMapping
    public List<PracticeSessionResponse> list() { return practiceSessionService.list(); }

    @PostMapping("/{sessionId}/registrations")
    public PracticeSessionResponse register(@PathVariable Long sessionId,
                                            @Valid @RequestBody PlayerRegistrationRequest request) {
        return practiceSessionService.registerPlayer(sessionId, request.getPlayerId());
    }
}
