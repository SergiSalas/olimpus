package com.sergisalas.olimpus.matching.domain;

import java.util.UUID;

/**
 * Una pareja ya decidida para la ronda de hoy: quienes, cuanto prometia y de
 * donde salio.
 */
public record Match(UUID accountA, UUID accountB, double score, Origin origin) {

    public static Match from(ScoredPair pair, Origin origin) {
        return new Match(
                pair.a().accountId(), pair.b().accountId(), pair.score(), origin);
    }

    public boolean involves(UUID accountId) {
        return accountA.equals(accountId) || accountB.equals(accountId);
    }
}
