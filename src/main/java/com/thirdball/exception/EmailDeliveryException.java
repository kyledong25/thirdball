package com.thirdball.exception;

/** Indicates that the configured mail provider could not deliver an account code. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException() {
        super("We could not send the verification email. Please try again shortly.");
    }
}
