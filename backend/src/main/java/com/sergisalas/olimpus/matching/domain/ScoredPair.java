package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;
import java.util.UUID;

/**
 * A possible pair and how promising it is.
 *
 * @param score how promising it is for whichever of the two comes off worse
 * @param sideA how well b suits person a
 * @param sideB how well a suits person b
 */
public record ScoredPair(Profile a, Profile b, double score, double sideA, double sideB) {

    public boolean involves(UUID accountId) {
        return a.accountId().equals(accountId) || b.accountId().equals(accountId);
    }
}
