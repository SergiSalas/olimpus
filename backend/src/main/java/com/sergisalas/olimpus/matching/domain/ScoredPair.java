package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;

/**
 * Una pareja posible y lo que promete.
 *
 * @param score lo que promete para el que sale peor parado de los dos
 * @param sideA lo que le conviene b a la persona a
 * @param sideB lo que le conviene a a la persona b
 */
public record ScoredPair(Profile a, Profile b, double score, double sideA, double sideB) {

    public boolean involves(java.util.UUID accountId) {
        return a.accountId().equals(accountId) || b.accountId().equals(accountId);
    }
}
