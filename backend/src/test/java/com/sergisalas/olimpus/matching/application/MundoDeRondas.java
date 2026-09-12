package com.sergisalas.olimpus.matching.application;

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

/** Mundo de juguete para probar las rondas sin base de datos. */
class MundoDeRondas {

    final List<Profile> gente = new ArrayList<>();
    final Map<UUID, Conversation> guardadas = new LinkedHashMap<>();
    final RoundSchedule schedule = RoundSchedule.of(java.time.ZoneId.of("Europe/Madrid"));

    Instant ahora = Instant.parse("2026-09-12T06:00:00Z");

    final Clock clock =
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
                    return ahora;
                }
            };

    final ProfileDirectory profiles =
            new ProfileDirectory() {
                @Override
                public List<Profile> everyoneWithProfile() {
                    return List.copyOf(gente);
                }

                @Override
                public Optional<Profile> byAccountId(UUID accountId) {
                    return gente.stream().filter(p -> p.accountId().equals(accountId)).findFirst();
                }
            };

    final ConversationRepository conversations =
            new ConversationRepository() {
                @Override
                public void save(Conversation conversation) {
                    guardadas.put(conversation.id(), conversation);
                }

                @Override
                public List<Conversation> byDate(java.time.LocalDate date) {
                    return guardadas.values().stream()
                            .filter(c -> c.roundDate().equals(date))
                            .collect(Collectors.toList());
                }

                @Override
                public Optional<Conversation> openFor(UUID accountId, java.time.LocalDate date) {
                    return guardadas.values().stream()
                            .filter(c -> c.roundDate().equals(date) && c.isOpen() && c.involves(accountId))
                            .findFirst();
                }

                @Override
                public Optional<Conversation> byId(UUID id) {
                    return Optional.ofNullable(guardadas.get(id));
                }
            };

    final MatchContextFactory contexts =
            (today, pool) ->
                    MatchContext.on(today)
                            .interestWeights(InterestWeights.fromPopulation(pool))
                            .build();

    RunDailyRound rondaDiaria() {
        return new RunDailyRound(profiles, conversations, contexts, schedule);
    }

    GetTodaysConversation conversacionDeHoy() {
        return new GetTodaysConversation(conversations, profiles, schedule, clock);
    }
}
