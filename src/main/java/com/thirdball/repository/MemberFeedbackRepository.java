package com.thirdball.repository;

import com.thirdball.domain.MemberFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberFeedbackRepository extends JpaRepository<MemberFeedback, Long> {
    List<MemberFeedback> findAllByOrderBySubmittedAtDescIdDesc();
}
