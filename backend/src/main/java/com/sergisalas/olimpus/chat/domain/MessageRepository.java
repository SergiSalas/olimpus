package com.sergisalas.olimpus.chat.domain;

import java.util.List;
import java.util.UUID;

public interface MessageRepository {

    void save(Message message);

    /** Every message of a conversation, oldest first. */
    List<Message> byConversation(UUID conversationId);
}
