package com.lavaflow.auth.security;

import com.lavaflow.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();

        jwtProperties = new JwtProperties();
        // defaults: 15 min access, 7 day refresh

        jwtService = new JwtService(jwtProperties, keyPair.getPrivate(), keyPair.getPublic());
    }

    // ─── Test 4: JWT Generation ───────────────────────────────────────────────

    @Test
    @DisplayName("4. generateAccessToken returns non-null, non-blank JWT")
    void shouldGenerateAccessToken() {
        UUID userId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, UserRole.RESTAURANT_ADMIN, restaurantId);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("4b. generateAccessToken for SUPER_ADMIN (null restaurantId)")
    void shouldGenerateAccessTokenForSuperAdmin() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, UserRole.SUPER_ADMIN, null);

        assertThat(token).isNotBlank();
    }

    // ─── Test 5: JWT Signature Verification ───────────────────────────────────

    @Test
    @DisplayName("5. parseClaims verifies signature and extracts correct sub")
    void shouldVerifySignatureAndExtractSub() {
        UUID userId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, UserRole.KITCHEN_STAFF, restaurantId);
        var claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("KITCHEN_STAFF");
        assertThat(claims.get("restaurantId", String.class)).isEqualTo(restaurantId.toString());
    }

    @Test
    @DisplayName("5b. restaurantId claim is absent for SUPER_ADMIN")
    void shouldNotIncludeRestaurantIdForSuperAdmin() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, UserRole.SUPER_ADMIN, null);
        var claims = jwtService.parseClaims(token);

        assertThat(claims.get("restaurantId", String.class)).isNull();
    }

    // ─── Test 6: Expired JWT ──────────────────────────────────────────────────

    @Test
    @DisplayName("6. Expired JWT causes ExpiredJwtException")
    void shouldRejectExpiredToken() throws Exception {
        // Use a very short expiration via a modified JwtProperties
        JwtProperties shortProps = new JwtProperties();
        shortProps.setAccessTokenExpiration(java.time.Duration.ofMillis(1));
        JwtService shortJwtService = new JwtService(shortProps, keyPair.getPrivate(), keyPair.getPublic());

        String token = shortJwtService.generateAccessToken(UUID.randomUUID(), UserRole.CUSTOMER, null);
        Thread.sleep(10); // let it expire

        assertThatThrownBy(() -> shortJwtService.parseClaims(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    // ─── Test 7: Invalid / Tampered JWT ──────────────────────────────────────

    @Test
    @DisplayName("7. Tampered JWT signature causes JwtException")
    void shouldRejectTamperedToken() throws Exception {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), UserRole.CUSTOMER, null);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.parseClaims(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("7b. Token signed with different key causes JwtException")
    void shouldRejectTokenSignedWithWrongKey() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair otherPair = gen.generateKeyPair();

        JwtService otherService = new JwtService(jwtProperties, otherPair.getPrivate(), otherPair.getPublic());
        String tokenFromOtherKey = otherService.generateAccessToken(UUID.randomUUID(), UserRole.CUSTOMER, null);

        // Parse with original service (different public key) — should fail
        assertThatThrownBy(() -> jwtService.parseClaims(tokenFromOtherKey))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    // ─── Refresh Token String ─────────────────────────────────────────────────

    @Test
    @DisplayName("Refresh token string is 64 hex characters")
    void shouldGenerateValidRefreshTokenString() {
        String token = jwtService.generateRefreshTokenString();
        assertThat(token).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("Refresh token strings are unique")
    void refreshTokenStringsShouldBeUnique() {
        String t1 = jwtService.generateRefreshTokenString();
        String t2 = jwtService.generateRefreshTokenString();
        assertThat(t1).isNotEqualTo(t2);
    }
}
