package com.prashant.api.ecom.ducart.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("Too many requests. Please try again in 15 minutes.");
    }
}
