package com.sergisalas.olimpus.chat.adapter.out;

import com.sergisalas.olimpus.chat.domain.MessageModerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Development version: everything goes through.
 *
 * <p>It warns once at start-up rather than on every message, so the warning does
 * not become noise that nobody reads. This is one of the two pieces that must be
 * swapped before a single real person uses the app.
 */
@Component
public class AllowEverythingModerator implements MessageModerator {

    private static final Logger log = LoggerFactory.getLogger(AllowEverythingModerator.class);

    public AllowEverythingModerator() {
        log.warn(
                "Chat moderation is OFF: every message goes through. Development only. The beta"
                        + " must not open with this piece in place.");
    }

    @Override
    public Verdict review(String text) {
        return Verdict.ALLOW;
    }
}
