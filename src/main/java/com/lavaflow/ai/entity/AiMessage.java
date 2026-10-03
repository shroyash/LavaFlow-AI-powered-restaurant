package com.lavaflow.ai.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.common.enums.MessageSender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_messages")
@Getter
@Setter
@NoArgsConstructor
public class AiMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 50)
    private MessageSender sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_input", columnDefinition = "jsonb")
    private String toolInput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_output", columnDefinition = "jsonb")
    private String toolOutput;
}
