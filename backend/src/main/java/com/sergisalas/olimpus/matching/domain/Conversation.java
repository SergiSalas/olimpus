package com.sergisalas.olimpus.matching.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One day's conversation between two people.
 *
 * <p>It has a fixed closing time, the same for everyone, and keeps count of how
 * many messages each side has written. That count decides two things: whether
 * it is still silent at midday (and gets cancelled) and, later on, whether the
 * conversation has earned the next unlock level.
 */
public record Conversation(
        UUID id,
        LocalDate roundDate,
        RoundKind roundKind,
        UUID accountA,
        UUID accountB,
        Origin origin,
        double score,
        Instant opensAt,
        Instant closesAt,
        ConversationState state,
        int messagesFromA,
        int messagesFromB,
        /**
         * The shared interest the opening question is about, or null when they
         * share none. Only the interest is kept: the wording depends on the reader.
         */
        String icebreakerInterest) {

    public Conversation {
        if (id == null) throw new IllegalArgumentException("id is missing");
        if (accountA == null || accountB == null) {
            throw new IllegalArgumentException("both people are required");
        }
        if (accountA.equals(accountB)) {
            throw new IllegalArgumentException("nobody talks to themselves");
        }
        if (opensAt == null || closesAt == null) {
            throw new IllegalArgumentException("conversation times are missing");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("the conversation would close before opening");
        }
        if (messagesFromA < 0 || messagesFromB < 0) {
            throw new IllegalArgumentException("message counts cannot be negative");
        }
    }

    public static Conversation opened(
            Match match,
            LocalDate roundDate,
            RoundKind kind,
            Instant opensAt,
            Instant closesAt,
            String icebreakerInterest) {
        return new Conversation(
                UUID.randomUUID(),
                roundDate,
                kind,
                match.accountA(),
                match.accountB(),
                match.origin(),
                match.score(),
                opensAt,
                closesAt,
                ConversationState.OPEN,
                0,
                0,
                icebreakerInterest);
    }

    public boolean involves(UUID accountId) {
        return accountA.equals(accountId) || accountB.equals(accountId);
    }

    public UUID partnerOf(UUID accountId) {
        if (accountA.equals(accountId)) return accountB;
        if (accountB.equals(accountId)) return accountA;
        throw new IllegalArgumentException("that account is not in this conversation");
    }

    /** Nobody has said anything yet: neither one nor the other. */
    public boolean isSilent() {
        return messagesFromA == 0 && messagesFromB == 0;
    }

    /** Both have written: the minimum for it to start counting. */
    public boolean bothHaveWritten() {
        return messagesFromA > 0 && messagesFromB > 0;
    }

    public boolean isOpen() {
        return state == ConversationState.OPEN;
    }

    /** Messages are accepted while it is open and 22:00 has not arrived. */
    public boolean acceptsMessagesAt(Instant now) {
        return isOpen() && now.isBefore(closesAt);
    }

    /**
     * Adds one to the count of the side that writes. That count decides whether
     * the conversation took off (both have written) and, later on, whether it
     * has earned the next unlock level.
     */
    public Conversation withMessageFrom(UUID sender) {
        if (!involves(sender)) {
            throw new IllegalArgumentException("that account is not in this conversation");
        }
        boolean isA = accountA.equals(sender);
        return withStateAndCounts(
                state, messagesFromA + (isA ? 1 : 0), messagesFromB + (isA ? 0 : 1));
    }

    public Conversation cancelled() {
        return withStateAndCounts(ConversationState.CANCELLED, messagesFromA, messagesFromB);
    }

    public Conversation closed() {
        return withStateAndCounts(ConversationState.CLOSED, messagesFromA, messagesFromB);
    }

    private Conversation withStateAndCounts(ConversationState newState, int fromA, int fromB) {
        return new Conversation(
                id,
                roundDate,
                roundKind,
                accountA,
                accountB,
                origin,
                score,
                opensAt,
                closesAt,
                newState,
                fromA,
                fromB,
                icebreakerInterest);
    }
}
