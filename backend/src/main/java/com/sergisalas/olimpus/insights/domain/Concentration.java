package com.sergisalas.olimpus.insights.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How unevenly the conversations are spread.
 *
 * <p>One of the alarms in the design document: <i>a few people taking most of
 * the matches</i>. In a normal dating app that happens on its own and it is what
 * makes most people leave. Here the daily round should prevent it by
 * construction, so this number is the check that it actually does.
 *
 * <p>If the top tenth of people took a tenth of the conversations, the app is
 * perfectly even (0.1). If they took half of them, something is wrong.
 */
public final class Concentration {

    private Concentration() {}

    /**
     * @param conversationsPerPerson how many conversations each person has had
     * @return the share taken by the busiest tenth, between 0 and 1
     */
    public static double shareTakenByTopTenth(List<Integer> conversationsPerPerson) {
        if (conversationsPerPerson.isEmpty()) return 0;

        List<Integer> sorted = new ArrayList<>(conversationsPerPerson);
        sorted.sort(Comparator.reverseOrder());

        long total = sorted.stream().mapToLong(Integer::longValue).sum();
        if (total == 0) return 0;

        // At least one person, so a handful of users still gives a readable number.
        int topTenth = Math.max(1, (int) Math.round(sorted.size() / 10.0));
        long taken = sorted.stream().limit(topTenth).mapToLong(Integer::longValue).sum();

        return (double) taken / total;
    }

    /** What an even spread would look like, to compare against. */
    public static double evenShare(int people) {
        if (people == 0) return 0;
        int topTenth = Math.max(1, (int) Math.round(people / 10.0));
        return (double) topTenth / people;
    }
}
