package com.sergisalas.olimpus.chat.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * The hearts on messages. Kept apart from {@link MessageRepository} on purpose:
 * a message is what was written and does not change; the heart comes and goes.
 */
public interface MessageLikes {

    /** Puts the heart on (with when) or takes it off (with null). */
    void set(UUID messageId, Instant likedAt);

    /** The messages of a conversation that have a heart. */
    Set<UUID> likedIn(UUID conversationId);
}
