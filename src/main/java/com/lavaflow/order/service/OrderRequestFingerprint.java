package com.lavaflow.order.service;

import com.lavaflow.order.dto.PlaceOrderRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderRequestFingerprint {

    public String of(PlaceOrderRequest request) {
        Map<UUID, Integer> mergedItems = new TreeMap<>();
        request.getItems().forEach(item -> mergedItems.merge(item.getMenuItemId(), item.getQuantity(), Integer::sum));

        String items = mergedItems.entrySet().stream()
                .map(entry -> entry.getKey() + "x" + entry.getValue())
                .collect(Collectors.joining(","));

        String instructions = request.getSpecialInstructions() == null
                ? ""
                : request.getSpecialInstructions().trim();

        String canonical = String.join("|",
                request.getRestaurantId().toString(),
                request.getAddressId().toString(),
                request.getPaymentMethod().name(),
                instructions,
                items
        );

        return sha256Hex(canonical);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
