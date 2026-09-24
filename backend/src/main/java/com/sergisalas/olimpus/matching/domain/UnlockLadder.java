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

        // A connection is a yes from both at the end of the day, which says more
        // than asking to see a photo. So it carries everything the lower levels
        // showed, photo included.
        if (conversation.isConnected()) return UnlockLevel.TRUST;

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

    /**
     * The moment a level opened.
     *
     * @param afterMessageId the message that opened it, or null when time alone
     *     did (an hour passing, or the two photo requests)
     */
    public record Unlock(UnlockLevel level, java.util.UUID afterMessageId, Instant at) {}

    /**
     * When each level opened, in order.
     *
     * <p>Computed instead of remembered, like the level itself, so that the
     * notice inside the chat lands in the same place today and after a reload.
     * The alternative, writing down "unlocked!" when it happens, drifts from the
     * messages the moment anything is retried or replayed.
     */
    public static List<Unlock> unlocksOf(
            Conversation conversation, List<Message> messages, Instant now) {

        List<Unlock> unlocks = new java.util.ArrayList<>();
        UnlockLevel highest = UnlockLevel.MATCH;

        // Levels 1 and 2 are opened by a message: the reply that gets both
        // talking, or the one that completes the turns once the hour has passed.
        for (int i = 0; i < messages.size(); i++) {
            List<Message> soFar = messages.subList(0, i + 1);
            Message last = messages.get(i);
            UnlockLevel level = levelOf(conversation, soFar, last.sentAt());

            if (level.number() > highest.number() && level.atLeast(UnlockLevel.FIRST_MESSAGE)
                    && !level.atLeast(UnlockLevel.GOOD_CONNECTION)) {
                unlocks.add(new Unlock(level, last.id(), last.sentAt()));
                highest = level;
            }
        }

        // An hour can go by with nobody writing, and that also opens level 2.
        UnlockLevel byTime = levelOf(conversation, messages, now);
        if (byTime == UnlockLevel.CONVERSATION && highest.number() < byTime.number()) {
            unlocks.add(new Unlock(byTime, null, now));
            highest = byTime;
        }

        // Level 3 is not opened by talking but by both people accepting.
        if (conversation.bothWantPhoto()) {
            Instant both =
                    conversation.photoWantedByA().isAfter(conversation.photoWantedByB())
                            ? conversation.photoWantedByA()
                            : conversation.photoWantedByB();
            unlocks.add(new Unlock(UnlockLevel.GOOD_CONNECTION, null, both));
        }

        // And level 4 by the mutual yes at the end of the day.
        if (conversation.isConnected()) {
            unlocks.add(new Unlock(UnlockLevel.TRUST, null, conversation.closesAt()));
        }

        return unlocks;
    }

    /** Time since the conversation actually started, not since it was handed out. */
    public static Duration elapsed(List<Message> messages, Instant now) {
        if (messages.isEmpty()) return Duration.ZERO;
        return Duration.between(messages.get(0).sentAt(), now);
    }
}
