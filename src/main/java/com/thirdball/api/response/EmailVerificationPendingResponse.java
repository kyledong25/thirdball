package com.thirdball.api.response;

import com.thirdball.domain.ClubUser;

import java.time.Instant;

/** Returned after signup without exposing the verification code itself. */
public class EmailVerificationPendingResponse {
    private final String email;
    private final Instant expiresAt;

    private EmailVerificationPendingResponse(ClubUser user) {
        email = user.getEmail();
        expiresAt = user.getEmailVerificationCodeExpiresAt();
    }

    public static EmailVerificationPendingResponse from(ClubUser user) {
        return new EmailVerificationPendingResponse(user);
    }

    public String getEmail() { return email; }
    public Instant getExpiresAt() { return expiresAt; }
}
