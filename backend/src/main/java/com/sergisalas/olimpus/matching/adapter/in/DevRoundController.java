package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Run a round by hand, so there is no need to wait until 4:00 while developing.
 *
 * <p>It only exists when {@code olimpus.dev-endpoints} is on, and it must be off
 * in the beta.
 */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "olimpus.dev-endpoints", havingValue = "true")
public class DevRoundController {

    public record RoundResponse(
            LocalDate date,
            RoundKind kind,
            int peopleInPool,
            int conversations,
            List<UUID> leftOut,
            int cancelled,
            boolean alreadyRan) {}

    private final RunDailyRound runDailyRound;
    private final RoundSchedule schedule;
    private final Clock clock;

    public DevRoundController(
            RunDailyRound runDailyRound, RoundSchedule schedule, Clock clock) {
        this.runDailyRound = runDailyRound;
        this.schedule = schedule;
        this.clock = clock;
    }

    @PostMapping("/round")
    public RoundResponse run(
            @RequestParam(defaultValue = "MAIN") RoundKind kind,
            @RequestParam(required = false) LocalDate date) {

        LocalDate day = date != null ? date : schedule.dateOf(clock.instant());
        var result = runDailyRound.execute(day, kind);

        return new RoundResponse(
                result.date(),
                result.kind(),
                result.peopleInPool(),
                result.created().size(),
                result.leftOut(),
                result.cancelled().size(),
                result.alreadyRan());
    }
}
