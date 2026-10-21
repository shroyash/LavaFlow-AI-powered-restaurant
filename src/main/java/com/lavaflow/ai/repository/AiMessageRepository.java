package com.lavaflow.ai.repository;

import com.lavaflow.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiMessageRepository
        extends JpaRepository<AiMessage, UUID> {
}