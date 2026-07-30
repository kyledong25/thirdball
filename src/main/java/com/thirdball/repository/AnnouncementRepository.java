package com.thirdball.repository;

import com.thirdball.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByCreatedAtDescIdDesc();
    List<Announcement> findByPublishedTrueOrderByPublishedAtDescIdDesc();
}
