package com.thirdball.repository;

import com.thirdball.domain.MemberMatchResultProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MemberMatchResultProposalRepository extends JpaRepository<MemberMatchResultProposal, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MemberMatchResultProposal p where p.id = :id")
    Optional<MemberMatchResultProposal> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from MemberMatchResultProposal p "
            + "where p.reporter.id = :playerId or p.opponent.id = :playerId "
            + "order by p.proposedAt desc, p.id desc")
    List<MemberMatchResultProposal> findForPlayer(@Param("playerId") Long playerId);
}
