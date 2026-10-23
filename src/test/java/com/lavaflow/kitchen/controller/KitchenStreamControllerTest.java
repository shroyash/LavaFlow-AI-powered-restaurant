package com.lavaflow.kitchen.controller;

import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import com.lavaflow.kitchen.service.KitchenStreamRegistry;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.security.CurrentRestaurantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KitchenStreamControllerTest.TestSecurityConfig.class)
@WebAppConfiguration
@DisplayName("KitchenStreamController HTTP Security and Access Control Tests")
class KitchenStreamControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private KitchenStreamRegistry kitchenStreamRegistry;

    @Autowired
    private CurrentRestaurantResolver currentRestaurantResolver;

    private MockMvc mockMvc;

    private static final UUID RESTAURANT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        reset(kitchenStreamRegistry, currentRestaurantResolver);
    }

    // ── Helper factories ──────────────────────────────────────────────────────

    private UserPrincipal principalWithRole(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test-" + role.name().toLowerCase() + "@lavaflow.test");
        user.setRole(role);
        user.setActive(true);
        return new UserPrincipal(user);
    }

    private UserPrincipal principalWithRestaurant(UserRole role) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("staff-" + role.name().toLowerCase() + "@lavaflow.test");
        user.setRole(role);
        user.setActive(true);
        user.setRestaurant(restaurant);
        return new UserPrincipal(user);
    }

    // ── Unauthenticated ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated GET /api/v1/kitchen/stream returns 401")
    void stream_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(anonymous())
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(kitchenStreamRegistry, currentRestaurantResolver);
    }

    // ── Forbidden roles ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CUSTOMER role is rejected with 403")
    void stream_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principalWithRole(UserRole.CUSTOMER)))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(kitchenStreamRegistry, currentRestaurantResolver);
    }

    @Test
    @DisplayName("RIDER role is rejected with 403")
    void stream_riderRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principalWithRole(UserRole.RIDER)))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(kitchenStreamRegistry, currentRestaurantResolver);
    }

    @Test
    @DisplayName("SUPER_ADMIN role is rejected with 403")
    void stream_superAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principalWithRole(UserRole.SUPER_ADMIN)))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(kitchenStreamRegistry, currentRestaurantResolver);
    }

    // ── Authorized roles with restaurant ─────────────────────────────────────

    @Test
    @DisplayName("KITCHEN_STAFF with restaurant association returns 200 text/event-stream")
    void stream_kitchenStaffWithRestaurant_returns200() throws Exception {
        UserPrincipal principal = principalWithRestaurant(UserRole.KITCHEN_STAFF);
        when(currentRestaurantResolver.resolve(any())).thenReturn(RESTAURANT_ID);
        when(kitchenStreamRegistry.register(RESTAURANT_ID)).thenReturn(Optional.of(new SseEmitter()));

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));

        verify(currentRestaurantResolver).resolve(any());
        verify(kitchenStreamRegistry).register(RESTAURANT_ID);
    }

    @Test
    @DisplayName("RESTAURANT_ADMIN with restaurant association returns 200 text/event-stream")
    void stream_restaurantAdminWithRestaurant_returns200() throws Exception {
        UserPrincipal principal = principalWithRestaurant(UserRole.RESTAURANT_ADMIN);
        when(currentRestaurantResolver.resolve(any())).thenReturn(RESTAURANT_ID);
        when(kitchenStreamRegistry.register(RESTAURANT_ID)).thenReturn(Optional.of(new SseEmitter()));

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));

        verify(kitchenStreamRegistry).register(RESTAURANT_ID);
    }

    @Test
    @DisplayName("RESTAURANT_STAFF with restaurant association returns 200 text/event-stream")
    void stream_restaurantStaffWithRestaurant_returns200() throws Exception {
        UserPrincipal principal = principalWithRestaurant(UserRole.RESTAURANT_STAFF);
        when(currentRestaurantResolver.resolve(any())).thenReturn(RESTAURANT_ID);
        when(kitchenStreamRegistry.register(RESTAURANT_ID)).thenReturn(Optional.of(new SseEmitter()));

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM_VALUE));

        verify(kitchenStreamRegistry).register(RESTAURANT_ID);
    }

    // ── Missing restaurant association ────────────────────────────────────────

    @Test
    @DisplayName("KITCHEN_STAFF without restaurant association returns 403 (resolver throws AccessDeniedException)")
    void stream_kitchenStaffNoRestaurant_returns403() throws Exception {
        UserPrincipal principal = principalWithRole(UserRole.KITCHEN_STAFF);
        when(currentRestaurantResolver.resolve(any()))
                .thenThrow(new AccessDeniedException("You are not associated with a restaurant."));

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isForbidden());

        verify(currentRestaurantResolver).resolve(any());
        verifyNoInteractions(kitchenStreamRegistry);
    }

    // ── Connection limit ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Connection limit reached returns 429 Too Many Requests")
    void stream_connectionLimitReached_returns429() throws Exception {
        UserPrincipal principal = principalWithRestaurant(UserRole.KITCHEN_STAFF);
        when(currentRestaurantResolver.resolve(any())).thenReturn(RESTAURANT_ID);
        when(kitchenStreamRegistry.register(RESTAURANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isTooManyRequests());
    }

    // ── Tenant isolation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Emitter is registered under the resolver-provided restaurant ID, not a client-supplied value")
    void stream_emitterRegisteredUnderResolvedRestaurantId() throws Exception {
        UUID resolvedId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UserPrincipal principal = principalWithRestaurant(UserRole.KITCHEN_STAFF);
        when(currentRestaurantResolver.resolve(any())).thenReturn(resolvedId);
        when(kitchenStreamRegistry.register(resolvedId)).thenReturn(Optional.of(new SseEmitter()));

        mockMvc.perform(get("/api/v1/kitchen/stream")
                        .with(user(principal))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        // The registry must be called with the server-resolved ID only
        verify(kitchenStreamRegistry).register(resolvedId);
        verify(kitchenStreamRegistry, never()).register(argThat(id -> !id.equals(resolvedId)));
    }

    // ── Inner test configuration ──────────────────────────────────────────────

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        public KitchenStreamRegistry kitchenStreamRegistry() {
            return mock(KitchenStreamRegistry.class);
        }

        @Bean
        public CurrentRestaurantResolver currentRestaurantResolver() {
            return mock(CurrentRestaurantResolver.class);
        }

        @Bean
        public KitchenStreamController kitchenStreamController(
                KitchenStreamRegistry registry,
                CurrentRestaurantResolver resolver
        ) {
            return new KitchenStreamController(registry, resolver);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            AuthenticationEntryPoint entryPoint = (request, response, authException) ->
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);

            AccessDeniedHandler accessDeniedHandler = (request, response, accessDeniedException) ->
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);

            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint(entryPoint)
                            .accessDeniedHandler(accessDeniedHandler)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/kitchen/stream").authenticated()
                            .anyRequest().permitAll()
                    );

            return http.build();
        }
    }
}

