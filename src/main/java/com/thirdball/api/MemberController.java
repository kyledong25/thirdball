package com.thirdball.api;

import com.thirdball.api.response.PracticeSessionResponse;
import com.thirdball.api.response.AnnouncementResponse;
import com.thirdball.api.response.LadderPlayerResponse;
import com.thirdball.api.response.MemberMatchResultProposalResponse;
import com.thirdball.api.response.MemberFeedbackResponse;
import com.thirdball.api.response.PlayerResponse;
import com.thirdball.api.response.RatingHistoryPointResponse;
import com.thirdball.api.response.TournamentResponse;
import com.thirdball.api.request.UpdateMemberProfileRequest;
import com.thirdball.api.request.ProposeMemberMatchResultRequest;
import com.thirdball.api.request.SubmitFeedbackRequest;
import com.thirdball.domain.Player;
import com.thirdball.service.AuthenticationService;
import com.thirdball.service.AnnouncementService;
import com.thirdball.service.MemberLadderService;
import com.thirdball.service.MemberFeedbackService;
import com.thirdball.service.MemberMatchResultService;
import com.thirdball.service.PracticeSessionService;
import com.thirdball.service.MemberRatingHistoryService;
import com.thirdball.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import javax.validation.Valid;

/** General-member schedule and self-service event registration routes. */
@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasRole('MEMBER')")
public class MemberController {
    private final AuthenticationService authenticationService;
    private final PracticeSessionService practiceSessionService;
    private final TournamentService tournamentService;
    private final MemberRatingHistoryService memberRatingHistoryService;
    private final MemberLadderService memberLadderService;
    private final MemberMatchResultService memberMatchResultService;
    private final AnnouncementService announcementService;
    private final MemberFeedbackService memberFeedbackService;

    public MemberController(AuthenticationService authenticationService,
                            PracticeSessionService practiceSessionService,
                            TournamentService tournamentService,
                            MemberRatingHistoryService memberRatingHistoryService,
                            MemberLadderService memberLadderService,
                            MemberMatchResultService memberMatchResultService,
                            AnnouncementService announcementService,
                            MemberFeedbackService memberFeedbackService) {
        this.authenticationService = authenticationService;
        this.practiceSessionService = practiceSessionService;
        this.tournamentService = tournamentService;
        this.memberRatingHistoryService = memberRatingHistoryService;
        this.memberLadderService = memberLadderService;
        this.memberMatchResultService = memberMatchResultService;
        this.announcementService = announcementService;
        this.memberFeedbackService = memberFeedbackService;
    }

    @GetMapping("/practice-sessions")
    public List<PracticeSessionResponse> upcomingPracticeSessions() {
        return practiceSessionService.listUpcoming();
    }

    @GetMapping("/tournaments")
    public List<TournamentResponse> upcomingTournaments() {
        return tournamentService.listUpcoming();
    }

    @GetMapping("/profile")
    public PlayerResponse profile(Authentication authentication) {
        return PlayerResponse.from(authenticationService.currentMemberPlayer(authentication));
    }

    @PutMapping("/profile")
    public PlayerResponse updateProfile(@Valid @RequestBody UpdateMemberProfileRequest request,
                                        Authentication authentication) {
        return authenticationService.updateMemberProfile(authentication, request);
    }

    @GetMapping("/rating-history")
    public List<RatingHistoryPointResponse> ratingHistory(Authentication authentication) {
        return memberRatingHistoryService.listFor(authenticationService.currentMemberPlayer(authentication));
    }

    @GetMapping("/ladder")
    public List<LadderPlayerResponse> ladder() {
        return memberLadderService.list();
    }

    @GetMapping("/announcements")
    public List<AnnouncementResponse> announcements() {
        return announcementService.listPublished();
    }

    @PostMapping("/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberFeedbackResponse submitFeedback(@Valid @RequestBody SubmitFeedbackRequest request,
                                                  Authentication authentication) {
        return memberFeedbackService.submit(authenticationService.currentMemberPlayer(authentication), request);
    }

    @GetMapping("/match-results")
    public List<MemberMatchResultProposalResponse> matchResults(Authentication authentication) {
        return memberMatchResultService.listFor(authenticationService.currentMemberPlayer(authentication));
    }

    @PostMapping("/match-results")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberMatchResultProposalResponse proposeMatchResult(
            @Valid @RequestBody ProposeMemberMatchResultRequest request, Authentication authentication) {
        return memberMatchResultService.propose(authenticationService.currentMemberPlayer(authentication), request);
    }

    @PostMapping("/match-results/{proposalId}/agree")
    public MemberMatchResultProposalResponse agreeMatchResult(@PathVariable Long proposalId, Authentication authentication) {
        return memberMatchResultService.agree(proposalId, authenticationService.currentMemberPlayer(authentication));
    }

    @PostMapping("/match-results/{proposalId}/decline")
    public MemberMatchResultProposalResponse declineMatchResult(@PathVariable Long proposalId, Authentication authentication) {
        return memberMatchResultService.decline(proposalId, authenticationService.currentMemberPlayer(authentication));
    }

    @PostMapping("/practice-sessions/{sessionId}/signup")
    public PracticeSessionResponse signUpForPractice(@PathVariable Long sessionId, Authentication authentication) {
        Player player = authenticationService.currentMemberPlayer(authentication);
        return practiceSessionService.registerPlayer(sessionId, player.getId());
    }

    @PostMapping("/tournaments/{tournamentId}/signup")
    public TournamentResponse signUpForTournament(@PathVariable Long tournamentId, Authentication authentication) {
        Player player = authenticationService.currentMemberPlayer(authentication);
        return tournamentService.registerPlayer(tournamentId, player.getId());
    }
}
