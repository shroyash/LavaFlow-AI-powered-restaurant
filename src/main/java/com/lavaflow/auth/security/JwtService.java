package com.lavaflow.auth.security;

import com.lavaflow.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Service
public class JwtService {

    private static final String CLAIM_ROLE           = "role";
    private static final String CLAIM_RESTAURANT_ID  = "restaurantId";

    private final JwtProperties jwtProperties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtProperties jwtProperties, PrivateKey privateKey, PublicKey publicKey) {
        this.jwtProperties = jwtProperties;
        this.privateKey    = privateKey;
        this.publicKey     = publicKey;
    }

    public String generateAccessToken(UUID userId, UserRole role, UUID restaurantId) {
        Instant now        = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenExpiration());

        var builder = Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer("lavaflow-api")
                .setAudience("lavaflow-clients")
                .setSubject(userId.toString())
                .claim(CLAIM_ROLE, role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setId(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS256);

        if (restaurantId != null) {
            builder.claim(CLAIM_RESTAURANT_ID, restaurantId.toString());
        }

        return builder.compact();
    }


    public String generateRefreshTokenString() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpiration().getSeconds();
    }
}
