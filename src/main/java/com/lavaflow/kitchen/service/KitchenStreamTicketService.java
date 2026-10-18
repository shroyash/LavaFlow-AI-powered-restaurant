package com.lavaflow.kitchen.service;

import com.lavaflow.kitchen.config.KitchenStreamProperties;
import com.lavaflow.kitchen.dto.StreamTicketResponse;
import com.lavaflow.kitchen.exception.InvalidStreamTicketException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class KitchenStreamTicketService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final KitchenStreamProperties properties;
    private final byte[] secret;

    public KitchenStreamTicketService(KitchenStreamProperties properties) {
        this.properties = properties;
        this.secret = properties.getTicketSecret().getBytes(StandardCharsets.UTF_8);
    }

    public StreamTicketResponse issue(UUID restaurantId, UUID userId) {
        long expiresAt = Instant.now().getEpochSecond() + properties.getTicketTtlSeconds();
        byte[] payload = (restaurantId + "." + userId + "." + expiresAt).getBytes(StandardCharsets.UTF_8);

        String ticket = encode(payload) + "." + encode(sign(payload));

        return StreamTicketResponse.builder()
                .ticket(ticket)
                .expiresInSeconds(properties.getTicketTtlSeconds())
                .build();
    }

    public UUID verify(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new InvalidStreamTicketException();
        }

        try {
            String[] parts = ticket.split("\\.");
            if (parts.length != 2) {
                throw new InvalidStreamTicketException();
            }

            byte[] payload = decode(parts[0]);
            byte[] signature = decode(parts[1]);

            if (!MessageDigest.isEqual(sign(payload), signature)) {
                throw new InvalidStreamTicketException();
            }

            String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\.");
            if (fields.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(fields[2])) {
                throw new InvalidStreamTicketException();
            }

            return UUID.fromString(fields[0]);
        } catch (IllegalArgumentException e) {
            throw new InvalidStreamTicketException();
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign stream ticket", e);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
