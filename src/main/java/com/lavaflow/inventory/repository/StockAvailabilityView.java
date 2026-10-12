package com.lavaflow.inventory.repository;

import java.util.UUID;

public interface StockAvailabilityView {

    UUID getOutletId();

    UUID getMenuItemId();

    Integer getAvailable();
}