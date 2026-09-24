package com.sergisalas.olimpus.safety.application;

import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.safety.domain.Block;
import com.sergisalas.olimpus.safety.domain.BlockRepository;
import com.sergisalas.olimpus.safety.domain.ReportReason;
import java.time.Clock;
import java.util.UUID;

/**
 * Use case: report, block, or both. One tap.
 *
 * <p>Two things happen at once and in this order: the conversation is cut
 * <b>immediately</b>, and the block is written down. Somebody who is having a
 * bad time should not have to wait for anyone to review anything; the review
 * comes later, the escape hatch is now.
 *
 * <p>The other person is told nothing. They just stop being matched with this
 * one, forever, in both directions.
 */
public class ReportAndBlock {

    private final ConversationRepository conversations;
    private final BlockRepository blocks;
    private final Clock clock;

    public ReportAndBlock(
            ConversationRepository conversations, BlockRepository blocks, Clock clock) {
        this.conversations = conversations;
        this.blocks = blocks;
        this.clock = clock;
    }

    /** @param reason null blocks without reporting */
    public void execute(UUID conversationId, UUID reporter, ReportReason reason) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);
        if (!conversation.involves(reporter)) {
            throw new NotYourConversationException();
        }

        conversations.save(conversation.blocked());
        blocks.save(
                new Block(reporter, conversation.partnerOf(reporter), reason, clock.instant()));
    }
}
