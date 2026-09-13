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

/** Caso de uso: abrir el chat. Devuelve los mensajes y lo poco que se ve del otro. */
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

        Profile yo = profiles.byAccountId(viewer).orElseThrow(NotYourConversationException::new);
        Profile otro =
                profiles
                        .byAccountId(conversation.partnerOf(viewer))
                        .orElseThrow(NotYourConversationException::new);

        List<String> comunes = PartnerView.sharedInterests(yo, otro);
        PartnerView vista =
                PartnerView.levelZero(otro, yo, schedule.dateOf(clock.instant()), comunes);

        return new Chat(conversation, vista, comunes, messages.byConversation(conversationId));
    }
}
