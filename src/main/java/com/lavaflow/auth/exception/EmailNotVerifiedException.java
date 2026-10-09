package com.lavaflow.auth.exception;


public class EmailNotVerifiedException extends AccountInactiveException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}