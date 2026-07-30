package com.thirdball.service;

import com.thirdball.api.request.SubmitFeedbackRequest;
import com.thirdball.api.response.MemberFeedbackResponse;
import com.thirdball.domain.MemberFeedback;
import com.thirdball.domain.Player;
import com.thirdball.repository.MemberFeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberFeedbackServiceTest {

    @Test
    void submitsFeedbackLinkedToTheAuthenticatedMemberPlayer() {
        MemberFeedbackRepository repository = mock(MemberFeedbackRepository.class);
        MemberFeedbackService service = new MemberFeedbackService(repository);
        Player player = new Player();
        ReflectionTestUtils.setField(player, "id", 3L);
        player.setDisplayName("Maya Chen");
        SubmitFeedbackRequest request = new SubmitFeedbackRequest();
        request.setSubject("More beginner drills");
        request.setMessage("Could we add a beginner table once a week?");
        when(repository.save(any(MemberFeedback.class))).thenAnswer(invocation -> {
            MemberFeedback feedback = invocation.getArgument(0);
            ReflectionTestUtils.setField(feedback, "id", 9L);
            ReflectionTestUtils.setField(feedback, "submittedAt", Instant.parse("2026-07-30T18:00:00Z"));
            return feedback;
        });

        MemberFeedbackResponse response = service.submit(player, request);

        assertEquals(9L, response.getId());
        assertEquals("Maya Chen", response.getPlayerName());
        assertEquals("More beginner drills", response.getSubject());
    }
}
