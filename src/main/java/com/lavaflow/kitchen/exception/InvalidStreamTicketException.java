package com.lavaflow.kitchen.exception;

public class InvalidStreamTicketException extends RuntimeException {
    public InvalidStreamTicketException() {
        super("Invalid or expired stream ticket.");
    }
}
