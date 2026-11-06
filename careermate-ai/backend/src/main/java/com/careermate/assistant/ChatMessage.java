package com.careermate.assistant;

import com.careermate.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    /** USER or ASSISTANT. */
    @Column(name = "sender", nullable = false, length = 20)
    private String sender;

    @Column(nullable = false, length = 12000)
    private String content;

    @Column(name = "ai_powered", nullable = false)
    private boolean aiPowered;
}
