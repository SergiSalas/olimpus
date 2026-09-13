package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.auth.adapter.in.CurrentAccount;
import com.sergisalas.olimpus.auth.domain.Account;
import com.sergisalas.olimpus.matching.application.GetTodaysConversation;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.shared.adapter.Messages;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** What the app asks when it opens: who am I talking to today? */
@RestController
@RequestMapping("/api")
public class TodayController {

    /**
     * Only what the level allows travels about the other person. If more were
     * ever needed, it gets added here on purpose, not by accident.
     */
    public record PartnerResponse(int age, List<String> interests, int approxDistanceKm, int level) {}

    public record TodayResponse(
            boolean hasConversation,
            UUID conversationId,
            Instant closesAt,
            PartnerResponse partner,
            List<String> sharedInterests,
            Instant nextRoundAt,
            String message) {}

    private final GetTodaysConversation getTodaysConversation;
    private final RoundSchedule schedule;
    private final Messages messages;
    private final Clock clock;

    public TodayController(
            GetTodaysConversation getTodaysConversation,
            RoundSchedule schedule,
            Messages messages,
            Clock clock) {
        this.getTodaysConversation = getTodaysConversation;
        this.schedule = schedule;
        this.messages = messages;
        this.clock = clock;
    }

    @GetMapping("/today")
    public TodayResponse today(@CurrentAccount Account account) {
        var today = getTodaysConversation.execute(account.id());

        if (today.conversation().isEmpty()) {
            return new TodayResponse(
                    false,
                    null,
                    null,
                    null,
                    List.of(),
                    nextRound(),
                    messages.get("today.no-conversation"));
        }

        var conversation = today.conversation().get();
        var partner = today.partner().orElseThrow();

        return new TodayResponse(
                true,
                conversation.id(),
                conversation.closesAt(),
                new PartnerResponse(
                        partner.age(),
                        partner.interestsShown(),
                        partner.approxDistanceKm(),
                        partner.level()),
                today.sharedInterests(),
                null,
                null);
    }

    /** The next time someone may show up, so nobody waits blind. */
    private Instant nextRound() {
        Instant now = clock.instant();
        var date = schedule.dateOf(now);

        Instant secondChance = schedule.opensAt(date, RoundKind.SECOND_CHANCE);
        if (now.isBefore(secondChance)) return secondChance;

        return schedule.opensAt(date.plusDays(1), RoundKind.MAIN);
    }
}
