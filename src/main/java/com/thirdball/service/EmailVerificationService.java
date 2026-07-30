package com.thirdball.service;

import com.thirdball.domain.ClubUser;
import com.thirdball.exception.EmailDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

/** Creates, sends, and validates short-lived email verification codes. */
@Service
public class EmailVerificationService {
    private static final int CODE_BOUND = 1_000_000;

    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String fromAddress;
    private final long expirationMinutes;
    private final long resendCooldownSeconds;

    public EmailVerificationService(JavaMailSender mailSender,
                                    PasswordEncoder passwordEncoder,
                                    @Value("${app.email.from}") String fromAddress,
                                    @Value("${app.email.verification.code-expiration-minutes}") long expirationMinutes,
                                    @Value("${app.email.verification.resend-cooldown-seconds}") long resendCooldownSeconds) {
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.fromAddress = fromAddress;
        this.expirationMinutes = expirationMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    /** Assigns a new code and delivers it through the configured SMTP provider. */
    public void issueCode(ClubUser user) {
        Instant now = Instant.now();
        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(CODE_BOUND));
        user.setEmailVerificationCodeHash(passwordEncoder.encode(code));
        user.setEmailVerificationCodeExpiresAt(now.plusSeconds(expirationMinutes * 60));
        user.setEmailVerificationCodeSentAt(now);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Verify your TAMU Table Tennis account");
        message.setText("Your TAMU Table Tennis Club verification code is " + code
                + ". It expires in " + expirationMinutes + " minutes.\n\n"
                + "If you did not start this signup, you can ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new EmailDeliveryException();
        }
    }

    /** Silently ignores overly frequent resend requests to limit email abuse. */
    public void issueCodeWhenAllowed(ClubUser user) {
        Instant lastSentAt = user.getEmailVerificationCodeSentAt();
        if (lastSentAt == null || !lastSentAt.plusSeconds(resendCooldownSeconds).isAfter(Instant.now())) {
            issueCode(user);
        }
    }

    public boolean matchesActiveCode(ClubUser user, String code) {
        Instant expiresAt = user.getEmailVerificationCodeExpiresAt();
        String hash = user.getEmailVerificationCodeHash();
        return hash != null && expiresAt != null && expiresAt.isAfter(Instant.now())
                && passwordEncoder.matches(code, hash);
    }
}
