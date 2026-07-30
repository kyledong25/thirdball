package com.thirdball.service;

import com.thirdball.api.request.SubmitFeedbackRequest;
import com.thirdball.api.response.MemberFeedbackResponse;
import com.thirdball.domain.MemberFeedback;
import com.thirdball.domain.Player;
import com.thirdball.repository.MemberFeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberFeedbackService {
    private final MemberFeedbackRepository memberFeedbackRepository;

    public MemberFeedbackService(MemberFeedbackRepository memberFeedbackRepository) {
        this.memberFeedbackRepository = memberFeedbackRepository;
    }

    @Transactional
    public MemberFeedbackResponse submit(Player player, SubmitFeedbackRequest request) {
        MemberFeedback feedback = new MemberFeedback();
        feedback.setPlayer(player);
        feedback.setSubject(request.getSubject().trim());
        feedback.setMessage(request.getMessage().trim());
        return MemberFeedbackResponse.from(memberFeedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public List<MemberFeedbackResponse> list() {
        return memberFeedbackRepository.findAllByOrderBySubmittedAtDescIdDesc().stream()
                .map(MemberFeedbackResponse::from).collect(Collectors.toList());
    }
}
