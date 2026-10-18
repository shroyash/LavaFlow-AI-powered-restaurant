package com.lavaflow.kitchen.exception;

import com.lavaflow.common.enums.KitchenOrderStatus;
import com.lavaflow.common.exception.ConflictException;

public class InvalidKitchenOrderStatusException extends ConflictException {

    public InvalidKitchenOrderStatusException(KitchenOrderStatus from, KitchenOrderStatus to) {
        super("A kitchen order that is " + from + " cannot be changed to " + to + ".");
    }
}
