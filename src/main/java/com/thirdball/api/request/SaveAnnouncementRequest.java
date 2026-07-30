package com.thirdball.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class SaveAnnouncementRequest {
    @NotBlank @Size(max = 150)
    private String title;
    @NotBlank @Size(max = 4000)
    private String body;
    @NotNull
    private Boolean published;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
}
