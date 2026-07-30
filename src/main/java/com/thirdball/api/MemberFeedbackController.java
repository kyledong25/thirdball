package com.thirdball.api;

import com.thirdball.api.response.MemberFeedbackResponse;
import com.thirdball.service.MemberFeedbackService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Administrator access to feedback submitted from the member dashboard. */
@RestController
@RequestMapping("/api/feedback")
@PreAuthorize("hasRole('ADMIN')")
public class MemberFeedbackController {
    private final MemberFeedbackService memberFeedbackService;

    public MemberFeedbackController(MemberFeedbackService memberFeedbackService) {
        this.memberFeedbackService = memberFeedbackService;
    }

    @GetMapping
    public List<MemberFeedbackResponse> list() {
        return memberFeedbackService.list();
    }
}
