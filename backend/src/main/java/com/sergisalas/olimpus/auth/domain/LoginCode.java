package com.sergisalas.olimpus.auth.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * El codigo de seis cifras que se manda al email, ya convertido en huella.
 *
 * <p>Dura poco y tiene los intentos contados: si no, probar las 999.999
 * combinaciones seria cuestion de tiempo.
 */
public record LoginCode(EmailAddress email, String codeHash, Instant expiresAt, int attemptsLeft) {

    public static final Duration DURACION = Duration.ofMinutes(10);
    public static final int INTENTOS = 5;

    public LoginCode {
        if (email == null) throw new IllegalArgumentException("falta el email");
        if (codeHash == null || codeHash.isBlank()) {
            throw new IllegalArgumentException("falta la huella del codigo");
        }
        if (expiresAt == null) throw new IllegalArgumentException("falta la caducidad");
        if (attemptsLeft < 0) throw new IllegalArgumentException("los intentos no pueden ser negativos");
    }

    public static LoginCode issued(EmailAddress email, String codeHash, Instant now) {
        return new LoginCode(email, codeHash, now.plus(DURACION), INTENTOS);
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
