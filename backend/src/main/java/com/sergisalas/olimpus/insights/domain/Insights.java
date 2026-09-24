package com.sergisalas.olimpus.insights.domain;

import com.sergisalas.olimpus.matching.domain.Origin;
import java.time.LocalDate;
import java.util.List;

/**
 * What the app has learned about itself.
 *
 * <p>The question this exists to answer is the only one that matters early on:
 * <b>do the pairs the algorithm picks do better than random ones?</b> Every
 * round hands out a tenth of its pairs at random precisely so this comparison
 * can be made, and until it is made the weights are a guess.
 */
public record Insights(LocalDate from, LocalDate to, List<ByOrigin> byOrigin, Alarms alarms) {

    /**
     * One row per kind of pair.
     *
     * @param stillTalkingAfter48h connections where somebody wrote again two days
     *     later. This is the north star: it tells a real connection apart from
     *     two people who were merely polite at the end of the conversation.
     */
    public record ByOrigin(
            Origin origin, int pairs, int bothWrote, int connections, int stillTalkingAfter48h) {

        /** Of the pairs handed out, how many ended in a mutual yes. */
        public double connectionRate() {
            return pairs == 0 ? 0 : (double) connections / pairs;
        }

        /** Of the connections, how many were still alive two days later. */
        public double survivalRate() {
            return connections == 0 ? 0 : (double) stillTalkingAfter48h / connections;
        }

        /** Of the pairs handed out, how many became a conversation at all. */
        public double tookOffRate() {
            return pairs == 0 ? 0 : (double) bothWrote / pairs;
        }
    }

    /**
     * The things that say the app is going wrong, from the design document. They
     * are here next to the good numbers on purpose: a north star going up while
     * the alarms go up too is not success.
     */
    public record Alarms(
            int reports,
            int blocks,
            int peopleWithNoMatchInAWeek,
            double averageDaysWaiting,
            int diedInSilence,
            double shareTakenByTopTenth,
            double evenShareWouldBe) {}

    /** How much better (or worse) chosen pairs do than random ones, as a ratio. */
    public double liftOverRandom() {
        double chosen = rateOf(Origin.BEST_MATCH);
        double random = rateOf(Origin.RANDOM);
        return random == 0 ? 0 : chosen / random;
    }

    private double rateOf(Origin origin) {
        return byOrigin.stream()
                .filter(row -> row.origin() == origin)
                .findFirst()
                .map(ByOrigin::connectionRate)
                .orElse(0.0);
    }
}
