package com.lavaflow.kitchen.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreamTicketResponse {

    private String ticket;
    private long expiresInSeconds;
}
