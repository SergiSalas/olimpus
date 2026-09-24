package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.Decision;
import com.sergisalas.olimpus.matching.domain.OutsideDecisionWindowException;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: "do you want to keep getting to know this person?".
 *
 * <p>Each one answers alone, in the last half hour. <b>The answer is never
 * shown to the other</b>, not even afterwards: if they do not match, nobody
 * learns who said no. That is what makes saying no cost nothing.
 *
 * <p>Nothing happens at the moment of answering either. The result is put
 * together at 22:00, when the conversation closes, so the app cannot leak the
 * outcome early through its own timing.
 */
public class DecideOnPartner {

    private final ConversationRepository conversations;
    private final Clock clock;

    public DecideOnPartner(ConversationRepository conversations, Clock clock) {
        this.conversations = conversations;
        this.clock = clock;
    }

    public void execute(UUID conversationId, UUID deciding, Decision decision) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(deciding)) {
            throw new NotYourConversationException();
        }
        if (!conversation.acceptsDecisionAt(clock.instant())) {
            throw new OutsideDecisionWindowException();
        }

        // Answering again before closing is allowed: someone can change their
        // mind while the conversation is still alive.
        conversations.save(conversation.withDecisionBy(deciding, decision));
    }
}
