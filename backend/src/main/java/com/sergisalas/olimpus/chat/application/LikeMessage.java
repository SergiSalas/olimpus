package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.chat.domain.ChatClosedException;
import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageLikes;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Use case: put a heart on a message, or take it off.
 *
 * <p>Only on the other person's messages: a heart on your own says nothing. And
 * only while the chat still accepts messages, for the same reason nobody can
 * write in a closed one. It never counts towards the unlock levels: those need
 * turns, and a heart is not a turn.
 */
public class LikeMessage {

    /** The message, how the conversation stands and whether it now has the heart. */
    public record Liked(Message message, Conversation conversation, boolean liked) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final MessageLikes likes;
    private final Clock clock;

    public LikeMessage(
            ConversationRepository conversations,
            MessageRepository messages,
            MessageLikes likes,
            Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.likes = likes;
        this.clock = clock;
    }

    public Liked execute(UUID conversationId, UUID liker, UUID messageId, boolean liked) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(liker)) {
            throw new NotYourConversationException();
        }

        Instant now = clock.instant();
        if (!conversation.acceptsMessagesAt(now)) {
            throw conversation.isOpen()
                    ? ChatClosedException.timeOver()
                    : ChatClosedException.notOpen();
        }

        // Looked up inside this conversation: a message id from another chat is
        // treated as not existing, not as a different error that would confirm it.
        Message message =
                messages.byConversation(conversationId).stream()
                        .filter(m -> m.id().equals(messageId))
                        .findFirst()
                        .orElseThrow(NotYourConversationException::new);

        if (message.senderAccountId().equals(liker)) {
            throw new RuleViolationException("like.own-message");
        }

        likes.set(messageId, liked ? now : null);
        return new Liked(message, conversation, liked);
    }
}
