package com.lavaflow.customer.service;

import com.lavaflow.customer.dto.GeocodedAddress;

public interface AddressGeocodingService {

    GeocodedAddress geocode(String address);
}