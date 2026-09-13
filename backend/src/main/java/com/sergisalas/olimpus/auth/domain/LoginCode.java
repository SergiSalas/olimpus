package com.sergisalas.olimpus.auth.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * The six-digit code emailed to the person, already turned into a hash.
 *
 * <p>It is short-lived and has a limited number of attempts: otherwise trying
 * all 999,999 combinations would only be a matter of time.
 */
public record LoginCode(EmailAddress email, String codeHash, Instant expiresAt, int attemptsLeft) {

    public static final Duration LIFETIME = Duration.ofMinutes(10);
    public static final int ATTEMPTS = 5;

    public LoginCode {
        if (email == null) throw new IllegalArgumentException("email is missing");
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("code hash is missing");
        }
        if (expiresAt == null) throw new IllegalArgumentException("expiry is missing");
        if (attemptsLeft < 0) throw new IllegalArgumentException("attempts cannot be negative");
    }

    public static LoginCode issued(EmailAddress email, String codeHash, Instant now) {
        return new LoginCode(email, codeHash, now.plus(LIFETIME), ATTEMPTS);
    }

    public boolean hasExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean matches(String candidateHash) {
        return codeHash.equals(candidateHash);
    }

    public LoginCode afterFailedAttempt() {
        return new LoginCode(email, codeHash, expiresAt, attemptsLeft - 1);
    }

    public boolean outOfAttempts() {
        return attemptsLeft <= 0;
    }
}
