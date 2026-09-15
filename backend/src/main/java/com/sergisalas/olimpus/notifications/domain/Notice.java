package com.sergisalas.olimpus.notifications.domain;

import java.util.UUID;

/**
 * One of the five things Olimpus ever says out loud.
 *
 * <p>Five, and no more. An app that pings too often gets deleted, and here the
 * whole design leans on people coming back at the same hours anyway: what the
 * notifications do is mark those hours, not invent reasons to interrupt.
 *
 * @param deepLink where tapping it should land, or null for the main screen
 */
public record Notice(UUID accountId, Kind kind, String title, String body, String deepLink) {

    public enum Kind {
        /** 8:00 · you have someone to talk to today. */
        NEW_MATCH,
        /** Someone wrote and the app is closed. */
        NEW_MESSAGE,
        /** 14:00 · the second chance found you someone. */
        SECOND_CHANCE,
        /** 21:30 · half an hour left, and the question. */
        CLOSING_SOON,
        /** 22:00 · the result, when there is a connection. */
        CONNECTION
    }

    public Notice {
        if (accountId == null) throw new IllegalArgumentException("account is missing");
        if (kind == null) throw new IllegalArgumentException("kind is missing");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title is missing");
    }
}
