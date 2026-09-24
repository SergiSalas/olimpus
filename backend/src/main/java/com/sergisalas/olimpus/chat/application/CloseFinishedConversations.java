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
 * Use case: 22:00, the moment the day is settled.
 *
 * <p>Whoever got a yes from both keeps the chat, now with no closing time.
 * Everyone else's conversation closes, and neither side is told what the other
 * answered: the two endings look the same from the outside.
 */
public class CloseFinishedConversations {

    /** What the closing did, split so the logs and the notifications can tell them apart. */
    public record Result(List<Conversation> connected, List<Conversation> closed) {

        public int total() {
            return connected.size() + closed.size();
        }
    }

    private final ConversationRepository conversations;
    private final RoundSchedule schedule;
    private final Clock clock;

    public CloseFinishedConversations(
            ConversationRepository conversations, RoundSchedule schedule, Clock clock) {
        this.conversations = conversations;
        this.schedule = schedule;
        this.clock = clock;
    }

    public Result execute() {
        Instant now = clock.instant();
        LocalDate today = schedule.dateOf(now);

        List<Conversation> connected = new ArrayList<>();
        List<Conversation> closed = new ArrayList<>();

        // The previous day is checked too: if the server was down at 22:00,
        // yesterday's conversations cannot stay open forever.
        for (LocalDate day : List.of(today.minusDays(1), today)) {
            for (Conversation conversation : conversations.byDate(day)) {
                if (!conversation.isOpen() || now.isBefore(conversation.closesAt())) {
                    continue;
                }

                // Only a yes from both. No answer is not a yes: whoever never
                // opened the app did not choose to keep going.
                Conversation settled =
                        conversation.bothSaidYes() ? conversation.connected() : conversation.closed();
                conversations.save(settled);

                (settled.isConnected() ? connected : closed).add(settled);
            }
        }
        return new Result(connected, closed);
    }
}
