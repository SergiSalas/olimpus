package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.chat.domain.Message;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The rules that open each level.
 *
 * <p>Two things are needed, never one: <b>back-and-forth and time</b>. Copying
 * someone's rhythm to see their photo is easy; what cannot be faked is the time
 * spent. That is why every level above the first asks for both.
 *
 * <p>From the photo on, people decide, not the algorithm: what is seen cannot be
 * unseen, so level 3 needs both to accept.
 *
 * <p>Everything here is computed from the messages themselves, not from a
 * counter kept on the side. A counter can drift; the messages are what actually
 * happened.
 */
public final class UnlockLadder {

    private UnlockLadder() {}

    /** Turns each side needs for level 2. */
    public static final int TURNS_FOR_CONVERSATION = 3;

    /** And time on top, counted from the first message. */
    public static final Duration TIME_FOR_CONVERSATION = Duration.ofHours(1);

    /** Before this much time, the photo cannot even be asked for. */
    public static final Duration TIME_BEFORE_ASKING_FOR_PHOTO = Duration.ofHours(4);

    /** Turns per person. A turn is writing after the other has written. */
    public record Turns(int forA, int forB) {}

    /**
     * A hundred messages in a row are one turn, not a hundred. The first message
     * of the conversation counts as a turn for whoever opened.
     */
    public static Turns turnsOf(Conversation conversation, List<Message> messages) {
        int forA = 0;
        int forB = 0;
        UUID previous = null;

        for (Message message : messages) {
            UUID sender = message.senderAccountId();
            if (!sender.equals(previous)) {
                if (sender.equals(conversation.accountA())) forA++;
                else forB++;
                previous = sender;
            }
        }
        return new Turns(forA, forB);
    }

    public static UnlockLevel levelOf(
            Conversation conversation, List<Message> messages, Instant now) {

        if (messages.isEmpty()) return UnlockLevel.MATCH;

        boolean bothWrote =
                messages.stream().anyMatch(m -> m.senderAccountId().equals(conversation.accountA()))
                        && messages.stream()
                                .anyMatch(m -> m.senderAccountId().equals(conversation.accountB()));
        if (!bothWrote) return UnlockLevel.MATCH;

        if (conversation.bothWantPhoto()) return UnlockLevel.GOOD_CONNECTION;

        Turns turns = turnsOf(conversation, messages);
        boolean enoughTurns =
                turns.forA() >= TURNS_FOR_CONVERSATION && turns.forB() >= TURNS_FOR_CONVERSATION;

        return enoughTurns && elapsed(messages, now).compareTo(TIME_FOR_CONVERSATION) >= 0
                ? UnlockLevel.CONVERSATION
                : UnlockLevel.FIRST_MESSAGE;
    }

    /**
     * Whether the "I want to see you" button should even be there. Asking too
     * early turns the photo back into the first thing that matters.
     */
    public static boolean canAskForPhoto(
            Conversation conversation, List<Message> messages, Instant now) {

        UnlockLevel level = levelOf(conversation, messages, now);
        return level.atLeast(UnlockLevel.CONVERSATION)
                && elapsed(messages, now).compareTo(TIME_BEFORE_ASKING_FOR_PHOTO) >= 0;
    }

    /** Time since the conversation actually started, not since it was handed out. */
    public static Duration elapsed(List<Message> messages, Instant now) {
        if (messages.isEmpty()) return Duration.ZERO;
        return Duration.between(messages.get(0).sentAt(), now);
    }
}
