package com.thirdball.service;

import com.thirdball.api.response.LadderPlayerResponse;
import com.thirdball.domain.Player;
import com.thirdball.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Provides a read-only, privacy-limited view of the active club ladder for members. */
@Service
public class MemberLadderService {
    private final PlayerRepository playerRepository;

    public MemberLadderService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<LadderPlayerResponse> list() {
        return playerRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Player::isRatingEstablished).reversed()
                        .thenComparing(Comparator.comparingInt(Player::getRating).reversed())
                        .thenComparing(Player::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(LadderPlayerResponse::from)
                .collect(Collectors.toList());
    }
}
