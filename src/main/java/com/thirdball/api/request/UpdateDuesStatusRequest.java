package com.thirdball.api.request;

import javax.validation.constraints.NotNull;

/** Administrator-only paid/unpaid membership dues update. */
public class UpdateDuesStatusRequest {
    @NotNull
    private Boolean duesPaid;

    public Boolean getDuesPaid() { return duesPaid; }
    public void setDuesPaid(Boolean duesPaid) { this.duesPaid = duesPaid; }
}
