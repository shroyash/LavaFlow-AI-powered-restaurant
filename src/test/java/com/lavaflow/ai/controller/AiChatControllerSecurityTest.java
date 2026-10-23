package com.lavaflow.ai.controller;

import com.lavaflow.ai.dto.AiChatRequest;
import com.lavaflow.ai.service.AiChatService;
import com.lavaflow.auth.entity.User;
import com.lavaflow.auth.security.UserPrincipal;
import com.lavaflow.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiChatControllerSecurityTest.TestSecurityConfig.class)
@WebAppConfiguration
@DisplayName("AiChatController HTTP Security and Access Control Tests")
class AiChatControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AiChatService aiChatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        reset(aiChatService);
    }

    private UserPrincipal createPrincipal(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test-" + role.name() + "@example.com");
        user.setRole(role);
        user.setActive(true);
        return new UserPrincipal(user);
    }

    @Test
    @DisplayName("Unauthenticated request to POST /api/v1/ai/chat returns 401 Unauthorized")
    void chat_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(aiChatService);
    }

    @Test
    @DisplayName("Authenticated RESTAURANT_ADMIN is rejected by method security with 403 Forbidden")
    void chat_authenticatedAsRestaurantAdmin_returns403() throws Exception {
        UserPrincipal adminPrincipal = createPrincipal(UserRole.RESTAURANT_ADMIN);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(aiChatService);
    }

    @Test
    @DisplayName("Authenticated KITCHEN_STAFF is rejected by method security with 403 Forbidden")
    void chat_authenticatedAsKitchenStaff_returns403() throws Exception {
        UserPrincipal kitchenPrincipal = createPrincipal(UserRole.KITCHEN_STAFF);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(kitchenPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(aiChatService);
    }

    @Test
    @DisplayName("Authenticated CUSTOMER is permitted through method security and returns 200 OK")
    void chat_authenticatedAsCustomer_returns200() throws Exception {
        UserPrincipal customerPrincipal = createPrincipal(UserRole.CUSTOMER);
        UUID conversationId = UUID.randomUUID();

        when(aiChatService.chat(any(User.class), isNull(), isNull(), eq("What is available?")))
                .thenReturn(new AiChatService.ChatResult(conversationId, "Here is our menu!"));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(customerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is available?\"}"))
                .andExpect(status().isOk());

        verify(aiChatService).chat(any(User.class), isNull(), isNull(), eq("What is available?"));
    }

    @Test
    @DisplayName("Blank message fails @Valid request body validation with 400 Bad Request")
    void chat_blankMessage_returns400() throws Exception {
        UserPrincipal customerPrincipal = createPrincipal(UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .with(user(customerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiChatService);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        public AiChatService aiChatService() {
            return mock(AiChatService.class);
        }

        @Bean
        public AiChatController aiChatController(AiChatService aiChatService) {
            return new AiChatController(aiChatService);
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
                            .requestMatchers("/api/v1/ai/chat").authenticated()
                            .anyRequest().permitAll()
                    );

            return http.build();
        }
    }
}

