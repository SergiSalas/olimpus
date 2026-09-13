package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case: write in the conversation of the day.
 *
 * <p>This is where the three rules that no screen can skip live: only the two
 * people inside write, only while it is open, and every message adds to its
 * side's count.
 */
public class SendMessage {

    /** The stored message and how the conversation looks afterwards. */
    public record Sent(Message message, Conversation conversation) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final Clock clock;

    public SendMessage(
            ConversationRepository conversations, MessageRepository messages, Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.clock = clock;
    }

    public Sent execute(UUID conversationId, UUID sender, String text) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);

        if (!conversation.involves(sender)) {
            throw new NotYourConversationException();
        }

        Instant now = clock.instant();
        if (!conversation.acceptsMessagesAt(now)) {
            throw conversation.isOpen()
                    ? ChatClosedException.timeOver()
                    : ChatClosedException.notOpen();
        }

        Message message = Message.written(conversationId, sender, text, now);
        messages.save(message);

        Conversation updated = conversation.withMessageFrom(sender);
        conversations.save(updated);

        return new Sent(message, updated);
    }
}
