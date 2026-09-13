package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.PartnerView;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case: who am I talking to today?
 *
 * <p>It only returns what the unlock level allows to be seen. If there is no
 * conversation yet, it says when the next one arrives, which is exactly what
 * keeps the wait from feeling like the app is broken.
 */
public class GetTodaysConversation {

    public record Today(
            Optional<Conversation> conversation,
            Optional<PartnerView> partner,
            List<String> sharedInterests,
            LocalDate date) {

        public static Today nothing(LocalDate date) {
            return new Today(Optional.empty(), Optional.empty(), List.of(), date);
        }
    }

    private final ConversationRepository conversations;
    private final ProfileDirectory profiles;
    private final RoundSchedule schedule;
    private final Clock clock;

    public GetTodaysConversation(
            ConversationRepository conversations,
            ProfileDirectory profiles,
            RoundSchedule schedule,
            Clock clock) {
        this.conversations = conversations;
        this.profiles = profiles;
        this.schedule = schedule;
        this.clock = clock;
    }

    public Today execute(UUID accountId) {
        LocalDate today = schedule.dateOf(clock.instant());

        Optional<Conversation> open = conversations.openFor(accountId, today);
        if (open.isEmpty()) {
            return Today.nothing(today);
        }

        Conversation conversation = open.get();
        Optional<Profile> me = profiles.byAccountId(accountId);
        Optional<Profile> partner = profiles.byAccountId(conversation.partnerOf(accountId));
        if (me.isEmpty() || partner.isEmpty()) {
            return Today.nothing(today);
        }

        List<String> shared = PartnerView.sharedInterests(me.get(), partner.get());
        PartnerView view = PartnerView.levelZero(partner.get(), me.get(), today, shared);

        return new Today(Optional.of(conversation), Optional.of(view), shared, today);
    }
}
