package com.lavaflow.restaurant.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RestaurantDocumentResponse {

    private String id;
    private String documentType;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private LocalDateTime uploadedAt;
}