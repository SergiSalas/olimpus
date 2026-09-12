package com.sergisalas.olimpus.auth.domain;

import java.util.regex.Pattern;

/**
 * Un email ya normalizado: sin espacios y en minusculas. Existe para que dos
 * personas no puedan tener dos cuentas escribiendo "Ana@x.com" y "ana@x.com".
 */
public record EmailAddress(String value) {

    /** Deliberadamente permisiva: quien manda de verdad es el codigo que llega al buzon. */
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("hace falta un email");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailException("ese email es demasiado largo");
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new InvalidEmailException("ese email no tiene buena pinta");
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
