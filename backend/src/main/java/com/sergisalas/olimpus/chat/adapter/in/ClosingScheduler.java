package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
import com.sergisalas.olimpus.matching.domain.ConversationRepository;
import com.sergisalas.olimpus.matching.domain.RoundSchedule;
import com.sergisalas.olimpus.notifications.application.Announce;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 22:00. It checks every five minutes instead of once a day: if the server was
 * down right at that time, conversations do not stay open until the next day.
 */
@Component
public class ClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClosingScheduler.class);

    private final CloseFinishedConversations closeFinished;
    private final ConversationRepository conversations;
    private final RoundSchedule schedule;
    private final Announce announce;
    private final Clock clock;

    public ClosingScheduler(
            CloseFinishedConversations closeFinished,
            ConversationRepository conversations,
            RoundSchedule schedule,
            Announce announce,
            Clock clock) {
        this.closeFinished = closeFinished;
        this.conversations = conversations;
        this.schedule = schedule;
        this.announce = announce;
        this.clock = clock;
    }

    /**
     * 21:30, half an hour before closing: the question of the day. Only to
     * conversations that are still alive, and only to the ones where somebody
     * actually spoke: asking about a conversation nobody opened is noise.
     */
    @Scheduled(cron = "${olimpus.closing.warning:0 30 21 * * *}", zone = "${olimpus.zone:Europe/Madrid}")
    public void warnBeforeClosing() {
        var today = schedule.dateOf(clock.instant());
        var alive =
                conversations.byDate(today).stream()
                        .filter(c -> c.isOpen() && c.bothHaveWritten())
                        .toList();

        if (!alive.isEmpty()) {
            announce.closingSoon(alive);
        }
    }

    @Scheduled(fixedDelayString = "${olimpus.closing.every-ms:300000}")
    public void closeTheDueOnes() {
        var result = closeFinished.execute();
        if (!result.connected().isEmpty()) {
            // Only those who connected are told. Saying nothing to the rest says
            // less than "it did not work out", and says nothing about the other.
            announce.connections(result.connected());
        }
        if (result.total() > 0) {
            log.info(
                    "Day settled: {} conversations became connections, {} closed.",
                    result.connected().size(),
                    result.closed().size());
        }
    }
}
