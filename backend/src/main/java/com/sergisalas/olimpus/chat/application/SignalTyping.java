package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: "I'm writing". Nothing is stored: it only checks that whoever says
 * it can write in that conversation right now, and returns the conversation so
 * the other person can be told.
 */
public class SignalTyping {

    private final ConversationRepository conversations;
    private final Clock clock;

    public SignalTyping(ConversationRepository conversations, Clock clock) {
        this.conversations = conversations;
        this.clock = clock;
    }

    public Conversation execute(UUID conversationId, UUID writer) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(writer)) {
            throw new NotYourConversationException();
        }
        if (!conversation.acceptsMessagesAt(clock.instant())) {
            throw conversation.isOpen()
                    ? ChatClosedException.timeOver()
                    : ChatClosedException.notOpen();
        }
        return conversation;
    }
}
