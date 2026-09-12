package com.sergisalas.olimpus.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Una sesion abierta, guardada como huella de la llave que tiene el movil.
 *
 * <p>Se guarda en la base de datos, y no dentro de un token firmado, para
 * poder cortarla al momento: en una app donde habra que expulsar cuentas, eso
 * importa mas que ahorrarse una consulta.
 */
public record Session(
        String tokenHash, UUID accountId, Instant createdAt, Instant expiresAt, Instant revokedAt) {

    public static final Duration DURACION = Duration.ofDays(90);

    public Session {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("falta la huella de la llave");
        }
        if (accountId == null) throw new IllegalArgumentException("falta la cuenta");
        if (createdAt == null || expiresAt == null) {
            throw new IllegalArgumentException("faltan las fechas de la sesion");
        }
    }

    public static Session started(String tokenHash, UUID accountId, Instant now) {
        return new Session(tokenHash, accountId, now, now.plus(DURACION), null);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }
}
