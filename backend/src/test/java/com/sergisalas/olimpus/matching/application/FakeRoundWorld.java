package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.Message;
import com.sergisalas.olimpus.chat.domain.MessageRepository;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.InterestWeights;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** A toy world to test the rounds without a database. */
public class FakeRoundWorld {

    public final List<Profile> people = new ArrayList<>();
    public final List<Message> written = new ArrayList<>();
    public final Map<UUID, Conversation> stored = new LinkedHashMap<>();
    public final RoundSchedule schedule = RoundSchedule.of(java.time.ZoneId.of("Europe/Madrid"));

    public Instant now = Instant.parse("2026-09-12T06:00:00Z");

    /** The unlock ladder is computed from the messages, so the rounds need them too. */
    public final MessageRepository messages =
            new MessageRepository() {
                @Override
                public void save(Message message) {
                    written.add(message);
                }

                @Override
                public List<Message> byConversation(UUID conversationId) {
                    return written.stream()
                            .filter(m -> m.conversationId().equals(conversationId))
                            .toList();
                }
            };

    public final Clock clock =
            new Clock() {
                @Override
                public java.time.ZoneId getZone() {
                    return java.time.ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return now;
                }
            };

    public final ProfileDirectory profiles =
            new ProfileDirectory() {
                @Override
                public List<Profile> everyoneWithProfile() {
                    return List.copyOf(people);
                }

                @Override
                public Optional<Profile> byAccountId(UUID accountId) {
                    return people.stream().filter(p -> p.accountId().equals(accountId)).findFirst();
                }
            };

    public final ConversationRepository conversations =
            new ConversationRepository() {
                @Override
                public void save(Conversation conversation) {
                    stored.put(conversation.id(), conversation);
                }

                @Override
                public List<Conversation> byDate(java.time.LocalDate date) {
                    return stored.values().stream()
                            .filter(c -> c.roundDate().equals(date))
                            .collect(Collectors.toList());
                }

                @Override
                public Optional<Conversation> openFor(UUID accountId, java.time.LocalDate date) {
                    return stored.values().stream()
                            .filter(c -> c.roundDate().equals(date) && c.isOpen() && c.involves(accountId))
                            .findFirst();
                }

                @Override
                public Optional<Conversation> byId(UUID id) {
                    return Optional.ofNullable(stored.get(id));
                }
            };

    public final MatchContextFactory contexts =
            (today, pool) ->
                    MatchContext.on(today)
                            .interestWeights(InterestWeights.fromPopulation(pool))
                            .build();

    public RunDailyRound dailyRound() {
        return new RunDailyRound(profiles, conversations, contexts, schedule);
    }

    public GetTodaysConversation todaysConversation() {
        return new GetTodaysConversation(conversations, messages, profiles, schedule, clock);
    }
}
