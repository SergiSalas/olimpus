package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Use case: 22:00.
 *
 * <p>Closes the conversations that are past their time. From here on each
 * person's decision rules, which arrives in step 7.
 */
public class CloseFinishedConversations {

    private final ConversationRepository conversations;
    private final RoundSchedule schedule;
    private final Clock clock;

    public CloseFinishedConversations(
            ConversationRepository conversations, RoundSchedule schedule, Clock clock) {
        this.conversations = conversations;
        this.schedule = schedule;
        this.clock = clock;
    }

    public List<Conversation> execute() {
        Instant now = clock.instant();
        LocalDate today = schedule.dateOf(now);

        List<Conversation> closed = new ArrayList<>();
        // The previous day is checked too: if the server was down at 22:00,
        // yesterday's conversations cannot stay open forever.
        for (LocalDate day : List.of(today.minusDays(1), today)) {
            for (Conversation conversation : conversations.byDate(day)) {
                if (conversation.isOpen() && !now.isBefore(conversation.closesAt())) {
                    Conversation closedOne = conversation.closed();
                    conversations.save(closedOne);
                    closed.add(closedOne);
                }
            }
        }
        return closed;
    }
}
