package com.sergisalas.olimpus.auth.domain;

import java.util.regex.Pattern;

/**
 * An already normalised email: trimmed and in lower case. It exists so two
 * people cannot get two accounts by typing "Ana@x.com" and "ana@x.com".
 */
public record EmailAddress(String value) {

    /** Deliberately lenient: what really decides is the code reaching the inbox. */
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("email.missing");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailException("email.too-long");
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new InvalidEmailException("email.malformed");
        }
    }

    public static EmailAddress of(String raw) {
        return new EmailAddress(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
