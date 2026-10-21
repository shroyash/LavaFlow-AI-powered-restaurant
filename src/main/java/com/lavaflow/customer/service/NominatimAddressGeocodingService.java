package com.lavaflow.customer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.customer.dto.GeocodedAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@Service
public class NominatimAddressGeocodingService
        implements AddressGeocodingService {

    private final RestClient restClient;

    public NominatimAddressGeocodingService(
            @Value("${lavaflow.app.geocoding.nominatim.base-url}")
            String baseUrl,
            @Value("${lavaflow.app.geocoding.nominatim.user-agent}")
            String userAgent
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    @Override
    public GeocodedAddress geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new BadRequestException(
                    "Delivery address must not be empty."
            );
        }

        URI uri = UriComponentsBuilder
                .fromPath("/search")
                .queryParam("q", address.trim() + ", Nepal")
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .build()
                .encode()
                .toUri();

        List<NominatimResult> results;

        try {
            results = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, response) -> {
                                throw new GeocodingProviderException(
                                        "Geocoding provider returned HTTP "
                                                + response.getStatusCode()
                                );
                            }
                    )
                    .body(
                            new ParameterizedTypeReference<
                                    List<NominatimResult>>() {
                            }
                    );

        } catch (RestClientResponseException ex) {
            throw new GeocodingProviderException(
                    "Geocoding provider request failed.", ex
            );
        } catch (RestClientException ex) {
            throw new GeocodingProviderException(
                    "Could not connect to the geocoding provider.", ex
            );
        }

        if (results == null || results.isEmpty()) {
            throw new BadRequestException(
                    "Address could not be located. "
                            + "Please provide a more specific address."
            );
        }

        NominatimResult result = results.get(0);

        try {
            BigDecimal latitude = new BigDecimal(result.lat());
            BigDecimal longitude = new BigDecimal(result.lon());

            validateCoordinates(latitude, longitude);

            return new GeocodedAddress(
                    latitude,
                    longitude,
                    result.displayName()
            );

        } catch (NumberFormatException ex) {
            throw new GeocodingProviderException(
                    "Geocoding provider returned invalid coordinates.",
                    ex
            );
        }
    }

    private void validateCoordinates(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new GeocodingProviderException(
                    "Geocoding provider returned an invalid latitude."
            );
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new GeocodingProviderException(
                    "Geocoding provider returned an invalid longitude."
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(
            String lat,
            String lon,
            @JsonProperty("display_name")
            String displayName
    ) {
    }

    public static class GeocodingProviderException
            extends RuntimeException {

        public GeocodingProviderException(String message) {
            super(message);
        }

        public GeocodingProviderException(
                String message,
                Throwable cause
        ) {
            super(message, cause);
        }
    }
}