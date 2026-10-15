package com.lavaflow.order.exception;

import com.lavaflow.common.enums.OrderStatus;
import com.lavaflow.common.exception.ConflictException;

public class InvalidOrderStatusException extends ConflictException {

    public InvalidOrderStatusException(OrderStatus from, OrderStatus to) {
        super("An order that is " + from + " cannot be changed to " + to + ".");
    }

    public InvalidOrderStatusException(String message) {
        super(message);
    }
}
