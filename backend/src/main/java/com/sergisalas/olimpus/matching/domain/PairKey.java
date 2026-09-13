package com.sergisalas.olimpus.matching.domain;

import java.util.UUID;

/**
 * Two people, unordered. Used to ask things about a pair (have they blocked
 * each other? when did they talk?) without checking both directions.
 */
public record PairKey(UUID first, UUID second) {

    public static PairKey of(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? new PairKey(a, b) : new PairKey(b, a);
    }
}
