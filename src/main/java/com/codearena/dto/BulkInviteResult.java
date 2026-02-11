package com.codearena.dto;

public record BulkInviteResult(
    String email,
    String status
) {
    public static final String STATUS_INVITED = "invited";

    public static final String STATUS_ALREADY_INVITED = "already_invited";

    public static final String STATUS_INVALID_EMAIL = "invalid_email";

    public static BulkInviteResult invited(String email) {
        return new BulkInviteResult(email, STATUS_INVITED);
    }

    public static BulkInviteResult alreadyInvited(String email) {
        return new BulkInviteResult(email, STATUS_ALREADY_INVITED);
    }

    public static BulkInviteResult invalidEmail(String email) {
        return new BulkInviteResult(email, STATUS_INVALID_EMAIL);
    }

    public boolean isSuccess() {
        return STATUS_INVITED.equals(status);
    }

    public boolean isAlreadyInvited() {
        return STATUS_ALREADY_INVITED.equals(status);
    }

    public boolean isInvalidEmail() {
        return STATUS_INVALID_EMAIL.equals(status);
    }
}
