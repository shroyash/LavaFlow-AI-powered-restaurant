package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.NearbyRestaurantToolResponse;

import java.util.List;

public final class NearbyRestaurantResultCapture {

    private List<NearbyRestaurantToolResponse> restaurants = List.of();

    public List<NearbyRestaurantToolResponse> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(List<NearbyRestaurantToolResponse> restaurants) {
        this.restaurants = restaurants == null ? List.of() : List.copyOf(restaurants);
    }
}