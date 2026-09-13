package com.sergisalas.olimpus.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An account. It only exists once someone has proven the email is theirs by
 * entering the code: requesting the code creates nothing.
 */
public record Account(UUID id, EmailAddress email, Instant createdAt) {

    public Account {
        if (id == null) throw new IllegalArgumentException("account id is missing");
        if (email == null) throw new IllegalArgumentException("email is missing");
        if (createdAt == null) throw new IllegalArgumentException("creation date is missing");
    }

    public static Account created(EmailAddress email, Instant now) {
        return new Account(UUID.randomUUID(), email, now);
    }
}
