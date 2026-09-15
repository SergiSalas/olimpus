package com.sergisalas.olimpus.chat.adapter.in;

import com.sergisalas.olimpus.chat.application.CloseFinishedConversations;
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

    public ClosingScheduler(CloseFinishedConversations closeFinished) {
        this.closeFinished = closeFinished;
    }

    @Scheduled(fixedDelayString = "${olimpus.closing.every-ms:300000}")
    public void closeTheDueOnes() {
        var result = closeFinished.execute();
        if (result.total() > 0) {
            log.info(
                    "Day settled: {} conversations became connections, {} closed.",
                    result.connected().size(),
                    result.closed().size());
        }
    }
}
