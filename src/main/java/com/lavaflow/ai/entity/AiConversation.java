package com.lavaflow.ai.entity;

import com.lavaflow.auth.entity.User;
import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.ConversationStatus;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_conversations")
@Getter
@Setter
@NoArgsConstructor
public class AiConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(name = "title", length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ConversationStatus status = ConversationStatus.ACTIVE;
}
