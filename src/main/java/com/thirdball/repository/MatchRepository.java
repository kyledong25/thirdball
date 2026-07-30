package com.thirdball.repository;

import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> findByIdForUpdate(@Param("id") Long id);

    List<Match> findByTournament_IdOrderByRoundNumberAscBracketSlotAsc(Long tournamentId);

    @Query("select m from Match m where m.status = :status "
            + "and (m.playerOne.id = :playerId or m.playerTwo.id = :playerId) "
            + "order by m.completedAt asc, m.id asc")
    List<Match> findCompletedForPlayer(@Param("playerId") Long playerId, @Param("status") MatchStatus status);
}
