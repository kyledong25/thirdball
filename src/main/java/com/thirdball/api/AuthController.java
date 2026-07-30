package com.thirdball.api;

import com.thirdball.api.request.RegisterMemberRequest;
import com.thirdball.api.request.VerificationEmailRequest;
import com.thirdball.api.request.VerifyEmailRequest;
import com.thirdball.api.response.AuthenticatedUserResponse;
import com.thirdball.api.response.EmailVerificationPendingResponse;
import com.thirdball.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** Authentication endpoints for the role-specific web application. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmailVerificationPendingResponse register(@Valid @RequestBody RegisterMemberRequest request) {
        return authenticationService.registerMember(request);
    }

    @PostMapping("/verify-email")
    public AuthenticatedUserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authenticationService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody VerificationEmailRequest request) {
        authenticationService.resendVerificationCode(request);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(Authentication authentication) {
        return authenticationService.currentUser(authentication);
    }
}
