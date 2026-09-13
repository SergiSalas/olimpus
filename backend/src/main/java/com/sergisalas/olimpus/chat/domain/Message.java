package com.sergisalas.olimpus.chat.domain;

import com.sergisalas.olimpus.shared.domain.RuleViolationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A message inside the conversation of the day.
 *
 * <p>Text and nothing else: the first version has no photos in the chat,
 * because the photo is exactly what the app keeps for level 3.
 */
public record Message(
        UUID id, UUID conversationId, UUID senderAccountId, String text, Instant sentAt) {

    public static final int MAX_LENGTH = 1000;

    public Message {
        if (id == null) throw new IllegalArgumentException("message id is missing");
        if (conversationId == null) throw new IllegalArgumentException("conversation is missing");
        if (senderAccountId == null) throw new IllegalArgumentException("sender is missing");
        if (sentAt == null) throw new IllegalArgumentException("sent time is missing");

        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            throw new RuleViolationException("message.empty");
        }
        if (text.length() > MAX_LENGTH) {
            throw new RuleViolationException("message.too-long", MAX_LENGTH);
        }
    }

    public static Message written(UUID conversationId, UUID sender, String text, Instant now) {
        return new Message(UUID.randomUUID(), conversationId, sender, text, now);
    }
}
