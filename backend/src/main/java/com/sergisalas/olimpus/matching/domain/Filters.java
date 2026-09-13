package com.sergisalas.olimpus.matching.domain;

import com.sergisalas.olimpus.profile.domain.Profile;

/**
 * Filters come in two layers, and they are not interchangeable.
 *
 * <p><b>Hard</b> filters never give way, however long someone has been waiting.
 * If one of them were ever relaxed, it would be a safety bug, not a product
 * improvement.
 *
 * <p><b>Soft</b> filters are preferences, and they have to give way when there
 * are few people: they are filters, not promises. They loosen little by little
 * with the days of waiting, and the person is told ("widening radius to 30 km").
 */
public final class Filters {

    private Filters() {}

    /** Shared language level required with no waiting, and the absolute minimum. */
    private static final double BASE_LANGUAGE_LEVEL = 0.70;

    private static final double MIN_LANGUAGE_LEVEL = 0.35;

    /** After three days of waiting we are at maximum relaxation. */
    public static final int DAYS_TO_MAX_RELAXATION = 3;

    public static boolean passesHard(Profile a, Profile b, MatchContext ctx) {
        if (a.accountId().equals(b.accountId())) return false;

        // Profile already guarantees adulthood when built, but matching trusts
        // nobody: if an old profile slipped through, it is stopped here.
        if (a.isMinorOn(ctx.today()) || b.isMinorOn(ctx.today())) return false;

        // The gender preference has to fit on BOTH sides.
        if (!a.seeking().contains(b.gender())) return false;
        if (!b.seeking().contains(a.gender())) return false;

        // Blocks and reports, in either direction.
        return !ctx.isBlockedEitherWay(a.accountId(), b.accountId());
    }

    /**
     * @param relax 0 = preferences exactly as requested, 1 = the most that is
     *     allowed
     */
    public static boolean passesSoft(Profile a, Profile b, MatchContext ctx, double relax) {
        double r = clamp01(relax);

        // Distance: up to three times the requested radius, and mutual.
        double km = a.location().distanceKmTo(b.location());
        if (km > a.maxDistanceKm() * (1 + 2 * r)) return false;
        if (km > b.maxDistanceKm() * (1 + 2 * r)) return false;

        // Age range: widens up to five years on each side, and mutual. Never
        // below 18, because the hard filter prevents that.
        int ageA = a.ageOn(ctx.today());
        int ageB = b.ageOn(ctx.today());
        double slack = 5 * r;
        if (ageB < a.ageMin() - slack || ageB > a.ageMax() + slack) return false;
        if (ageA < b.ageMin() - slack || ageA > b.ageMax() + slack) return false;

        // Language: the bar drops with waiting, but never below the point where
        // no conversation is possible.
        if (sharedLanguageLevel(a, b) < requiredLanguageLevel(r)) return false;

        // Intent: at maximum relaxation any combination passes.
        return IntentFit.between(a.intent(), b.intent()) >= 0.3 * (1 - r);
    }

    public static double requiredLanguageLevel(double relax) {
        return Math.max(MIN_LANGUAGE_LEVEL, BASE_LANGUAGE_LEVEL - 0.35 * clamp01(relax));
    }

    /**
     * How much the preferences of someone who has been waiting for days are
     * relaxed. It jumps on the first day and saturates at three.
     */
    public static double relaxationForDaysWaiting(int days) {
        return clamp01(days / (double) DAYS_TO_MAX_RELAXATION);
    }

    /**
     * A pair's relaxation is that of whoever has waited longest: if one of them
     * is in a hurry, the door opens even if the other has just arrived.
     */
    public static double relaxationFor(Profile a, Profile b, MatchContext ctx) {
        return Math.max(
                relaxationForDaysWaiting(ctx.daysWaiting(a.accountId())),
                relaxationForDaysWaiting(ctx.daysWaiting(b.accountId())));
    }

    /**
     * Best shared language: the weaker side's fluency rules, because the
     * conversation is limited by whoever speaks it worst.
     */
    public static double sharedLanguageLevel(Profile a, Profile b) {
        double best = 0;
        for (var mine : a.languages()) {
            for (var theirs : b.languages()) {
                if (mine.code().equals(theirs.code())) {
                    best = Math.max(best, Math.min(mine.fluency(), theirs.fluency()));
                }
            }
        }
        return best;
    }

    private static double clamp01(double v) {
        return Math.min(1, Math.max(0, v));
    }
}
