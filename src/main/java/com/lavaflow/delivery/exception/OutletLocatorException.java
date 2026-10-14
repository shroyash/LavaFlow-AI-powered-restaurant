package com.lavaflow.delivery.exception;

import com.lavaflow.common.exception.ConflictException;
import com.lavaflow.delivery.enums.LocatorFailureReason;
import lombok.Getter;

@Getter
public class OutletLocatorException extends ConflictException {

    private final LocatorFailureReason reason;

    public OutletLocatorException(LocatorFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }
}