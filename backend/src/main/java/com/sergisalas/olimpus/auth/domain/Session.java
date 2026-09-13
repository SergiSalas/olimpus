package com.sergisalas.olimpus.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * An open session, stored as the hash of the token the phone holds.
 *
 * <p>It lives in the database, not inside a signed token, so it can be cut off
 * instantly: in an app where accounts will have to be kicked out, that matters
 * more than saving a query.
 */
public record Session(
        String tokenHash, UUID accountId, Instant createdAt, Instant expiresAt, Instant revokedAt) {

    public static final Duration LIFETIME = Duration.ofDays(90);

    public Session {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("token hash is missing");
        }
        if (accountId == null) throw new IllegalArgumentException("account is missing");
        if (createdAt == null || expiresAt == null) {
            throw new IllegalArgumentException("session dates are missing");
        }
    }

    public static Session started(String tokenHash, UUID accountId, Instant now) {
        return new Session(tokenHash, accountId, now, now.plus(LIFETIME), null);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
