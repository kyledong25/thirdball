package com.thirdball.repository;

import com.thirdball.domain.ClubUser;
import com.thirdball.domain.ClubRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClubUserRepository extends JpaRepository<ClubUser, Long> {
    Optional<ClubUser> findByEmail(String email);
    boolean existsByRole(ClubRole role);
    boolean existsByPlayer_Id(Long playerId);
}
