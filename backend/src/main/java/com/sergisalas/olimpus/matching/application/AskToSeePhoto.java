package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.TooSoonForPhotoException;
import com.sergisalas.olimpus.matching.domain.UnlockLadder;
import com.sergisalas.olimpus.matching.domain.UnlockLevel;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: "I want to see you".
 *
 * <p>Level 3 is the only one people open themselves, and it takes both. What is
 * seen cannot be unseen, so neither one can decide for the other.
 *
 * <p>The answer never says whether the other person had already asked. Knowing
 * it would turn a free decision into pressure; the mutual yes at the end of the
 * day works the same way.
 */
public class AskToSeePhoto {

    public record Answer(boolean bothAccepted, UnlockLevel level) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final Clock clock;

    public AskToSeePhoto(
            ConversationRepository conversations, MessageRepository messages, Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.clock = clock;
    }

    public Answer execute(UUID conversationId, UUID asking) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(asking)) {
            throw new NotYourConversationException();
        }

        var written = messages.byConversation(conversationId);
        if (!UnlockLadder.canAskForPhoto(conversation, written, clock.instant())) {
            throw new TooSoonForPhotoException();
        }

        Conversation updated = conversation.withPhotoWantedBy(asking, clock.instant());
        conversations.save(updated);

        return new Answer(
                updated.bothWantPhoto(),
                UnlockLadder.levelOf(updated, written, clock.instant()));
    }
}
