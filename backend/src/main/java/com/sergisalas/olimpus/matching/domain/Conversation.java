package com.sergisalas.olimpus.matching.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * La conversacion de un dia entre dos personas.
 *
 * <p>Tiene hora de cierre fija, la misma para todo el mundo, y lleva la cuenta
 * de cuantos mensajes ha escrito cada lado. Esa cuenta es la que decide dos
 * cosas: si a mediodia esto esta en silencio (y se cancela) y, mas adelante, si
 * la conversacion se ha ganado el siguiente nivel de desbloqueo.
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
        int messagesFromB) {

    public Conversation {
        if (id == null) throw new IllegalArgumentException("falta el id");
        if (accountA == null || accountB == null) {
            throw new IllegalArgumentException("faltan las dos personas");
        }
        if (accountA.equals(accountB)) {
            throw new IllegalArgumentException("nadie habla consigo mismo");
        }
        if (opensAt == null || closesAt == null) {
            throw new IllegalArgumentException("faltan las horas de la conversacion");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("la conversacion cerraria antes de abrirse");
        }
        if (messagesFromA < 0 || messagesFromB < 0) {
            throw new IllegalArgumentException("los mensajes no pueden ser negativos");
        }
    }

    public static Conversation opened(
            Match match, LocalDate roundDate, RoundKind kind, Instant opensAt, Instant closesAt) {
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
                ConversationState.ABIERTA,
                0,
                0);
    }

    public boolean involves(UUID accountId) {
        return accountA.equals(accountId) || accountB.equals(accountId);
    }

    public UUID partnerOf(UUID accountId) {
        if (accountA.equals(accountId)) return accountB;
        if (accountB.equals(accountId)) return accountA;
        throw new IllegalArgumentException("esa cuenta no esta en esta conversacion");
    }

    /** Nadie ha dicho nada todavia: ni uno ni el otro. */
    public boolean isSilent() {
        return messagesFromA == 0 && messagesFromB == 0;
    }

    /** Han escrito los dos: es lo minimo para que empiece a contar. */
    public boolean bothHaveWritten() {
        return messagesFromA > 0 && messagesFromB > 0;
    }

    public boolean isOpen() {
        return state == ConversationState.ABIERTA;
    }

    public Conversation cancelled() {
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
                ConversationState.CANCELADA,
                messagesFromA,
                messagesFromB);
    }

    public Conversation closed() {
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
                ConversationState.CERRADA,
                messagesFromA,
                messagesFromB);
    }
}
