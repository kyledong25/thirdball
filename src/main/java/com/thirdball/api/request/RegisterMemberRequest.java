package com.thirdball.api.request;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Public registration payload. Registration can only create MEMBER accounts. */
public class RegisterMemberRequest {
    @NotBlank @Size(max = 100)
    private String displayName;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @NotBlank @Size(min = 12, max = 72)
    private String password;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
