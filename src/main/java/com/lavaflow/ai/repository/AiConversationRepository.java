package com.lavaflow.ai.repository;

import com.lavaflow.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository
        extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByIdAndCustomer_Id(
            UUID conversationId,
            UUID customerId
    );
}