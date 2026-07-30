package com.thirdball.service;

import com.thirdball.domain.ClubUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class EmailVerificationServiceTest {

    @Test
    void sendsAHashedSixDigitCodeThatCanBeVerifiedBeforeExpiry() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        BrevoEmailClient brevoEmailClient = mock(BrevoEmailClient.class);
        EmailVerificationService service = new EmailVerificationService(mailSender, brevoEmailClient,
                new BCryptPasswordEncoder(), "no-reply@tabletennis.tamu.edu", 15, 60);
        ClubUser user = new ClubUser();
        user.setEmail("member@tamu.edu");

        service.issueCode(user);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        Matcher codeMatcher = Pattern.compile("\\b(\\d{6})\\b").matcher(messageCaptor.getValue().getText());
        assertTrue(codeMatcher.find());
        assertNotNull(user.getEmailVerificationCodeHash());
        assertNotNull(user.getEmailVerificationCodeExpiresAt());
        assertTrue(service.matchesActiveCode(user, codeMatcher.group(1)));
    }

    @Test
    void usesBrevoWhenAnApiKeyIsConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        BrevoEmailClient brevoEmailClient = mock(BrevoEmailClient.class);
        when(brevoEmailClient.isConfigured()).thenReturn(true);
        EmailVerificationService service = new EmailVerificationService(mailSender, brevoEmailClient,
                new BCryptPasswordEncoder(), "no-reply@tabletennis.tamu.edu", 15, 60);
        ClubUser user = new ClubUser();
        user.setEmail("member@tamu.edu");

        service.issueCode(user);

        verify(brevoEmailClient).sendPlainText(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(mailSender);
    }
}
