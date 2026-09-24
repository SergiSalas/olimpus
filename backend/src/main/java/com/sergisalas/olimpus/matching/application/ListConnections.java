package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.PartnerView;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.matching.domain.UnlockLadder;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: the people you connected with.
 *
 * <p>They are kept apart from the conversation of the day and never expire. They
 * do not take up the new conversation either: connections pile up while new
 * people keep arriving, which is the whole point of the daily round.
 */
public class ListConnections {

    public record Connection(
            UUID conversationId,
            PartnerView partner,
            Instant connectedOn,
            Optional<Message> lastMessage) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ProfileDirectory profiles;
    private final RoundSchedule schedule;
    private final Clock clock;

    public ListConnections(
            ConversationRepository conversations,
            MessageRepository messages,
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        this.conversations = conversations;
        this.messages = messages;
        this.profiles = profiles;
        this.schedule = schedule;
        this.clock = clock;
    }

    public List<Connection> execute(UUID accountId) {
        Optional<Profile> me = profiles.byAccountId(accountId);
        if (me.isEmpty()) return List.of();

        var today = schedule.dateOf(clock.instant());
        List<Connection> connections = new ArrayList<>();

        for (Conversation conversation : conversations.connectionsOf(accountId)) {
            Optional<Profile> partner = profiles.byAccountId(conversation.partnerOf(accountId));
            if (partner.isEmpty()) continue;

            List<Message> written = messages.byConversation(conversation.id());
            var level = UnlockLadder.levelOf(conversation, written, clock.instant());

            connections.add(
                    new Connection(
                            conversation.id(),
                            PartnerView.at(
                                    level,
                                    partner.get(),
                                    me.get(),
                                    today,
                                    PartnerView.sharedInterests(me.get(), partner.get())),
                            conversation.closesAt(),
                            written.isEmpty()
                                    ? Optional.empty()
                                    : Optional.of(written.get(written.size() - 1))));
        }
        return connections;
    }
}
