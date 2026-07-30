package com.thirdball.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SubmitFeedbackRequest {
    @NotBlank @Size(max = 150)
    private String subject;
    @NotBlank @Size(max = 4000)
    private String message;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
