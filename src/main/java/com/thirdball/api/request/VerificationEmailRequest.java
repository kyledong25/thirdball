package com.thirdball.api.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Identifies an account that needs a fresh verification code. */
public class VerificationEmailRequest {
    @NotBlank @Email @Size(max = 255)
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
