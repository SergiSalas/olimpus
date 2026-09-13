package com.sergisalas.olimpus.chat.domain;

import java.util.List;
import java.util.UUID;

public interface MessageRepository {

    void save(Message message);

    /** Todos los mensajes de una conversacion, del mas viejo al mas nuevo. */
    List<Message> byConversation(UUID conversationId);
}
