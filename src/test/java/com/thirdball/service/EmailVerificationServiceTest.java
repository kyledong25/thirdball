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

class EmailVerificationServiceTest {

    @Test
    void sendsAHashedSixDigitCodeThatCanBeVerifiedBeforeExpiry() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = new EmailVerificationService(mailSender,
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
}
