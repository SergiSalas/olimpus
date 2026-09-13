package com.sergisalas.olimpus.matching.adapter.in;

import com.sergisalas.olimpus.matching.application.RunDailyRound;
import com.sergisalas.olimpus.matching.domain.RoundKind;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The two tasks of the day. It is the only place that knows about system
 * clocks: the use case receives the date, so tests do not have to wait until
 * 4 in the morning.
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final RunDailyRound runDailyRound;
    private final RoundSchedule schedule;
    private final Clock clock;

    public RoundScheduler(RunDailyRound runDailyRound, RoundSchedule schedule, Clock clock) {
        this.runDailyRound = runDailyRound;
        this.schedule = schedule;
        this.clock = clock;
    }

    @Scheduled(cron = "${olimpus.rounds.main:0 0 4 * * *}", zone = "${olimpus.zone:Europe/Madrid}")
    public void mainRound() {
        run(RoundKind.MAIN);
    }

    @Scheduled(cron = "${olimpus.rounds.second-chance:0 0 14 * * *}", zone = "${olimpus.zone:Europe/Madrid}")
    public void secondChanceRound() {
        run(RoundKind.SECOND_CHANCE);
    }

    private void run(RoundKind kind) {
        var result = runDailyRound.execute(schedule.dateOf(clock.instant()), kind);
        if (result.alreadyRan()) {
            log.info("Round {} on {}: already ran, nothing touched.", kind, result.date());
            return;
        }
        log.info(
                "Round {} on {}: {} people, {} conversations, {} left out, {} cancelled for silence.",
                kind,
                result.date(),
                result.peopleInPool(),
                result.created().size(),
                result.leftOut().size(),
                result.cancelled().size());
    }
}
