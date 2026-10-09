package com.lavaflow.restaurant.dto;

import org.springframework.core.io.Resource;

public record RestaurantDocumentDownload(Resource resource, String contentType, String fileName) {
}