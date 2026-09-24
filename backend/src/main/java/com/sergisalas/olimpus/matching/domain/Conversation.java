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
        String icebreakerInterest,
        /** When each side said they want to see the other. Null until they do. */
        Instant photoWantedByA,
        Instant photoWantedByB,
        /** What each one answered at the end. Null until they answer. */
        Decision decisionByA,
        Decision decisionByB) {

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
                icebreakerInterest,
                null,
                null,
                null,
                null);
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

    /**
     * Messages are accepted while it is open and 22:00 has not arrived, and
     * forever once it became a connection.
     */
    public boolean acceptsMessagesAt(Instant now) {
        return isConnected() || (isOpen() && now.isBefore(closesAt));
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

    /**
     * Whether someone has already said they want to see the other.
     *
     * <p>Nobody is told that the other one asked: only that both did, when both
     * did. Otherwise the first to ask would be putting pressure on the other.
     */
    public boolean photoWantedBy(UUID accountId) {
        if (accountA.equals(accountId)) return photoWantedByA != null;
        if (accountB.equals(accountId)) return photoWantedByB != null;
        throw new IllegalArgumentException("that account is not in this conversation");
    }

    public boolean bothWantPhoto() {
        return photoWantedByA != null && photoWantedByB != null;
    }

    public Conversation withPhotoWantedBy(UUID accountId, Instant when) {
        if (!involves(accountId)) {
            throw new IllegalArgumentException("that account is not in this conversation");
        }
        boolean isA = accountA.equals(accountId);
        return copy(
                state,
                messagesFromA,
                messagesFromB,
                isA ? firstNotNull(photoWantedByA, when) : photoWantedByA,
                isA ? photoWantedByB : firstNotNull(photoWantedByB, when));
    }

    public Conversation cancelled() {
        return withState(ConversationState.CANCELLED);
    }

    public Conversation closed() {
        return withState(ConversationState.CLOSED);
    }

    /** Reported or blocked: nothing more can be written, whatever state it was in. */
    public Conversation blocked() {
        return withState(ConversationState.BLOCKED);
    }

    private Conversation withState(ConversationState newState) {
        return copy(newState, messagesFromA, messagesFromB, photoWantedByA, photoWantedByB);
    }

    private Conversation withStateAndCounts(ConversationState newState, int fromA, int fromB) {
        return copy(newState, fromA, fromB, photoWantedByA, photoWantedByB);
    }

    private Conversation copy(
            ConversationState newState,
            int fromA,
            int fromB,
            Instant wantedByA,
            Instant wantedByB) {
        return copy(newState, fromA, fromB, wantedByA, wantedByB, decisionByA, decisionByB);
    }

    private Conversation copy(
            ConversationState newState,
            int fromA,
            int fromB,
            Instant wantedByA,
            Instant wantedByB,
            Decision byA,
            Decision byB) {
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
                icebreakerInterest,
                wantedByA,
                wantedByB,
                byA,
                byB);
    }

    /** How long before closing the question is asked. */
    public static final java.time.Duration DECISION_WINDOW = java.time.Duration.ofMinutes(30);

    /**
     * The last minutes, and only them. Asking earlier would end the conversation
     * before it is over; asking afterwards gets a cold, polite answer.
     */
    public boolean acceptsDecisionAt(Instant now) {
        return isOpen()
                && !now.isBefore(closesAt.minus(DECISION_WINDOW))
                && now.isBefore(closesAt);
    }

    public Decision decisionBy(UUID accountId) {
        if (accountA.equals(accountId)) return decisionByA;
        if (accountB.equals(accountId)) return decisionByB;
        throw new IllegalArgumentException("that account is not in this conversation");
    }

    /** Only a yes from both is a connection. No answer is not a yes. */
    public boolean bothSaidYes() {
        return decisionByA == Decision.YES && decisionByB == Decision.YES;
    }

    public Conversation withDecisionBy(UUID accountId, Decision decision) {
        if (!involves(accountId)) {
            throw new IllegalArgumentException("that account is not in this conversation");
        }
        boolean isA = accountA.equals(accountId);
        return copy(
                state,
                messagesFromA,
                messagesFromB,
                photoWantedByA,
                photoWantedByB,
                isA ? decision : decisionByA,
                isA ? decisionByB : decision);
    }

    /** Both said yes: the chat stays, with no closing time. */
    public Conversation connected() {
        return withState(ConversationState.CONNECTED);
    }

    public boolean isConnected() {
        return state == ConversationState.CONNECTED;
    }

    /** Asking twice does not move the moment it was first asked. */
    private static Instant firstNotNull(Instant existing, Instant fallback) {
        return existing != null ? existing : fallback;
    }
}
