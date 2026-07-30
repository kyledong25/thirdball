package com.thirdball.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thirdball.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sends transactional email through Brevo's HTTPS API. This avoids SMTP, whose
 * outbound ports are blocked on Render's free web-service plan.
 */
@Service
public class BrevoEmailClient {
    private static final Logger logger = LoggerFactory.getLogger(BrevoEmailClient.class);
    private static final String SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDER_NAME = "TAMU Table Tennis Club";
    private static final Pattern EMAIL_ADDRESS = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final String apiKey;

    public BrevoEmailClient(RestTemplateBuilder restTemplateBuilder,
                            @Value("${app.email.brevo.api-key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    /** Returns true only when production has supplied a Brevo API key. */
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /** Sends a plain-text email using Brevo's authenticated HTTPS endpoint. */
    public void sendPlainText(String fromAddress, String recipient, String subject, String textContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("api-key", apiKey);

        BrevoEmailRequest body = new BrevoEmailRequest(
                new EmailAddress(fromAddress, SENDER_NAME),
                Collections.singletonList(new EmailAddress(recipient, null)),
                subject,
                textContent
        );

        try {
            restTemplate.exchange(SEND_EMAIL_URL, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        } catch (RestClientResponseException exception) {
            logger.warn("Brevo rejected a verification email with HTTP status {}: {}",
                    exception.getRawStatusCode(), redactEmailAddresses(exception.getResponseBodyAsString()));
            throw new EmailDeliveryException();
        } catch (RestClientException exception) {
            logger.warn("Brevo verification-email request failed ({})", exception.getClass().getSimpleName());
            throw new EmailDeliveryException();
        }
    }

    /** Keeps provider diagnostics useful in Render while avoiding address disclosure in logs. */
    private String redactEmailAddresses(String text) {
        return EMAIL_ADDRESS.matcher(text).replaceAll("[redacted-email]");
    }

    /** JSON model accepted by Brevo's /v3/smtp/email endpoint. */
    private static final class BrevoEmailRequest {
        private final EmailAddress sender;
        private final List<EmailAddress> to;
        private final String subject;
        private final String textContent;

        private BrevoEmailRequest(EmailAddress sender, List<EmailAddress> to, String subject, String textContent) {
            this.sender = sender;
            this.to = to;
            this.subject = subject;
            this.textContent = textContent;
        }

        public EmailAddress getSender() { return sender; }
        public List<EmailAddress> getTo() { return to; }
        public String getSubject() { return subject; }
        public String getTextContent() { return textContent; }
    }

    /** Address object used for Brevo's sender and recipient payload fields. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class EmailAddress {
        private final String email;
        private final String name;

        private EmailAddress(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}
