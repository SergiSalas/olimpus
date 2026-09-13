package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;

/**
 * How promising a pair is, from 0 to 1.
 *
 * <p>The weights are <b>hypotheses, not configuration</b>. They were set by hand
 * from Columbia's real speed-dating data: what weighed most was a close age,
 * then shared rare interests, and then both people being sociable. Once the app
 * has its own data, that data will tell which weights were right.
 *
 * <p>That is why the round stores where each pair comes from (see
 * {@link Origin}) and part of it is random: if the chosen pairs do no better
 * than random ones, these numbers are adding nothing.
 */
public final class Scorer {

    private Scorer() {}

    public static final double W_AGE_CLOSENESS = 0.28;
    public static final double W_INTERESTS = 0.22;
    public static final double W_SOCIABILITY = 0.18;
    public static final double W_INTENT = 0.12;
    public static final double W_LANGUAGE = 0.08;
    public static final double W_REQUESTED_AGE = 0.06;
    public static final double W_DISTANCE = 0.04;
    public static final double W_NOVELTY = 0.02;

    public static final double WEIGHT_SUM =
            W_AGE_CLOSENESS
                    + W_INTERESTS
                    + W_SOCIABILITY
                    + W_INTENT
                    + W_LANGUAGE
                    + W_REQUESTED_AGE
                    + W_DISTANCE
                    + W_NOVELTY;

    /**
     * Scores the pair from both sides and keeps the worse one.
     *
     * <p>It is the product document's criterion: a pair is only good if it suits
     * both. With the average, "one loves it and the other does not care" would
     * pass, and that ends in a dead conversation.
     */
    public static ScoredPair score(Profile a, Profile b, MatchContext ctx) {
        double sideA = directional(a, b, ctx);
        double sideB = directional(b, a, ctx);
        return new ScoredPair(a, b, Math.min(sideA, sideB), sideA, sideB);
    }

    /**
     * How well b suits person a. Asymmetric on purpose: the age range and the
     * maximum distance are measured from a.
     */
    public static double directional(Profile a, Profile b, MatchContext ctx) {
        double sum =
                W_AGE_CLOSENESS * ageClosenessFit(a, b, ctx)
                        + W_INTERESTS
                                * ctx.interestWeights().similarity(a.interests(), b.interests())
                        + W_SOCIABILITY * sociabilityFit(a, b)
                        + W_INTENT * IntentFit.between(a.intent(), b.intent())
                        + W_LANGUAGE * Filters.sharedLanguageLevel(a, b)
                        + W_REQUESTED_AGE * requestedAgeFit(a, b, ctx)
                        + W_DISTANCE * distanceFit(a, b)
                        + W_NOVELTY * noveltyFit(a, b, ctx);

        return clamp01(sum / WEIGHT_SUM);
    }

    /**
     * Similar age. It is the heaviest factor because it was the one that showed
     * up most in the real data: four years apart already costs quite a bit,
     * twelve leave the factor near zero.
     */
    public static double ageClosenessFit(Profile a, Profile b, MatchContext ctx) {
        int difference = Math.abs(a.ageOn(ctx.today()) - b.ageOn(ctx.today()));
        return Math.exp(-difference / 6.0);
    }

    /**
     * Both being sociable, more than being alike.
     *
     * <p>Two very quiet people are "compatible" in any similarity table, and yet
     * they produce silence. What similarity does reward is the kind of
     * conversation: someone after light chat and someone who wants to go deep
     * miss each other.
     */
    public static double sociabilityFit(Profile a, Profile b) {
        double jointLevel = (a.sociabilityScore() + b.sociabilityScore()) / 2;
        double sameStyle = 1 - Math.abs(a.conversationDepthScore() - b.conversationDepthScore());
        return 0.6 * jointLevel + 0.4 * sameStyle;
    }

    /** How well b's age fits what a asked for. Worth 1 at the centre of the range. */
    public static double requestedAgeFit(Profile a, Profile b, MatchContext ctx) {
        int ageB = b.ageOn(ctx.today());
        if (ageB >= a.ageMin() && ageB <= a.ageMax()) {
            double centre = (a.ageMin() + a.ageMax()) / 2.0;
            double half = Math.max(1, (a.ageMax() - a.ageMin()) / 2.0);
            return 1 - 0.25 * (Math.abs(ageB - centre) / half);
        }
        int excess = ageB < a.ageMin() ? a.ageMin() - ageB : ageB - a.ageMax();
        return Math.max(0, 0.75 - 0.15 * excess);
    }

    public static double distanceFit(Profile a, Profile b) {
        double km = a.location().distanceKmTo(b.location());
        return Math.exp(-km / Math.max(1, a.maxDistanceKm()));
    }

    /**
     * Novelty: instead of blocking forever someone who already talked to you,
     * the weight of meeting again recovers with the days. Never talked is worth
     * 1; talked today, almost 0; in a couple of weeks it is possible again.
     */
    public static double noveltyFit(Profile a, Profile b, MatchContext ctx) {
        Integer days = ctx.daysSinceTalked(a.accountId(), b.accountId());
        if (days == null) return 1;
        return 1 - Math.exp(-Math.max(0, days) / 10.0);
    }

    private static double clamp01(double v) {
        return Math.min(1, Math.max(0, v));
    }
}
