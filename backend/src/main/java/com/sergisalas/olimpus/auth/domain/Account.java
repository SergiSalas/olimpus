package com.sergisalas.olimpus.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Una cuenta. Solo existe cuando alguien ha demostrado que el email es suyo
 * metiendo el codigo: pedir el codigo no crea nada.
 */
public record Account(UUID id, EmailAddress email, Instant createdAt) {

    public Account {
        if (id == null) throw new IllegalArgumentException("falta el id de la cuenta");
        if (email == null) throw new IllegalArgumentException("falta el email");
        if (createdAt == null) throw new IllegalArgumentException("falta la fecha de creacion");
    }

    public static Account created(EmailAddress email, Instant now) {
        return new Account(UUID.randomUUID(), email, now);
    }
}
