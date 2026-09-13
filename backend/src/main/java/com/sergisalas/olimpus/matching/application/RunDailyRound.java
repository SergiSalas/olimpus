package com.sergisalas.olimpus.matching.application;

import com.sergisalas.olimpus.chat.domain.Icebreakers;
import com.sergisalas.olimpus.matching.domain.Conversation;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.DailyRound;
import com.sergisalas.olimpus.matching.domain.Match;
import com.sergisalas.olimpus.matching.domain.MatchContext;
import com.sergisalas.olimpus.matching.domain.MatchContextFactory;
import com.sergisalas.olimpus.matching.domain.ProfileDirectory;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.profile.domain.Profile;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: the round of the day.
 *
 * <p>At 4:00 it matches everyone. At 14:00 it runs the second chance: first it
 * cancels the conversations that are still silent (nobody has written anything)
 * and then it matches again whoever is left with nothing.
 *
 * <p>It is replayable on purpose: the random seed comes from the day and the
 * round kind, so the same round can be recomputed identically to review it.
 */
public class RunDailyRound {

    /** What a round did. Used for the log and for notifications. */
    public record RoundResult(
            LocalDate date,
            RoundKind kind,
            int peopleInPool,
            List<Conversation> created,
            List<UUID> leftOut,
            List<Conversation> cancelled,
            boolean alreadyRan) {

        public static RoundResult skipped(LocalDate date, RoundKind kind) {
            return new RoundResult(date, kind, 0, List.of(), List.of(), List.of(), true);
        }
    }

    private final ProfileDirectory profiles;
    private final ConversationRepository conversations;
    private final MatchContextFactory contexts;
    private final RoundSchedule schedule;

    public RunDailyRound(
            ProfileDirectory profiles,
            ConversationRepository conversations,
            MatchContextFactory contexts,
            RoundSchedule schedule) {
        this.profiles = profiles;
        this.conversations = conversations;
        this.contexts = contexts;
        this.schedule = schedule;
    }

    public RoundResult execute(LocalDate date, RoundKind kind) {
        List<Conversation> ofTheDay = conversations.byDate(date);

        // Launching the round twice (a restart, a repeated task) must not match
        // twice.
        boolean alreadyRan = ofTheDay.stream().anyMatch(c -> c.roundKind() == kind);
        if (alreadyRan) {
            return RoundResult.skipped(date, kind);
        }

        List<Conversation> cancelled = new ArrayList<>();
        if (kind == RoundKind.SECOND_CHANCE) {
            for (Conversation conversation : ofTheDay) {
                if (conversation.isOpen() && conversation.isSilent()) {
                    Conversation cancelledOne = conversation.cancelled();
                    conversations.save(cancelledOne);
                    cancelled.add(cancelledOne);
                }
            }
        }

        // Whoever already has a live conversation today stays out: one new a day.
        Set<UUID> busy =
                conversations.byDate(date).stream()
                        .filter(Conversation::isOpen)
                        .flatMap(c -> List.of(c.accountA(), c.accountB()).stream())
                        .collect(Collectors.toSet());

        List<Profile> pool =
                profiles.everyoneWithProfile().stream()
                        .filter(p -> !busy.contains(p.accountId()))
                        .toList();

        MatchContext ctx = contexts.forRound(date, pool);
        List<Match> matches = DailyRound.plan(pool, ctx, seedFor(date, kind));

        Instant opens = schedule.opensAt(date, kind);
        Instant closes = schedule.closesAt(date);

        List<Conversation> created = new ArrayList<>();
        for (Match match : matches) {
            // The opening interest is chosen now and stored: both people see
            // exactly the same one, and it does not change if a profile changes
            // later.
            String icebreakerInterest =
                    Icebreakers.rarestShared(find(pool, match.accountA()), find(pool, match.accountB()))
                            .orElse(null);

            Conversation conversation =
                    Conversation.opened(match, date, kind, opens, closes, icebreakerInterest);
            conversations.save(conversation);
            created.add(conversation);
        }

        return new RoundResult(
                date, kind, pool.size(), created, DailyRound.leftOut(pool, matches), cancelled, false);
    }

    /** Same date and same round kind, same matching. */
    private static Random seedFor(LocalDate date, RoundKind kind) {
        return new Random(date.toEpochDay() * 31 + kind.ordinal());
    }

    private static Profile find(List<Profile> pool, UUID accountId) {
        return pool.stream()
                .filter(p -> p.accountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("matched someone who was not in the pool"));
    }
}
