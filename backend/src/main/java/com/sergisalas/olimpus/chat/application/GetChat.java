package com.sergisalas.olimpus.chat.application;

import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.chat.domain.NotYourConversationException;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.PartnerView;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Use case: open the chat. Returns the messages and the little that is visible of the other person. */
public class GetChat {

    public record Chat(
            Conversation conversation,
            PartnerView partner,
            List<String> sharedInterests,
            List<Message> messages) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ProfileDirectory profiles;
    private final RoundSchedule schedule;
    private final Clock clock;

    public GetChat(
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

    public Chat execute(UUID conversationId, UUID viewer) {
        Conversation conversation =
                conversations.byId(conversationId).orElseThrow(NotYourConversationException::new);

        if (!conversation.involves(viewer)) {
            throw new NotYourConversationException();
        }

        Profile me = profiles.byAccountId(viewer).orElseThrow(NotYourConversationException::new);
        Profile partner =
                profiles
                        .byAccountId(conversation.partnerOf(viewer))
                        .orElseThrow(NotYourConversationException::new);

        List<String> shared = PartnerView.sharedInterests(me, partner);
        PartnerView view =
                PartnerView.levelZero(partner, me, schedule.dateOf(clock.instant()), shared);

        return new Chat(conversation, view, shared, messages.byConversation(conversationId));
    }
}
