package com.thirdball.service;

import com.thirdball.api.request.CreateTournamentRequest;
import com.thirdball.api.response.TournamentResponse;
import com.thirdball.domain.Player;
import com.thirdball.domain.Match;
import com.thirdball.domain.MatchStatus;
import com.thirdball.domain.Tournament;
import com.thirdball.domain.TournamentStatus;
import com.thirdball.exception.ConflictException;
import com.thirdball.exception.NotFoundException;
import com.thirdball.repository.PlayerRepository;
import com.thirdball.repository.MatchRepository;
import com.thirdball.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.Instant;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public TournamentService(TournamentRepository tournamentRepository, PlayerRepository playerRepository,
                             MatchRepository matchRepository) {
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public TournamentResponse create(CreateTournamentRequest request) {
        validateSchedule(request.getStartsAt(), request.getEndsAt());
        Tournament tournament = new Tournament();
        tournament.setName(request.getName().trim());
        tournament.setDescription(request.getDescription());
        tournament.setLocation(request.getLocation());
        tournament.setStartsAt(request.getStartsAt());
        tournament.setEndsAt(request.getEndsAt());
        tournament.setMaxParticipants(request.getMaxParticipants());
        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        return TournamentResponse.from(tournamentRepository.save(tournament));
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> list() {
        return tournamentRepository.findAll().stream()
                .map(TournamentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> listUpcoming() {
        Instant now = Instant.now();
        return tournamentRepository.findAll().stream()
                .filter(tournament -> tournament.getEndsAt().isAfter(now))
                .sorted((first, second) -> first.getStartsAt().compareTo(second.getStartsAt()))
                .map(TournamentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TournamentResponse registerPlayer(Long tournamentId, Long playerId) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament " + tournamentId + " was not found"));
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new ConflictException("Tournament registration is not open");
        }

        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new NotFoundException("Player " + playerId + " was not found"));
        if (!player.isActive()) {
            throw new ConflictException("Inactive players cannot register for tournaments");
        }
        if (tournament.getParticipants().contains(player)) {
            throw new ConflictException("Player is already registered for this tournament");
        }
        if (tournament.getParticipants().size() >= tournament.getMaxParticipants()) {
            throw new ConflictException("Tournament registration is full");
        }

        tournament.getParticipants().add(player);
        return TournamentResponse.from(tournament);
    }

    /**
     * Creates a complete single-elimination tree from the current field. Players
     * are seeded by established rating (highest first); unrated players follow
     * in registration order. The standard seed layout keeps top seeds apart
     * until the later rounds and any opening-round byes advance automatically.
     */
    @Transactional
    public TournamentResponse generateBracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament " + tournamentId + " was not found"));
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new ConflictException("A bracket can only be generated while registration is open");
        }
        if (matchRepository.existsByTournament_Id(tournamentId)) {
            throw new ConflictException("This tournament already has bracket matches");
        }

        List<Player> seededPlayers = tournament.getParticipants().stream()
                .filter(Player::isActive)
                .sorted(Comparator.comparing(Player::isRatingEstablished).reversed()
                        .thenComparing(Player::getRating, Comparator.reverseOrder())
                        .thenComparing(Player::getId))
                .collect(Collectors.toList());
        if (seededPlayers.size() < 2) {
            throw new ConflictException("At least two active registered players are required to generate a bracket");
        }

        int bracketSize = nextPowerOfTwo(seededPlayers.size());
        List<Integer> bracketSeeds = standardSeedOrder(bracketSize);
        List<List<Match>> rounds = new ArrayList<>();

        List<Match> firstRound = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < bracketSize / 2; matchIndex++) {
            Match match = newBracketMatch(tournament, 1, matchIndex + 1);
            int playerOneSeed = bracketSeeds.get(matchIndex * 2);
            int playerTwoSeed = bracketSeeds.get(matchIndex * 2 + 1);
            if (playerOneSeed <= seededPlayers.size()) {
                match.setPlayerOne(seededPlayers.get(playerOneSeed - 1));
            }
            if (playerTwoSeed <= seededPlayers.size()) {
                match.setPlayerTwo(seededPlayers.get(playerTwoSeed - 1));
            }
            firstRound.add(match);
        }
        rounds.add(firstRound);

        int matchesInRound = bracketSize / 4;
        int roundNumber = 2;
        while (matchesInRound >= 1) {
            List<Match> round = new ArrayList<>();
            for (int matchIndex = 0; matchIndex < matchesInRound; matchIndex++) {
                round.add(newBracketMatch(tournament, roundNumber, matchIndex + 1));
            }
            rounds.add(round);
            matchesInRound /= 2;
            roundNumber++;
        }

        List<Match> allMatches = rounds.stream().flatMap(List::stream).collect(Collectors.toList());
        matchRepository.saveAll(allMatches);
        matchRepository.flush();

        for (int roundIndex = 0; roundIndex < rounds.size() - 1; roundIndex++) {
            List<Match> currentRound = rounds.get(roundIndex);
            List<Match> nextRound = rounds.get(roundIndex + 1);
            for (int matchIndex = 0; matchIndex < currentRound.size(); matchIndex++) {
                Match current = currentRound.get(matchIndex);
                current.setNextMatch(nextRound.get(matchIndex / 2));
                current.setNextMatchPlayerSlot((matchIndex % 2) + 1);
            }
        }

        for (Match match : firstRound) {
            advanceOpeningRoundBye(match);
        }
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        return TournamentResponse.from(tournament);
    }

    private Match newBracketMatch(Tournament tournament, int roundNumber, int bracketSlot) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRoundNumber(roundNumber);
        match.setBracketSlot(bracketSlot);
        return match;
    }

    private void advanceOpeningRoundBye(Match match) {
        Player advancingPlayer = match.getPlayerOne() == null ? match.getPlayerTwo()
                : match.getPlayerTwo() == null ? match.getPlayerOne() : null;
        if (advancingPlayer == null) {
            return;
        }
        match.setWinner(advancingPlayer);
        match.setStatus(MatchStatus.BYE);
        match.setCompletedAt(Instant.now());
        if (match.getNextMatchPlayerSlot() == 1) {
            match.getNextMatch().setPlayerOne(advancingPlayer);
        } else {
            match.getNextMatch().setPlayerTwo(advancingPlayer);
        }
    }

    private int nextPowerOfTwo(int number) {
        int value = 1;
        while (value < number) {
            value *= 2;
        }
        return value;
    }

    private List<Integer> standardSeedOrder(int bracketSize) {
        List<Integer> seeds = new ArrayList<>();
        seeds.add(1);
        seeds.add(2);
        while (seeds.size() < bracketSize) {
            int nextSize = seeds.size() * 2;
            List<Integer> nextSeeds = new ArrayList<>();
            for (Integer seed : seeds) {
                nextSeeds.add(seed);
                nextSeeds.add(nextSize + 1 - seed);
            }
            seeds = nextSeeds;
        }
        return seeds;
    }

    private void validateSchedule(java.time.Instant startsAt, java.time.Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Tournament end time must be after its start time");
        }
    }
}
