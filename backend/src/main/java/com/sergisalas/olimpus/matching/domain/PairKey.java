package com.sergisalas.olimpus.matching.domain;

import java.util.UUID;

/**
 * Dos personas, sin orden. Sirve para preguntar cosas de una pareja (¿se han
 * bloqueado?, ¿cuando hablaron?) sin tener que mirar las dos direcciones.
 */
public record PairKey(UUID first, UUID second) {

    public static PairKey of(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? new PairKey(a, b) : new PairKey(b, a);
    }
}
